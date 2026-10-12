package app;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Optional;

/** Landing screen: a branded panel on the left and a login / register card on the right. */
public class HomePanel extends JPanel {

    private final MainFrame frame;
    private final UserDao userDao = new UserDao();

    private String role = "FREELANCER";
    private boolean registerMode = false;

    private final RoundedButton freelancerToggle = new RoundedButton("Freelancer", RoundedButton.Style.SEGMENT);
    private final RoundedButton clientToggle = new RoundedButton("Client", RoundedButton.Style.SEGMENT);
    private final RoundedButton switchLink = new RoundedButton(" ", RoundedButton.Style.LINK);
    private final RoundedButton registerBtn = new RoundedButton("Create account", RoundedButton.Style.PRIMARY);

    private final JLabel heading = new JLabel();
    private final JLabel subheading = new JLabel();

    private final CardLayout formCards = new CardLayout();
    private final JPanel formArea = new JPanel(formCards);

    private final JTextField loginEmail = Theme.textField(22);
    private final JPasswordField loginPassword = Theme.passwordField(22);

    private final JTextField regName = Theme.textField(22);
    private final JTextField regEmail = Theme.textField(22);
    private final JPasswordField regPassword = Theme.passwordField(22);
    private final JPasswordField regConfirm = Theme.passwordField(22);

    private final JLabel nameHint = Theme.hintLabel();
    private final JLabel emailHint = Theme.hintLabel();
    private final JLabel passwordHint = Theme.hintLabel();
    private final JLabel confirmHint = Theme.hintLabel();

    public HomePanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        add(buildBrandPanel(), BorderLayout.WEST);
        add(buildFormSide(), BorderLayout.CENTER);
        setRole("FREELANCER");
        showMode(false);
        validateRegisterForm();
    }

    // ------------------------------------------------------------------ layout

    private JPanel buildBrandPanel() {
        JPanel brand = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, Theme.PRIMARY, 0, getHeight(), Theme.SIDEBAR));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        brand.setPreferredSize(new Dimension(440, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(6, 48, 6, 48);

        gbc.gridy = 0;
        brand.add(light("<html>Time Management<br>for Freelancers</html>",
                new Font("Segoe UI", Font.BOLD, 32), Color.WHITE), gbc);
        gbc.gridy = 1;
        gbc.insets = new Insets(10, 48, 30, 48);
        brand.add(light("Track hours. Manage projects. Get paid.",
                Theme.BASE.deriveFont(16f), new Color(186, 230, 253)), gbc);

        gbc.insets = new Insets(5, 48, 5, 48);
        String[] points = {
                "Live timer for billable hours",
                "Projects with deadlines and status",
                "Hourly or fixed-price invoices",
                "Clients see progress in real time"
        };
        for (int i = 0; i < points.length; i++) {
            gbc.gridy = 2 + i;
            brand.add(light("\u2022  " + points[i], Theme.BASE, Color.WHITE), gbc);
        }
        return brand;
    }

    private JPanel buildFormSide() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(Theme.BACKGROUND);

        // theme switch in the top-right corner
        RoundedButton themeBtn = new RoundedButton(Theme.isDark() ? "Light mode" : "Dark mode",
                RoundedButton.Style.LINK);
        themeBtn.addActionListener(e -> frame.toggleTheme());
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 12));
        topBar.setOpaque(false);
        topBar.add(themeBtn);
        side.add(topBar, BorderLayout.NORTH);

        RoundedPanel card = new RoundedPanel();
        card.setLayout(new BorderLayout(0, 16));
        card.setPadding(30, 34, 24, 34);

        heading.setFont(Theme.HEADING);
        heading.setForeground(Theme.TEXT);
        subheading.setFont(Theme.BASE);
        subheading.setForeground(Theme.MUTED);
        JPanel texts = new JPanel(new GridLayout(2, 1, 0, 2));
        texts.setOpaque(false);
        texts.add(heading);
        texts.add(subheading);

        JPanel toggles = new JPanel(new GridLayout(1, 2, 8, 0));
        toggles.setOpaque(false);
        toggles.add(freelancerToggle);
        toggles.add(clientToggle);
        freelancerToggle.addActionListener(e -> setRole("FREELANCER"));
        clientToggle.addActionListener(e -> setRole("CLIENT"));

        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);
        top.add(texts, BorderLayout.NORTH);
        top.add(toggles, BorderLayout.CENTER);
        card.add(top, BorderLayout.NORTH);

        formArea.setOpaque(false);
        formArea.add(buildLoginForm(), "login");
        formArea.add(buildRegisterForm(), "register");
        card.add(formArea, BorderLayout.CENTER);

        switchLink.addActionListener(e -> showMode(!registerMode));
        JPanel linkRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        linkRow.setOpaque(false);
        linkRow.add(switchLink);
        card.add(linkRow, BorderLayout.SOUTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(card);
        side.add(center, BorderLayout.CENTER);
        return side;
    }

    private JPanel buildLoginForm() {
        JPanel form = Theme.formPanel();
        Theme.addField(form, "EMAIL", loginEmail);
        Theme.addField(form, "PASSWORD", loginPassword);
        RoundedButton loginBtn = new RoundedButton("Log in", RoundedButton.Style.PRIMARY);
        loginBtn.addActionListener(e -> doLogin());
        loginPassword.addActionListener(e -> doLogin());
        Theme.addWide(form, loginBtn, 22);
        return form;
    }

    private JPanel buildRegisterForm() {
        JPanel form = Theme.formPanel();
        Theme.addField(form, "FULL NAME", regName);
        Theme.addHint(form, nameHint);
        Theme.addField(form, "EMAIL", regEmail);
        Theme.addHint(form, emailHint);
        Theme.addField(form, "CREATE PASSWORD", regPassword);
        Theme.addHint(form, passwordHint);
        Theme.addField(form, "CONFIRM PASSWORD", regConfirm);
        Theme.addHint(form, confirmHint);

        registerBtn.addActionListener(e -> doRegister());
        regConfirm.addActionListener(e -> doRegister());
        Theme.addWide(form, registerBtn, 16);

        // live feedback while typing
        DocumentListener live = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validateRegisterForm();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validateRegisterForm();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validateRegisterForm();
            }
        };
        for (JTextComponent field : new JTextComponent[]{regName, regEmail, regPassword, regConfirm}) {
            field.getDocument().addDocumentListener(live);
        }
        return form;
    }

    private JLabel light(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    // ------------------------------------------------------------------ live validation

    private void setHint(JLabel hint, String text, Color color) {
        hint.setText(text);
        hint.setForeground(color);
    }

    /** Updates the hint lines under the register fields and enables the button only when everything is valid. */
    private void validateRegisterForm() {
        boolean nameOk = !regName.getText().trim().isEmpty();
        if (nameOk) {
            setHint(nameHint, " ", Theme.MUTED);
        } else {
            setHint(nameHint, "Enter your name", Theme.MUTED);
        }

        String email = regEmail.getText().trim();
        boolean emailOk = Validator.isValidEmail(email);
        if (email.isEmpty()) {
            setHint(emailHint, "e.g. name@example.com", Theme.MUTED);
        } else if (!emailOk) {
            setHint(emailHint, "That does not look like a valid email address", Theme.DANGER);
        } else {
            setHint(emailHint, "Email looks good", Theme.SUCCESS);
        }

        String password = new String(regPassword.getPassword());
        boolean passwordOk = password.length() >= 6;
        if (password.isEmpty()) {
            setHint(passwordHint, "At least 6 characters", Theme.MUTED);
        } else if (!passwordOk) {
            setHint(passwordHint, (6 - password.length()) + " more character(s) needed", Theme.DANGER);
        } else if (password.length() >= 10 && password.matches(".*\\d.*") && password.matches(".*[A-Za-z].*")) {
            setHint(passwordHint, "Strong password", Theme.SUCCESS);
        } else {
            setHint(passwordHint, "Password accepted", Theme.SUCCESS);
        }

        String confirm = new String(regConfirm.getPassword());
        boolean confirmOk = !confirm.isEmpty() && confirm.equals(password);
        if (confirm.isEmpty()) {
            setHint(confirmHint, " ", Theme.MUTED);
        } else if (!confirmOk) {
            setHint(confirmHint, "Passwords do not match", Theme.DANGER);
        } else {
            setHint(confirmHint, "Passwords match", Theme.SUCCESS);
        }

        registerBtn.setEnabled(nameOk && emailOk && passwordOk && confirmOk);
    }

    // ------------------------------------------------------------------ state

    private void setRole(String newRole) {
        role = newRole;
        freelancerToggle.setActive("FREELANCER".equals(role));
        clientToggle.setActive("CLIENT".equals(role));
        updateTexts();
    }

    private void showMode(boolean register) {
        registerMode = register;
        formCards.show(formArea, register ? "register" : "login");
        switchLink.setText(register ? "Already have an account? Log in" : "New here? Create an account");
        switchLink.revalidate();
        updateTexts();
    }

    private void updateTexts() {
        boolean freelancer = "FREELANCER".equals(role);
        if (registerMode) {
            heading.setText("Create your account");
            subheading.setText(freelancer
                    ? "Start tracking your hours and invoicing clients."
                    : "Follow your projects and review invoices in one place.");
        } else {
            heading.setText("Welcome back");
            subheading.setText(freelancer
                    ? "Log in to your freelancer dashboard."
                    : "Log in to your client dashboard.");
        }
    }

    // ------------------------------------------------------------------ actions

    private void doRegister() {
        if (!registerBtn.isEnabled()) {
            return;
        }
        try {
            String name = Validator.requireText(regName.getText(), "Name");
            String email = Validator.requireEmail(regEmail.getText()).toLowerCase();
            String password = Validator.requirePassword(new String(regPassword.getPassword()));
            String confirm = new String(regConfirm.getPassword());
            if (!password.equals(confirm)) {
                throw new InvalidInputException("Passwords do not match.");
            }

            User user;
            if ("CLIENT".equals(role)) {
                user = new Client(name, email);
            } else {
                user = new Freelancer(name, email);
            }
            user.setPasswordHash(PasswordUtil.hash(password));
            userDao.save(user);

            regName.setText("");
            regEmail.setText("");
            regPassword.setText("");
            regConfirm.setText("");
            loginEmail.setText(email);
            loginPassword.setText("");
            showMode(false);
            Ui.toast(this, "Account created for " + user.getName() + ". You can log in now.");
        } catch (InvalidInputException | DatabaseException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    private void doLogin() {
        try {
            String email = Validator.requireText(loginEmail.getText(), "Email");
            String password = new String(loginPassword.getPassword());

            Optional<User> found = userDao.findByEmail(email);
            boolean valid = found.isPresent()
                    && role.equals(found.get().getRole())
                    && PasswordUtil.verify(password, found.get().getPasswordHash());
            if (!valid) {
                // Same message for every failure so it doesn't reveal which emails exist.
                Ui.error(this, "Invalid email or password for a " + role.toLowerCase() + " account.");
                return;
            }

            User user = found.get();
            loginPassword.setText("");
            Session.login(user);
            if (user instanceof Freelancer) {
                frame.showDashboard(new FreelancerDashboardPanel(frame, (Freelancer) user));
            } else {
                frame.showDashboard(new ClientDashboardPanel(frame, (Client) user));
            }
        } catch (InvalidInputException | DatabaseException ex) {
            Ui.error(this, ex.getMessage());
        }
    }
}
