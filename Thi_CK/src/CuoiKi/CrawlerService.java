package CuoiKi;

import java.sql.*;
import org.json.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class CrawlerService {

    // Giả lập trình duyệt để tránh bị Codeforces chặn (Tránh lỗi 403 Forbidden)
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /**
     * Duyệt qua danh sách người dùng trong Database và bắt đầu crawl.
     */
    public static void crawlAllUsers() {
        try (Connection c = Database.getConn()) {
            Statement st = c.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM users");

            while (rs.next()) {
                int userId = rs.getInt("id");
                String username = rs.getString("username");

                System.out.println("\n===== BẮT ĐẦU CRAWL USER: " + username + " =====");
                crawlCodeforces(userId, username);

                // Nghỉ 5 giây giữa các user để bảo vệ địa chỉ IP
                Thread.sleep(5000); 
            }
        } catch (Exception e) {
            System.err.println("Lỗi hệ thống crawl: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách submission của một user từ API Codeforces.
     */
    public static void crawlCodeforces(int userId, String username) {
        try {
            // Chỉ lấy 10 submissions gần nhất để tối ưu hiệu năng
            String apiUrl = "https://codeforces.com/api/user.status?handle=" + username + "&from=1&count=10";

            String jsonContent = Jsoup.connect(apiUrl)
                    .ignoreContentType(true)
                    .userAgent(USER_AGENT)
                    .execute()
                    .body();

            JSONObject json = new JSONObject(jsonContent);

            if (!json.getString("status").equals("OK")) {
                System.out.println("⚠️ Lỗi API Codeforces với user: " + username);
                return;
            }

            JSONArray results = json.getJSONArray("result");
            int count = 0;

            for (int i = 0; i < results.length() && count < 5; i++) { // Giới hạn 5 bài tập phân tích AI mỗi lần
                JSONObject sub = results.getJSONObject(i);

                // Chỉ xử lý các bài đã Accepted (OK)
                if (!sub.has("verdict") || !sub.getString("verdict").equals("OK")) {
                    continue;
                }

                int contestId = sub.getInt("contestId");
                int submissionId = sub.getInt("id");
                String problem = sub.getJSONObject("problem").optString("name", "Unknown");
                String lang = sub.optString("programmingLanguage", "Unknown");

                System.out.print("→ Đang xử lý bài: " + problem);

                // 1. Lấy mã nguồn (Source Code) từ trang HTML
                String code = crawlSourceCode(contestId, submissionId);

                if (!code.isEmpty() && !code.equals("code unavailable")) {
                    System.out.print(" | Đã lấy code | Đang gọi AI...");

                    // 2. Gọi AI để phân tích mã nguồn
                    JSONObject aiResult = AIAnalyzer.analyzeCode(code);

                    // 3. Lưu tất cả vào Database bao gồm kết quả AI
                    saveSubmission(userId, problem, lang, code, aiResult);

                    System.out.println(" [HOÀN TẤT]");
                    count++;
                    
                    // 4. Nghỉ 4 giây để tránh lỗi Rate Limit (429) của Groq AI
                    Thread.sleep(4000); 
                } else {
                    System.out.println(" | [❌ Không lấy được code]");
                }
            }
        } catch (Exception e) {
            System.err.println("\nLỗi crawl user " + username + ": " + e.getMessage());
        }
    }

    /**
     * Truy cập trang web Codeforces để bóc tách mã nguồn bài tập.
     */
    public static String crawlSourceCode(int contestId, int submissionId) {
        try {
            String url = "https://codeforces.com/contest/" + contestId + "/submission/" + submissionId;

            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8")
                    .referrer("https://codeforces.com/")
                    .timeout(10000)
                    .get();

            if (doc.getElementById("program-source-text") != null) {
                return doc.getElementById("program-source-text").text();
            }
        } catch (Exception e) {
            // Không in log để giữ Console sạch sẽ
        }
        return "";
    }

    /**
     * Lưu thông tin bài tập và kết quả phân tích AI vào cơ sở dữ liệu.
     */
    public static void saveSubmission(int userId, String problem, String lang, String code, JSONObject aiAnalysis) {
        String sql = "INSERT INTO submissions(user_id, problem_name, language, code, algorithm, data_structure, ai_generated) VALUES (?,?,?,?,?,?,?)";
        
        try (Connection c = Database.getConn(); 
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setString(2, problem);
            ps.setString(3, lang);
            ps.setString(4, code);

            // Lưu kết quả từ AI nếu có
            if (aiAnalysis != null) {
                ps.setString(5, aiAnalysis.optString("algorithm", "Chưa xác định"));
                ps.setString(6, aiAnalysis.optString("data_structure", "Chưa xác định"));
                ps.setString(7, aiAnalysis.optString("ai_generated", "No"));
            } else {
                ps.setNull(5, Types.VARCHAR);
                ps.setNull(6, Types.VARCHAR);
                ps.setNull(7, Types.VARCHAR);
            }

            ps.executeUpdate();

        } catch (Exception e) {
            System.err.println("Lỗi lưu DB: " + e.getMessage());
        }
    }
}