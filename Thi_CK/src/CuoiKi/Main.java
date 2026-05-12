package CuoiKi;

public class Main {
    public static void main(String[] args) {
        Scheduler.start(); // chạy crawl tự động
        new GiaoDien();    // mở giao diện
    }
}