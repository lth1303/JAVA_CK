package CuoiKi;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import java.awt.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class GiaoDien extends JFrame {

	private JTextField txtUser;
    private JTextField txtSearch;

    private JButton btnAdd;
    private JButton btnDelete;
    private JButton btnCrawl;
    private JButton btnAI;
    private JButton btnRefresh;

    private JTable tableUsers;
    private JTable tableAnalysis;

    private DefaultTableModel modelUsers;
    private DefaultTableModel modelAnalysis;

    private JComboBox<String> cbUsers;
    private JLabel lblTotal;
    private JTextArea txtLogs;
    private JProgressBar progressBar;
    
    private JEditorPane txtUserEvaluation;
    private JComboBox<String> cbEvaluateUser;

    public GiaoDien() {

        setTitle("Hệ thống crawl & phân tích code từ Codeforces - Lê Thị Hiền");
        setSize(1400, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(10,10,10,10));
        add(root);
        
        JTabbedPane tabs =  new JTabbedPane();
        tabs.putClientProperty("JTabbedPane.tabHeight",42);
        tabs.setFont( new Font( "Segoe UI", Font.BOLD, 14));
        tabs.addTab("Chức năng chung",createUserPanel());
        tabs.addTab("Kết quả phân tích",createAnalysisPanel());
        tabs.addTab("Đánh giá User",createEvaluatePanel());
        root.add(tabs, BorderLayout.CENTER);

        loadUsers();
        loadComboUsers();
        setVisible(true);
    }

    private JPanel createUserPanel() {
        JPanel panel = new JPanel(new BorderLayout(15,15) );
        panel.setBorder(new EmptyBorder(15,15,15,15) );
        JPanel top =new JPanel( new FlowLayout( FlowLayout.LEFT,10,5 ));

        txtUser = new JTextField(20);
        txtSearch =new JTextField(15);
        txtUser.putClientProperty("JTextField.placeholderText", "Nhập username...");
        txtSearch.putClientProperty( "JTextField.placeholderText", "Nhập user muốn tìm..." );

        btnAdd =createButton("Thêm User");
        btnDelete = createButton("Xóa User");
        btnCrawl = createButton("Crawl code");
        btnAI =createButton("Phân tích code");
        btnRefresh =createButton("Refresh");

        top.add(new JLabel("Username"));
        top.add(txtUser);
        top.add(btnAdd);
        top.add(btnDelete);
        top.add(btnCrawl);
        top.add(btnAI);
        top.add(Box.createHorizontalStrut(20));
        top.add(new JLabel("Search"));
        top.add(txtSearch);
        top.add(btnRefresh);

        panel.add( top, BorderLayout.NORTH);

        modelUsers = new DefaultTableModel( new String[] { 
        		"ID", "Username", "Platform"
        		}, 0);
        tableUsers = new JTable(modelUsers);
        styleTable(tableUsers);
        JScrollPane scroll = new JScrollPane(tableUsers);
        scroll.putClientProperty(  "JScrollPane.smoothScrolling", true );
        panel.add( scroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel( new BorderLayout(10,10));
        
        txtLogs = new JTextArea();
        txtLogs.setEditable(false);
        txtLogs.setFont( new Font("Consolas", Font.PLAIN,13));

        JScrollPane logScroll = new JScrollPane(txtLogs);
        logScroll.setPreferredSize( new Dimension(100,180));

        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        progressBar.setIndeterminate(false);
        progressBar.setStringPainted(false);
        progressBar.putClientProperty( "JProgressBar.largeHeight",true);

        bottom.add( progressBar,BorderLayout.NORTH);
        bottom.add( logScroll,BorderLayout.CENTER);
        panel.add( bottom, BorderLayout.SOUTH );

        btnAdd.addActionListener( e -> addUser() );
        btnDelete.addActionListener( e -> deleteUser() );
        btnRefresh.addActionListener( e -> loadUsers() );
        btnCrawl.addActionListener(  e -> startCrawl() );
        btnAI.addActionListener( e -> analyzeAI());

        txtSearch.addKeyListener( new java.awt.event.KeyAdapter() {
                    public void keyReleased(java.awt.event.KeyEvent evt) {
                        searchUsers( txtSearch.getText() );
                    }
                }
        );
        return panel;
    }

    private JPanel createAnalysisPanel() {
        JPanel panel = new JPanel(new BorderLayout(15,15) );
        panel.setBorder( new EmptyBorder(15,15,15,15));
        JPanel top = new JPanel( new FlowLayout(FlowLayout.LEFT, 10, 5 ));
        
        cbUsers = new JComboBox<>();
        cbUsers.setPreferredSize(new Dimension(250,38) );
        lblTotal =new JLabel("Tổng bài: 0");
        lblTotal.setFont( new Font( "Segoe UI", Font.BOLD,15 ) );

        top.add(new JLabel("User"));
        top.add(cbUsers);
        top.add(lblTotal);
        panel.add( top,BorderLayout.NORTH );

        modelAnalysis =new DefaultTableModel(
                        new String[]{
                                "Problem",
                                "Language",
                                "Thuật toán",
                                "Cấu trúc dữ liệu",
                                "Độ phức tạp",
                                "Tỷ lệ dùng AI",
                                "Sử dụng AI"
                        },
                        0
                );
        tableAnalysis = new JTable(modelAnalysis);
        styleTable(tableAnalysis);
        JScrollPane scroll =new JScrollPane(tableAnalysis);
        panel.add( scroll, BorderLayout.CENTER);

        cbUsers.addActionListener( e -> {

                    String user = (String)cbUsers.getSelectedItem();
                    if (user != null) {
                        loadAnalysisByUser(user);
                    }
                }
        );
        
        tableAnalysis.getSelectionModel().addListSelectionListener(e -> {

            if (!e.getValueIsAdjusting()) {
                int row = tableAnalysis.getSelectedRow();
                if (row != -1) {
                    String problemName = modelAnalysis.getValueAt(row, 0).toString();
                    showSubmissionDetail(problemName);
                }
            }
        });
        return panel;
    }
    
    private JPanel createEvaluatePanel() {
        JPanel panel =new JPanel( new BorderLayout(15,15) );
        panel.setBorder( new EmptyBorder(15,15,15,15) );
        JPanel top = new JPanel( new FlowLayout(FlowLayout.LEFT, 10, 5 ));
        cbEvaluateUser = new JComboBox<>();
        cbEvaluateUser.setPreferredSize(new Dimension(250,35)); 
        JButton btnEvaluate = createButton("Đánh giá");
        
        top.add(new JLabel("User"));
        top.add(cbEvaluateUser);
        top.add(btnEvaluate);
        panel.add( top,BorderLayout.NORTH);

        txtUserEvaluation = new JEditorPane();
        txtUserEvaluation.setEditable(false);
        txtUserEvaluation.setContentType(  "text/html" );
        txtUserEvaluation.putClientProperty( JEditorPane.HONOR_DISPLAY_PROPERTIES,true);
        txtUserEvaluation.setFont( new Font("Segoe UI", Font.PLAIN, 14 ));
        txtUserEvaluation.setBackground( new Color(250,250,250));
        txtUserEvaluation.setBorder( new EmptyBorder(20,20,20,20));

        JScrollPane scroll = new JScrollPane( txtUserEvaluation);
        scroll.setBorder( BorderFactory.createTitledBorder( "Đánh giá tổng quan User" ) );
        panel.add( scroll,BorderLayout.CENTER);

        loadEvaluateUsers();
        btnEvaluate.addActionListener(e -> {

            String user =(String)cbEvaluateUser.getSelectedItem();
            if (user != null) {
                evaluateUser(user);
            }
        });

        return panel;
    }
    
    private void loadEvaluateUsers() {
        try (Connection c = Database.getConn()) {
            cbEvaluateUser.removeAllItems();
            Statement st = c.createStatement();
            ResultSet rs =st.executeQuery( "SELECT username FROM users"  );
            while (rs.next()) {
                cbEvaluateUser.addItem(rs.getString("username") );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void evaluateUser(String username) {

        try (Connection c = Database.getConn()) {
            String sql =
                    "SELECT * FROM submissions s "
                            + "JOIN users u "
                            + "ON s.user_id=u.id "
                            + "WHERE u.username=?";
            PreparedStatement ps =c.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            int total = 0;
            int aiSum = 0;
            java.util.Map<String, Integer> algoMap =
                    new java.util.HashMap<>();
            java.util.Map<String, Integer> dsMap =
                    new java.util.HashMap<>();

            while (rs.next()) {
                total++;
                aiSum += rs.getInt("ai_probability");
                String algo = rs.getString("algorithm");

                if (algo != null) {
                    String[] arr = algo.split(",");
                    for (String s : arr) {
                        s = s.trim();
                        if (!s.isBlank()) {
                            algoMap.put( s, algoMap.getOrDefault(s, 0) + 1
                            );
                        }
                    }
                }

                String ds =rs.getString("data_structure");
                if (ds != null) {
                    String[] arr =ds.split(",");
                    for (String s : arr) {
                        s = s.trim();
                        if (!s.isBlank()) {
                            dsMap.put( s, dsMap.getOrDefault(s, 0) + 1
                            );
                        }
                    }
                }
            }

            if (total == 0) {
                txtUserEvaluation.setText( "Chưa có dữ liệu." );
                return;
            }

            StringBuilder topAlgo =new StringBuilder();
            algoMap.entrySet()
                    .stream()
                    .sorted((a, b) -> b.getValue() - a.getValue())
                    .limit(5)
                    .forEach(e -> {
                        topAlgo.append("• ")
                                .append(e.getKey())
                                .append(" (")
                                .append(e.getValue())
                                .append(" bài)\n");
                    });

            StringBuilder topDS =new StringBuilder();
            dsMap.entrySet()
                    .stream()
                    .sorted((a, b) -> b.getValue() - a.getValue())
                    .limit(5)
                    .forEach(e -> {
                        topDS.append("• ")
                                .append(e.getKey())
                                .append(" (")
                                .append(e.getValue())
                                .append(" bài)\n");
                    });

            int avgAI =aiSum / total;
            String aiLevel;
            Color aiColor;
            if (avgAI >= 70) {
                aiLevel = "CAO";
                aiColor = new Color(231, 76, 60);
            } else if (avgAI >= 40) {
                aiLevel = "TRUNG BÌNH";
                aiColor = new Color(241, 196, 15);
            } else if (avgAI == 0) {
                aiLevel = "KHÔNG SỬ DỤNG AI";
                aiColor = new Color(46, 204, 113);
            }else {
                aiLevel = "THẤP";
                aiColor = new Color(46, 204, 113);
            }

            String html =
                    "<html>"
                            + "<div style='font-family:Segoe UI;padding:15px;'>"
                            + "<h1 style='color:#3498db;'>"
                            + username.toUpperCase()
                            + "</h1>"
                            + "<hr>"
                            + "<h2>Tổng quan</h2>"
                            + "<p><b>Tổng số bài:</b> "
                            + total
                            + "</p>"
                            + "<p><b>AI trung bình:</b> "
                            + avgAI
                            + "%</p>"
                            + "<p><b>Mức độ sử dụng AI:</b> "
                            + "<span style='color:rgb("
                            + aiColor.getRed() + ","
                            + aiColor.getGreen() + ","
                            + aiColor.getBlue()
                            + ");font-weight:bold;'>"
                            + aiLevel
                            + "</span></p>"
                            + "<br>"
                            + "<h2>Thuật toán sử dụng nhiều</h2>"
                            + "<div style='margin-left:10px;'>"
                            + topAlgo.toString()
                            .replace("\n", "<br>")
                            + "</div>"
                            + "<br>"
                            + "<h2>Cấu trúc dữ liệu sử dụng nhiều</h2>"
                            + "<div style='margin-left:10px;'>"
                            + topDS.toString()
                            .replace("\n", "<br>")
                            + "</div>"
                            + "</div>"
                            + "</html>";

            txtUserEvaluation.setContentType("text/html");
            txtUserEvaluation.setText(html);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    private void showSubmissionDetail(String problemName) {
        if (problemName == null || problemName.trim().isEmpty()) return;
        try (Connection c = Database.getConn()) {
            String sql = "SELECT * FROM submissions WHERE problem_name LIKE ? ORDER BY id DESC LIMIT 1";
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setString(1, problemName.trim());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return;

            String language = rs.getString("language");
            String algorithm = formatListString(rs.getString("algorithm"));
            String dataStructure = formatListString(rs.getString("data_structure"));
            String complexity = formatListString(rs.getString("time_complexity"));
            
            String aiProb = rs.getString("ai_probability");
            String aiGen = rs.getString("ai_generated");
            String rawCode = rs.getString("code");

            JDialog dialog = new JDialog(this, "Chi tiết phân tích bài nộp", true);
            dialog.setSize(1100, 750);
            dialog.setLocationRelativeTo(this);
            
            JPanel container = new JPanel(new BorderLayout(15, 15));
            container.setBorder(new EmptyBorder(15, 15, 15, 15));
            container.setBackground(Color.WHITE);

            JPanel leftPanel = new JPanel();
            leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
            leftPanel.setPreferredSize(new Dimension(350, 0));
            leftPanel.setBackground(new Color(248, 249, 250));
            leftPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230)),
                new EmptyBorder(20, 20, 20, 20)
            ));

            JLabel lblProblemTitle = new JLabel("<html><body style='width: 250px'>" + problemName.toUpperCase() + "</body></html>");
            lblProblemTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
            lblProblemTitle.setForeground(new Color(41, 128, 185));
            leftPanel.add(lblProblemTitle);
            leftPanel.add(Box.createVerticalStrut(15));
            leftPanel.add(new JSeparator());
            leftPanel.add(Box.createVerticalStrut(20));

            addInfoRow(leftPanel, "NGÔN NGỮ", language);
            addInfoRow(leftPanel, "THUẬT TOÁN", algorithm);
            addInfoRow(leftPanel, "CẤU TRÚC DỮ LIỆU", dataStructure);
            addInfoRow(leftPanel, "ĐỘ PHỨC TẠP", complexity);

            leftPanel.add(Box.createVerticalStrut(10));
            leftPanel.add(new JSeparator());
            leftPanel.add(Box.createVerticalStrut(20));

            JLabel lblAISecton = new JLabel("PHÂN TÍCH AI");
            lblAISecton.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblAISecton.setForeground(new Color(142, 68, 173));
            leftPanel.add(lblAISecton);
            leftPanel.add(Box.createVerticalStrut(10));

            addInfoRow(leftPanel, "XÁC SUẤT SỬ DỤNG AI", aiProb + "%");
            
            String statusText = "true".equalsIgnoreCase(aiGen) ? "CÓ SỬ DỤNG AI" : "KHÔNG";
            addInfoRow(leftPanel, "CÓ SỬ DỤNG AI", statusText);

            JTextArea txtCode = new JTextArea(formatSourceCode(rawCode));
            txtCode.setEditable(false);
            txtCode.setFont(new Font("Consolas", Font.PLAIN, 13));
            txtCode.setBackground(new Color(30, 30, 30));
            txtCode.setForeground(new Color(220, 220, 220));
            txtCode.setCaretPosition(0);

            JScrollPane scroll = new JScrollPane(txtCode);
            scroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(60, 60, 60)), " SOURCE CODE "));
            
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, scroll);
            splitPane.setDividerLocation(350);
            splitPane.setDividerSize(5);

            container.add(splitPane, BorderLayout.CENTER);
            dialog.add(container);

            SwingUtilities.invokeLater(() -> dialog.setVisible(true));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String formatListString(String input) {
        if (input == null || input.isEmpty() || input.equalsIgnoreCase("null")) return "N/A";
        return input.replace("[", "")
                    .replace("]", "")
                    .replace("\"", "")
                    .replace(",", ", ")
                    .trim();
    }
    
    private void addInfoRow(JPanel panel, String title, String value) {
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblTitle.setForeground(new Color(127, 140, 141));
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JTextArea txtValue = new JTextArea(value);
        txtValue.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtValue.setForeground(new Color(44, 62, 80));
        txtValue.setEditable(false);
        txtValue.setOpaque(false);
        txtValue.setLineWrap(true);   
        txtValue.setWrapStyleWord(true);
        txtValue.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtValue.setMaximumSize(new Dimension(300, Integer.MAX_VALUE));
        txtValue.setBorder(new EmptyBorder(0, 0, 15, 0));

        panel.add(lblTitle);
        panel.add(txtValue);
    }
    

    private String formatSourceCode(String code) {
        if (code == null || code.isEmpty()) return "";
        String result = code.replace("\\n", "\n").replace("\\t", "    ");
        
        if (!result.contains("\n") || result.length() > 200) {
            result = result.replaceAll("(?<=;)", "\n")        
                           .replaceAll("(?<=\\{)", "\n")     
                           .replaceAll("(?=\\})", "\n")       
                           .replaceAll("(?<=\\})", "\n")      
                           .replaceAll("(?=#include)", "\n")  
                           .replaceAll("(?=using namespace)", "\n");
        }
        return result;
    }
    

    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR ));
        btn.putClientProperty( "JButton.buttonType","roundRect");
        return btn;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(35);
        table.setShowVerticalLines(false);
        table.setFont( new Font( "Segoe UI", Font.PLAIN,13 ) );
        table.setSelectionBackground( new Color( 70, 120,220));

        JTableHeader header =table.getTableHeader();
        header.setFont( new Font("Segoe UI", Font.BOLD, 14 ));
        header.setReorderingAllowed(false);
    }

    private void log(String text) {
        SwingUtilities.invokeLater(() -> {
            txtLogs.append(text + "\n");
            txtLogs.setCaretPosition(txtLogs.getDocument().getLength() );
        });
    }

    private void loadUsers() {
        try (Connection c =Database.getConn()) {
            modelUsers.setRowCount(0);
            Statement st =c.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM users" );

            while (rs.next()) {
                modelUsers.addRow(new Object[]{
                                rs.getInt("id"),
                                rs.getString("username"),
                                rs.getString("platform")
                        }
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadComboUsers() {
        try (Connection c = Database.getConn()) {
            cbUsers.removeAllItems();
            Statement st =c.createStatement();
            ResultSet rs =st.executeQuery("SELECT username FROM users" );

            while (rs.next()) {
                cbUsers.addItem(rs.getString("username") );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addUser() {
        String username = txtUser.getText().trim();
        if (username.isBlank()) {
            JOptionPane.showMessageDialog( this,"Nhập username!" );
            return;
        }

        Database.insertUser( username,"codeforces"
        );
        loadUsers();
        loadComboUsers();
        loadEvaluateUsers();
        txtUser.setText("");
        log("Đã thêm user: " + username);
    }

    private void deleteUser() {
        int row = tableUsers.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog( this,"Chọn user!");
            return;
        }
        String username = modelUsers.getValueAt(row, 1).toString();
        Database.deleteUser(username);
        loadUsers();
        loadComboUsers();
        loadEvaluateUsers();
        log("Đã xóa user: " + username);
    }

    private void searchUsers(String keyword) {
        try (Connection c = Database.getConn()) {
            modelUsers.setRowCount(0);
            PreparedStatement ps =c.prepareStatement("SELECT * FROM users WHERE username LIKE ?" );
            ps.setString(  1,"%" + keyword + "%");
            ResultSet rs =ps.executeQuery();

            while (rs.next()) {
                modelUsers.addRow( new Object[]{
                                rs.getInt("id"),
                                rs.getString("username"),
                                rs.getString("platform")
                        }
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startCrawl() {
        new Thread(() -> {
            try {

                SwingUtilities.invokeLater(() -> {
                    progressBar.setVisible(true);
                    btnCrawl.setEnabled(false);
                    progressBar.setIndeterminate(true);
                    progressBar.setStringPainted(true);
                    progressBar.setString("Đang crawl...");
                });
                CrawlerService.startAutomation();
                log("Crawl hoàn tất!");
                SwingUtilities.invokeLater(() -> {
                    loadAnalysisByUser((String)cbUsers.getSelectedItem() );
                });
            } catch (Exception e) {
                e.printStackTrace();
                log("Crawl ERROR: " + e.getMessage());
            } finally {
            	SwingUtilities.invokeLater(() -> {
                    btnCrawl.setEnabled(true);
                    progressBar.setVisible(false);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(0);
                    progressBar.setString("");
                });
            }

        }).start();
    }
    
    public void runScheduledCrawl() {
        new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                    progressBar.setStringPainted(true);
                    progressBar.setString("Đang crawl tự động...");
                });

                CrawlerService.crawlAllUsers();
                log("Crawl tự động hoàn tất!");
                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                    progressBar.setString("Hoàn tất");
                    loadAnalysisByUser((String) cbUsers.getSelectedItem() );
                });

            } catch (Exception e) {
                e.printStackTrace();
                log("Scheduler ERROR: " + e.getMessage());
            } finally {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setVisible(false);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(0);
                    progressBar.setString("");
                });
            }

        }).start();
    }

    private void analyzeAI() {
        new Thread(() -> {

            try {

                SwingUtilities.invokeLater(() -> {
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                    progressBar.setStringPainted(true);
                    progressBar.setString("Đang phân tích AI...");
                });
                log("Bắt đầu phân tích AI...");
                CrawlerService.analyzeAllSubmissions();
                log("Phân tích AI hoàn tất!");
                SwingUtilities.invokeLater(() -> {
                    loadAnalysisByUser((String) cbUsers.getSelectedItem());
                });

            } catch (Exception e) {
                e.printStackTrace();
                log("AI Analyze ERROR: " + e.getMessage());

            } finally {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setVisible(false);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(0);
                    progressBar.setString("");
                });
            }
        }).start();
    }


    private void loadAnalysisByUser(String username) {
        try (Connection c = Database.getConn()) {
            modelAnalysis.setRowCount(0);
            String sql =
                    "SELECT * FROM submissions s "
                            + "JOIN users u "
                            + "ON s.user_id=u.id "
                            + "WHERE u.username=?";

            PreparedStatement ps =c.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs =ps.executeQuery();
            int total = 0;
            while (rs.next()) {
                total++;
                modelAnalysis.addRow(
                        new Object[]{
                                rs.getString("problem_name"),
                                rs.getString("language"),
                                rs.getString("algorithm"),
                                rs.getString("data_structure"),
                                rs.getString("time_complexity"),
                                rs.getInt("ai_probability") + "%",
                                rs.getString("ai_generated")
                        }
                );
            }
            lblTotal.setText( "Tổng bài: " + total
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        try {
            FlatDarkLaf.setup();

        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> {
            Database.initDatabase();
            SeleniumManager.init();
            new GiaoDien();
        });
    }
}