import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

 class OnlineVotingSystem {
    public static void main(String[] args) {
        new LoginForm();
    }
}

class DBConnection {
    static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection("jdbc:mysql://localhost:3306/voting_db", "root", "password");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

class LoginForm extends JFrame implements ActionListener {
    JTextField usernameField;
    JPasswordField passwordField;
    JButton loginButton;

    LoginForm() {
        setTitle("Login - Online Voting System");
        setSize(300, 200);
        setLayout(new GridLayout(3, 2));

        add(new JLabel("Username:"));
        usernameField = new JTextField();
        add(usernameField);

        add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        add(passwordField);

        loginButton = new JButton("Login");
        loginButton.addActionListener(this);
        add(loginButton);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM users WHERE username=? AND password=?");
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role");
                int userId = rs.getInt("id");
                dispose();
                if (role.equals("admin")) {
                    new AdminPanel();
                } else {
                    new AdminPanel.VoterPanel(userId);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

class AdminPanel extends JFrame implements ActionListener {
    JButton addCandidateBtn, viewResultsBtn;

    AdminPanel() {
        setTitle("Admin Panel");
        setSize(400, 200);
        setLayout(new FlowLayout());

        addCandidateBtn = new JButton("Add Candidate");
        addCandidateBtn.addActionListener(this);
        add(addCandidateBtn);

        viewResultsBtn = new JButton("View Results");
        viewResultsBtn.addActionListener(this);
        add(viewResultsBtn);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == addCandidateBtn) {
            String name = JOptionPane.showInputDialog("Enter Candidate Name:");
            String party = JOptionPane.showInputDialog("Enter Party Name:");

            try (Connection con = DBConnection.getConnection()) {
                PreparedStatement ps = con.prepareStatement("INSERT INTO candidates (name, party) VALUES (?, ?)");
                ps.setString(1, name);
                ps.setString(2, party);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Candidate Added.");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (e.getSource() == viewResultsBtn) {
            new ResultsFrame();
        }
    }

    static class VoterPanel extends JFrame implements ActionListener {
        int userId;
        ButtonGroup group;
        JPanel panel;

        VoterPanel(int userId) {
            this.userId = userId;
            setTitle("Cast Your Vote");
            setSize(400, 300);
            panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            group = new ButtonGroup();
            try (Connection con = DBConnection.getConnection()) {
                PreparedStatement ps = con.prepareStatement("SELECT * FROM users WHERE id=?");
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getBoolean("has_voted")) {
                    JLabel msg = new JLabel("You have already voted.");
                    panel.add(msg);
                } else {
                    PreparedStatement ps2 = con.prepareStatement("SELECT * FROM candidates");
                    ResultSet rs2 = ps2.executeQuery();
                    while (rs2.next()) {
                        JRadioButton rb = new JRadioButton(rs2.getInt("id") + ": " + rs2.getString("name") + " - " + rs2.getString("party"));
                        rb.setActionCommand(String.valueOf(rs2.getInt("id")));
                        group.add(rb);
                        panel.add(rb);
                    }
                    JButton voteBtn = new JButton("Vote");
                    voteBtn.addActionListener(this);
                    panel.add(voteBtn);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            add(panel);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setVisible(true);
        }

        public void actionPerformed(ActionEvent e) {
            String selected = group.getSelection().getActionCommand();
            int candidateId = Integer.parseInt(selected);

            try (Connection con = DBConnection.getConnection()) {
                PreparedStatement ps = con.prepareStatement("INSERT INTO votes (user_id, candidate_id) VALUES (?, ?)");
                ps.setInt(1, userId);
                ps.setInt(2, candidateId);
                ps.executeUpdate();

                PreparedStatement ps2 = con.prepareStatement("UPDATE candidates SET votes = votes + 1 WHERE id = ?");
                ps2.setInt(1, candidateId);
                ps2.executeUpdate();

                PreparedStatement ps3 = con.prepareStatement("UPDATE users SET has_voted = TRUE WHERE id = ?");
                ps3.setInt(1, userId);
                ps3.executeUpdate();

                JOptionPane.showMessageDialog(this, "Your vote has been recorded.");
                dispose();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}

class ResultsFrame extends JFrame {
    ResultsFrame() {
        setTitle("Election Results");
        setSize(400, 300);
        setLayout(new BorderLayout());

        JTextArea area = new JTextArea();
        area.setEditable(false);

        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM candidates ORDER BY votes DESC");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                area.append(rs.getString("name") + " (" + rs.getString("party") + ") - Votes: " + rs.getInt("votes") + "\n");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        add(new JScrollPane(area), BorderLayout.CENTER);
        setVisible(true);
    }
}
