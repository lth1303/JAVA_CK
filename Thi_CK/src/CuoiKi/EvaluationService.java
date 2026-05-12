package CuoiKi;

import java.sql.*;

public class EvaluationService {

    public static double evaluateUser(int userId) {
        int algoScore = 0;
        int dsScore = 0;
        int total = 0;
        int aiCount = 0;

        try (Connection c = Database.getConn()) {

            String sql = "SELECT * FROM analysis a JOIN submissions s ON a.submission_id = s.id WHERE s.user_id=?";
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                total++;

                String algo = rs.getString("algorithm");

                if (algo.contains("Dynamic")) algoScore += 3;
                else if (algo.contains("Graph")) algoScore += 4;
                else algoScore += 1;

                String ds = rs.getString("data_structure");
                if (ds.contains("Tree")) dsScore += 3;
                else dsScore += 1;

                if (rs.getBoolean("ai_generated")) aiCount++;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (total == 0) return 0;

        double aiRatio = (double) aiCount / total;

        return 0.4 * algoScore + 0.4 * dsScore + 0.2 * (1 - aiRatio) * 100;
    }
}