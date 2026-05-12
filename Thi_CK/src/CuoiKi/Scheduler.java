package CuoiKi;

import java.util.Timer;
import java.util.TimerTask;

public class Scheduler {
    public static void start() {
        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            public void run() {
                System.out.println("Đang crawl tự động...");
                CrawlerService.crawlAllUsers();
            }
        }, 0, 24 * 60 * 60 * 1000);
    }
}