package CuoiKi;

import java.sql.*;

public class Database {
    static String URL = "jdbc:mysql://localhost:3306/data_ck";
    static String USER = "root";
    static String PASS = "";

    public static Connection getConn() throws Exception {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static void insertUser(String username, String platform) {
        try (Connection c = getConn()) {
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO users(username, platform) VALUES (?,?)"
            );
            ps.setString(1, username);
            ps.setString(2, platform);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}