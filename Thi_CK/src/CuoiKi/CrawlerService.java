package CuoiKi;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Random;

public class CrawlerService {


    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0 Safari/537.36";
    private static final String CF_API_URL = "https://codeforces.com/api/user.status?handle=%s&from=1&count=10";
    private static final String CF_SUBMISSION_URL = "https://codeforces.com/contest/%d/submission/%d";
    private static final int MAX_SUBMISSIONS_PER_USER = 5;
    private static final int MIN_CODE_LENGTH = 50;
    private static final int GYM_CONTEST_THRESHOLD = 100000;
    private static final int LOGIN_WAIT_TIME = 40000;
    
    public static void startAutomation() {
        WebDriver driver = SeleniumManager.getDriver();      
        waitForManualAction(driver);       
        crawlAllUsers();        
    }
    
    private static void waitForManualAction(WebDriver driver) {

        try {
            driver.get("https://codeforces.com/");
            Thread.sleep(3000);
            String pageSource =driver.getPageSource().toLowerCase();
            boolean loggedIn = pageSource.contains("logout")
                    || pageSource.contains("/profile/")
                    || pageSource.contains("enter")
                            == false;

            if (loggedIn) {

                return;
            }

            System.out.println( "\nCHƯA ĐĂNG NHẬP CODEFORCES");
            System.out.println( "Vui lòng đăng nhập..." );
            
            Thread.sleep(LOGIN_WAIT_TIME);
            driver.navigate().refresh();
            Thread.sleep(3000);

            pageSource = driver.getPageSource().toLowerCase();
            loggedIn = pageSource.contains("logout")|| pageSource.contains("/profile/");
            
            if (loggedIn) {
                System.out.println( "\nĐĂNG NHẬP THÀNH CÔNG" );
                System.out.println( "BẮT ĐẦU CRAWL CODE..." );

            } else {
                System.out.println( "\nKHÔNG PHÁT HIỆN ĐĂNG NHẬP");
            }

        } catch (Exception e) {
            logError("waitForManualAction",e );
        }
    }

    public static void crawlAllUsers() {
        System.out.println("\n--- BẮT ĐẦU  CRAWL TỰ ĐỘNG ---");
        try (Connection c = Database.getConn();
             PreparedStatement ps = c.prepareStatement("SELECT id, username FROM users");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int userId = rs.getInt("id");
                String username = rs.getString("username");

                printSectionHeader("USER: " + username.toUpperCase());
                processUserSubmissions(userId, username);

                Thread.sleep(5000);
            }
        } catch (Exception e) {
            logError("crawlAllUsers", e);
        }
    }

    public static void analyzeAllSubmissions() {
        printSectionHeader("BẮT ĐẦU PHÂN TÍCH AI");
        String query ="SELECT id, code FROM submissions WHERE algorithm IS NULL";

        try (
                Connection c = Database.getConn();
                PreparedStatement ps = c.prepareStatement(query);
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String code = rs.getString("code");
                System.out.println( "\nĐANG PHÂN TÍCH SUBMISSION ID: " + id );
                JSONObject aiResult = null;

                for (int retry = 1; retry <= 3; retry++) {
                    aiResult = AIAnalyzer.analyzeCode(code);
                    if (aiResult != null) {
                        break;
                    }

                    System.out.println( "AI Retry " + retry + " thất bại -> chờ 15 giây...");
                    Thread.sleep(10000);
                }

                if (aiResult != null) {
                    updateAnalysisInDb( id, aiResult);
                    System.out.println( "PHÂN TÍCH HOÀN TẤT!" );

                } else {
                    System.out.println("BỎ QUA SUBMISSION ID: " + id
                    );
                }

                int delay = 4000 + new Random().nextInt(10000);
                System.out.println(  "Chờ " + (delay / 1000)  + " giây...");

                Thread.sleep(delay);
            }

        } catch (Exception e) {
            logError("analyzeAllSubmissions", e
            );
        }
    }


    private static void processUserSubmissions(int userId, String username) {
        try {
            String apiUrl = String.format(CF_API_URL, username);
            String jsonContent = Jsoup.connect(apiUrl)
                    .ignoreContentType(true)
                    .userAgent(USER_AGENT)
                    .timeout(10000)
                    .execute().body();

            JSONObject json = new JSONObject(jsonContent);
            if (!"OK".equals(json.optString("status"))) return;

            JSONArray results = json.getJSONArray("result");
            int savedCount = 0;

            for (int i = 0; i < results.length() && savedCount < MAX_SUBMISSIONS_PER_USER; i++) {
                JSONObject sub = results.getJSONObject(i);
                
                if (shouldSkipSubmission(sub)) continue;

                long subId = sub.getLong("id");
                int contestId = sub.getInt("contestId");
                String pName = sub.getJSONObject("problem").optString("name", "Unknown");
                String lang = sub.optString("programmingLanguage", "Unknown");

                System.out.printf("Processing: %s (ID: %d)", pName, subId);

                String code = tryCrawlSourceCode(contestId, subId);
                if (code != null && !code.isBlank()) {
                    saveToDatabase(userId, subId, contestId, pName, lang, code);
                    System.out.println(" -> [ĐÃ LƯU SOURCE CODE]");
                    savedCount++;
                    randomDelay();
                } else {
                    System.out.println(" -> [THẤT BẠI: Không lấy được code]");
                }
            }
        } catch (Exception e) {
            logError("processUserSubmissions (" + username + ")", e);
        }
    }

    private static boolean shouldSkipSubmission(JSONObject sub) {
        if (!"OK".equals(sub.optString("verdict"))) return true;
        if (sub.getInt("contestId") > GYM_CONTEST_THRESHOLD) return true;
        if (Database.submissionExists(sub.getLong("id"))) return true;
        return false;
    }

    private static String tryCrawlSourceCode(int contestId, long subId) throws InterruptedException {

        String url = String.format(CF_SUBMISSION_URL, contestId,subId);
        for (int retry = 1; retry <= 3; retry++) {
            try {
                WebDriver driver = SeleniumManager.getDriver();
                driver.get(url);
                Thread.sleep(4000);
                if (driver.getTitle() .contains("Verification")) {
                    System.out.println("\nCAPTCHA DETECTED" );

                    Thread.sleep(15000);
                    driver.navigate().refresh();
                    Thread.sleep(5000);
                }

                if (driver.getPageSource() .contains("program-source-text")) {
                    WebElement element =driver.findElement( By.id("program-source-text") );
                    String code =element.getAttribute( "textContent" );

                    if (code != null && code.length() >= MIN_CODE_LENGTH) {
                        return code;
                    }
                }

            } catch (Exception e) {
                System.out.println("\nSELENIUM ERROR -> RESET DRIVER" );
                SeleniumManager.resetDriver();
            }
            System.out.print(" (Retry " + retry + "...) "
            );
            Thread.sleep(5000);
        }

        return null;
    }

    private static void saveToDatabase(int uId, long sId, int cId, String pName, String lang, String code) {
        String sql = "INSERT INTO submissions(user_id, submission_id, contest_id, problem_name, language, code) VALUES (?,?,?,?,?,?)";
        try (Connection c = Database.getConn(); 
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, uId);
            ps.setLong(2, sId);
            ps.setInt(3, cId);
            ps.setString(4, pName);
            ps.setString(5, lang);
            ps.setString(6, code);
            ps.executeUpdate();
        } catch (Exception e) {
            logError("saveToDatabase", e);
        }
    }

    private static String formatAiField(Object field) {
        if (field instanceof JSONArray) {
            JSONArray arr = (JSONArray) field;
            return String.join(", ", arr.toList().stream().map(Object::toString).toList());
        }
        return field != null ? field.toString() : "Unknown";
    }

    private static void updateAnalysisInDb(int dbId, JSONObject ai) {
        String sql = "UPDATE submissions SET algorithm=?, data_structure=?, time_complexity=?, ai_generated=?, ai_probability=? WHERE id=?";
        try (Connection c = Database.getConn(); 
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setString(1, formatAiField(ai.opt("algorithm")));
            ps.setString(2, formatAiField(ai.opt("data_structure")));          
            ps.setString(3, ai.optString("time_complexity", "Unknown"));
            ps.setString(4, ai.optString("ai_generated", "false"));
            ps.setInt(5, ai.optInt("ai_probability", 0));
            ps.setInt(6, dbId);
            ps.executeUpdate();
        } catch (Exception e) {
            logError("updateAnalysisInDb", e);
        }
    }

    private static void printSectionHeader(String title) {
        System.out.println("\n" + "=".repeat(40));
        System.out.println(" " + title);
        System.out.println("=".repeat(40));
    }

    private static void logError(String method, Exception e) {
        System.err.println("!!! ERROR in " + method + ": " + e.getMessage());
    }

    private static void randomDelay() throws InterruptedException {
        Thread.sleep(3000 + new Random().nextInt(4000));
    }
}