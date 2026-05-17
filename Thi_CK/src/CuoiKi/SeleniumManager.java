package CuoiKi;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class SeleniumManager {

    private static WebDriver driver;

    public static synchronized void init() {

        try {

            close();

            System.out.println( "Đang khởi tạo Selenium...");
            WebDriverManager.chromedriver().setup();
            ChromeOptions options =new ChromeOptions();

            options.addArguments("--start-maximized");
            options.addArguments( "--disable-blink-features=AutomationControlled");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--no-sandbox");

            driver = new ChromeDriver(options);
            System.out.println( "Selenium khởi tạo thành công!" );

        } catch (Exception e) {
            System.out.println(  "SELENIUM INIT ERROR:");
            e.printStackTrace();
        }
    }

    public static synchronized WebDriver getDriver() {

        try {
            if (driver == null) {
                System.out.println( "Driver chưa tồn tại -> init()" );
                init();
            }
            driver.getTitle();

        } catch (Exception e) {
            System.out.println( "Session Selenium đã chết -> Khởi tạo lại" );
            init();
        }
        return driver;
    }


    public static synchronized void resetDriver() {
        System.out.println( "RESET SELENIUM DRIVER..." );
        close();
        init();
    }


    public static synchronized void close() {
        try {

            if (driver != null) {
                driver.quit();
                driver = null;
            }

        } catch (Exception e) {
            driver = null;
        }
    }
}