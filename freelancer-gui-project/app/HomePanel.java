package app;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

public class HomePanel extends JPanel {

    private final MainFrame frame;
    private final UserDao userDao = new UserDao();

    public HomePanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Time Management for Freelancers", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        gbc.gridy = 0;
        add(title, gbc);

        JButton regFreelancer = button("Register as Freelancer");
        JButton regClient = button("Register as Client");
        JButton loginFreelancer = button("Freelancer Login");
        JButton loginClient = button("Client Login");

        regFreelancer.addActionListener(e -> register("FREELANCER"));
        regClient.addActionListener(e -> register("CLIENT"));
        loginFreelancer.addActionListener(e -> login("FREELANCER"));
        loginClient.addActionListener(e -> login("CLIENT"));

        gbc.gridy = 1;
        add(regFreelancer, gbc);
        gbc.gridy = 2;
        add(regClient, gbc);
        gbc.gridy = 3;
        add(loginFreelancer, gbc);
        gbc.gridy = 4;
        add(loginClient, gbc);
    }

    private JButton button(String text) {
        JButton b = new JButton(text);
        b.setPreferredSize(new Dimension(260, 40));
        return b;
    }

    private void register(String role) {
        JTextField nameField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        Object[] fields = {"Name:", nameField, "Email:", emailField};

        int result = JOptionPane.showConfirmDialog(this, fields,
                "Register as " + role.toLowerCase(), JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            String name = Validator.requireText(nameField.getText(), "Name");
            String email = Validator.requireEmail(emailField.getText());
            User user;
            if ("CLIENT".equals(role)) {
                user = new Client(name, email);
            } else {
                user = new Freelancer(name, email);
            }
            userDao.save(user);
            Ui.info(this, "Registered successfully: " + user.getName());
        } catch (InvalidInputException | DatabaseException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    private void login(String role) {
        try {
            List<User> users = userDao.findByRole(role);
            if (users.isEmpty()) {
                Ui.error(this, "No " + role.toLowerCase() + "s are registered yet. Please register first.");
                return;
            }
            User chosen = (User) JOptionPane.showInputDialog(this, "Select your account:", "Login",
                    JOptionPane.PLAIN_MESSAGE, null, users.toArray(), users.get(0));
            if (chosen == null) {
                return;
            }
            if (chosen instanceof Freelancer) {
                frame.showDashboard(new FreelancerDashboardPanel(frame, (Freelancer) chosen));
            } else {
                frame.showDashboard(new ClientDashboardPanel(frame, (Client) chosen));
            }
        } catch (DatabaseException ex) {
            Ui.error(this, ex.getMessage());
        }
    }
}
