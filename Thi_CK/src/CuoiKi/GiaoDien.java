package CuoiKi;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import org.json.JSONObject;

public class GiaoDien extends JFrame {

    // ===== TAB USER =====
    JTextField txtUser;
    JButton btnAdd, btnCrawl, btnAI;
    JTable tableUsers;
    DefaultTableModel modelUsers;

    // ===== TAB ANALYSIS =====
    JTable tableAnalysis;
    DefaultTableModel modelAnalysis;

    JComboBox<String> cbUsers;
    JButton btnRefresh;

    JLabel lblTitle;
    JLabel lblTotal;

    public GiaoDien() {

        setTitle("Codeforces AI Analyzer");
        setSize(1100, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 14));
        UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("Table.font", new Font("Segoe UI", Font.PLAIN, 14));

        JTabbedPane tabs = new JTabbedPane();

        // =========================================================
        // TAB 1
        // =========================================================

        JPanel panelUser = new JPanel(new BorderLayout(10, 10));
        panelUser.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top1 = new JPanel(new FlowLayout(FlowLayout.LEFT));

        txtUser = new JTextField(20);

        btnAdd = createButton("➕ Thêm User");
        btnCrawl = createButton("🌐 Crawl code");
        btnAI = createButton("🤖 Phân tích AI");

        top1.add(new JLabel("Username: "));
        top1.add(txtUser);
        top1.add(btnAdd);
        top1.add(btnCrawl);
        top1.add(btnAI);

        panelUser.add(top1, BorderLayout.NORTH);

        modelUsers = new DefaultTableModel(
                new String[]{"ID", "Username", "Platform"}, 0);

        tableUsers = new JTable(modelUsers);

        styleTable(tableUsers);

        JScrollPane sp1 = new JScrollPane(tableUsers);

        panelUser.add(sp1, BorderLayout.CENTER);

        // =========================================================
        // TAB 2
        // =========================================================

        JPanel panelAnalysis = new JPanel(new BorderLayout(10, 10));
        panelAnalysis.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top2 = new JPanel(new BorderLayout());

        JPanel leftTop = new JPanel(new FlowLayout(FlowLayout.LEFT));

        lblTitle = new JLabel("KẾT QUẢ PHÂN TÍCH");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));

        leftTop.add(lblTitle);

        top2.add(leftTop, BorderLayout.NORTH);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        cbUsers = new JComboBox<>();
        cbUsers.setPreferredSize(new Dimension(250, 35));

        btnRefresh = createButton("🔄 Làm mới");

        lblTotal = new JLabel("Tổng bài: 0");

        filterPanel.add(new JLabel("Chọn User: "));
        filterPanel.add(cbUsers);
        filterPanel.add(btnRefresh);
        filterPanel.add(lblTotal);

        top2.add(filterPanel, BorderLayout.SOUTH);

        panelAnalysis.add(top2, BorderLayout.NORTH);

        modelAnalysis = new DefaultTableModel(
                new String[]{
                        "Bài tập",
                        "Ngôn ngữ",
                        "Thuật toán",
                        "Cấu trúc DL",
                        "AI Gen"
                }, 0);

        tableAnalysis = new JTable(modelAnalysis);

        styleTable(tableAnalysis);

        JScrollPane sp2 = new JScrollPane(tableAnalysis);

        panelAnalysis.add(sp2, BorderLayout.CENTER);

        // =========================================================

        tabs.addTab("👤 Quản lý User", panelUser);
        tabs.addTab("📊 Kết quả Phân tích", panelAnalysis);

        add(tabs);

        // =========================================================
        // EVENTS
        // =========================================================

        btnAdd.addActionListener(e -> addUser());

        btnCrawl.addActionListener(e -> {
            new Thread(() -> {
                CrawlerService.crawlAllUsers();

                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this,
                                "Crawl hoàn tất!")
                );
            }).start();
        });

        btnAI.addActionListener(e -> analyzeAI());

        btnRefresh.addActionListener(e -> {
            loadComboUsers();
        });

        cbUsers.addActionListener(e -> {
            String user = (String) cbUsers.getSelectedItem();

            if (user != null) {
                loadAnalysisByUser(user);
            }
        });

        // =========================================================

        loadUsers();
        loadComboUsers();

        setVisible(true);
    }

    // =========================================================
    // STYLE
    // =========================================================

    JButton createButton(String text) {

        JButton btn = new JButton(text);

        btn.setFocusPainted(false);
        btn.setBackground(new Color(52, 152, 219));
        btn.setForeground(Color.WHITE);

        return btn;
    }

    void styleTable(JTable table) {

        table.setRowHeight(30);

        JTableHeader header = table.getTableHeader();

        header.setBackground(new Color(41, 128, 185));
        header.setForeground(Color.WHITE);

        header.setFont(new Font("Segoe UI", Font.BOLD, 14));

        table.setSelectionBackground(new Color(174, 214, 241));
    }

    // =========================================================
    // LOAD USERS
    // =========================================================

    void loadUsers() {

        try (Connection c = Database.getConn()) {

            modelUsers.setRowCount(0);

            Statement st = c.createStatement();

            ResultSet rs = st.executeQuery("SELECT * FROM users");

            while (rs.next()) {

                modelUsers.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("platform")
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // LOAD COMBO USERS
    // =========================================================

    void loadComboUsers() {

        try (Connection c = Database.getConn()) {

            cbUsers.removeAllItems();

            Statement st = c.createStatement();

            ResultSet rs = st.executeQuery(
                    "SELECT username FROM users");

            while (rs.next()) {

                cbUsers.addItem(rs.getString("username"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // LOAD ANALYSIS BY USER
    // =========================================================

    void loadAnalysisByUser(String username) {

        try (Connection c = Database.getConn()) {

            modelAnalysis.setRowCount(0);

            String sql = 
            	    "SELECT s.problem_name, s.language, " +
            	    "a.algorithm, a.data_structure, a.ai_generated " +
            	    "FROM users u " +
            	    "JOIN submissions s ON u.id = s.user_id " +
            	    "LEFT JOIN analysis a ON s.id = a.submission_id " + // Đổi thành LEFT JOIN
            	    "WHERE u.username=?";

            PreparedStatement ps = c.prepareStatement(sql);

            ps.setString(1, username);

            ResultSet rs = ps.executeQuery();

            int total = 0;

            while (rs.next()) {
                total++;
                modelAnalysis.addRow(new Object[]{
                    rs.getString("problem_name"),
                    rs.getString("language"),
                    rs.getString("algorithm") != null ? rs.getString("algorithm") : "Chưa phân tích",
                    rs.getString("data_structure") != null ? rs.getString("data_structure") : "Chưa xác định",
                    rs.getString("ai_generated")
                });
            }

            lblTotal.setText("Tổng bài: " + total);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // ADD USER
    // =========================================================

    void addUser() {

        String username = txtUser.getText().trim();

        if (!username.isEmpty()) {

            Database.insertUser(username, "codeforces");

            loadUsers();
            loadComboUsers();

            txtUser.setText("");
        }
    }

    // =========================================================
    // ANALYZE AI
    // =========================================================

    void analyzeAI() {

        new Thread(() -> {

            try (Connection c = Database.getConn()) {

                String sql =
                        "SELECT id, code FROM submissions " +
                        "WHERE id NOT IN " +
                        "(SELECT submission_id FROM analysis)";

                ResultSet rs =
                        c.createStatement().executeQuery(sql);

                while (rs.next()) {
                    int id = rs.getInt("id");
                    JSONObject res = AIAnalyzer.analyzeCode(rs.getString("code"));

                    if (res != null) {
                        saveAnalysis(id, res);
                        
                        // Cập nhật bảng ngay lập tức sau khi lưu xong 1 bài
                        SwingUtilities.invokeLater(() -> {
                            String currentUser = (String) cbUsers.getSelectedItem();
                            if (currentUser != null) loadAnalysisByUser(currentUser);
                        });
                    }
                    Thread.sleep(5000); // Tăng lên 5s để an toàn cho API Groq
                }

                SwingUtilities.invokeLater(() -> {

                    String user =
                            (String) cbUsers.getSelectedItem();

                    if (user != null) {
                        loadAnalysisByUser(user);
                    }

                    JOptionPane.showMessageDialog(this,
                            "Phân tích hoàn tất!");
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // =========================================================
    // SAVE ANALYSIS
    // =========================================================

    void saveAnalysis(int subId, JSONObject res) {

        try (Connection c = Database.getConn()) {

            PreparedStatement ps =
                    c.prepareStatement(
                            "INSERT INTO analysis" +
                                    "(submission_id, algorithm, data_structure, ai_generated) " +
                                    "VALUES (?,?,?,?)");

            ps.setInt(1, subId);

            ps.setString(2,
                    res.optString("algorithm"));

            ps.setString(3,
                    res.optString("data_structure"));

            ps.setBoolean(4,
                    res.optBoolean("ai_generated"));

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(GiaoDien::new);
    }
}