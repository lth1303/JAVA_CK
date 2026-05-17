package CuoiKi;

import java.sql.*;

public class Database {


    private static final String URL =
            "jdbc:mysql://localhost:3306/data_java"
                    + "?useSSL=false"
                    + "&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=UTC";

    private static final String USER = "---";
    private static final String PASS = "---";

    public static Connection getConn() throws Exception {
        Class.forName( "com.mysql.cj.jdbc.Driver");

        return DriverManager.getConnection( URL, USER,PASS );
    }

    public static void initDatabase() {
        try (Connection c = getConn()) {
            System.out.println("Database connected successfully!" );

        } catch (Exception e) {
            System.out.println("INIT DATABASE ERROR:" );
            e.printStackTrace();
        }
    }

    public static boolean userExists( String username) {
        try (Connection c = getConn()) {
            PreparedStatement ps =c.prepareStatement(
                            "SELECT id FROM users "
                                    + "WHERE username=?"
                    );

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static void insertUser(String username, String platform) {

        try (Connection c = getConn()) {
            if (username == null|| username.isBlank()) {
                System.out.println(  "Username rỗng!" );
                return;
            }
            if (userExists(username)) {
                System.out.println(  "User đã tồn tại: " + username );

                return;
            }

            PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO users("
                                    + "username, platform"
                                    + ") VALUES (?,?)"
                    );

            ps.setString(1, username);
            ps.setString(2, platform);
            ps.executeUpdate();
            System.out.println("Thêm user thành công: " + username );

        } catch (Exception e) {
            System.out.println( "INSERT USER ERROR:" );
            e.printStackTrace();
        }
    }

    public static void deleteUser(
            String username
    ) {
        try (Connection c = getConn()) {
            PreparedStatement ps =c.prepareStatement(
                            "DELETE FROM users "
                                    + "WHERE username=?"
                    );

            ps.setString(1, username);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println( "Đã xóa user: " + username );

            } else {

                System.out.println( "Không tìm thấy user" );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean submissionExists(
            long submissionId
    ) {

        try (Connection c = getConn()) {
            PreparedStatement ps =c.prepareStatement(
                            "SELECT id FROM submissions "
                                    + "WHERE submission_id=?"
                    );

            ps.setLong(1, submissionId);
            ResultSet rs =ps.executeQuery();
            return rs.next();

        } catch (Exception e) {

            e.printStackTrace();
        }

        return false;
    }

    public static void testConnection() {
        try (Connection c = getConn()) {
            System.out.println( "MySQL Connected Successfully!" );

        } catch (Exception e) {
            System.out.println(
                    "MYSQL CONNECTION ERROR:"
            );
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        testConnection();

        initDatabase();
    }
}