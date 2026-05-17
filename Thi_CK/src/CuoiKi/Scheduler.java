package CuoiKi;

import java.util.Timer;
import java.util.TimerTask;

public class Scheduler {

    public static void start(GiaoDien ui) {
        Timer timer = new Timer();
        timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        try {

                            ui.runScheduledCrawl();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                },

                0,
                3600000
        );
    }
}