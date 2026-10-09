package app;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FreelancerDashboardPanel extends JPanel implements Closable {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final MainFrame frame;
    private final Freelancer freelancer;

    private final UserDao userDao = new UserDao();
    private final ProjectDao projectDao = new ProjectDao();
    private final TimeEntryDao timeEntryDao = new TimeEntryDao();
    private final InvoiceDao invoiceDao = new InvoiceDao();
    private final MessageDao messageDao = new MessageDao();
    private final BillingService billing = new BillingService();

    private List<Project> projects = new ArrayList<>();

    private final DefaultTableModel timeModel = Ui.readOnlyModel("Project", "Start", "End", "Hours");
    private final DefaultTableModel projectsModel = Ui.readOnlyModel("ID", "Name", "Client", "Deadline", "Status");
    private final DefaultTableModel invoicesModel =
            Ui.readOnlyModel("Invoice #", "Type", "Client", "Project", "Hours", "Total", "Status");
    private final DefaultTableModel messagesModel = Ui.readOnlyModel("To", "Message", "Sent");

    private final JTable projectsTable = new JTable(projectsModel);

    private final JComboBox<Project> timeProjectCombo = new JComboBox<>();
    private final JComboBox<Project> invoiceProjectCombo = new JComboBox<>();
    private final JComboBox<User> messageClientCombo = new JComboBox<>();

    private final JLabel timerLabel = new JLabel("00:00:00");
    private final JLabel deadlineLabel = new JLabel("Checking deadlines...");
    private final JButton startBtn = new JButton("Start Timer");
    private final JButton stopBtn = new JButton("Stop & Save");

    private StopwatchThread stopwatch;
    private LocalDateTime timerStart;
    private Project timerProject;
    private final DeadlineMonitor monitor;

    public FreelancerDashboardPanel(MainFrame frame, Freelancer freelancer) {
        this.frame = frame;
        this.freelancer = freelancer;
        setLayout(new BorderLayout());

        JLabel header = new JLabel(freelancer.getDashboardTitle(), SwingConstants.CENTER);
        header.setFont(new Font("SansSerif", Font.BOLD, 18));
        deadlineLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel top = new JPanel(new GridLayout(2, 1));
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        top.add(header);
        top.add(deadlineLabel);
        add(top, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Time Tracking", buildTimeTab());
        tabs.addTab("Projects", buildProjectsTab());
        tabs.addTab("Invoices", buildInvoicesTab());
        tabs.addTab("Messages", buildMessagesTab());
        add(tabs, BorderLayout.CENTER);

        JButton logoutBtn = new JButton("Log Out");
        logoutBtn.addActionListener(e -> this.frame.goHome());
        JPanel bottom = new JPanel();
        bottom.add(logoutBtn);
        add(bottom, BorderLayout.SOUTH);

        refreshProjects();
        refreshTimeEntries();
        refreshInvoices();
        refreshMessages();
        refreshClients();

        monitor = new DeadlineMonitor(freelancer.getId(),
                text -> SwingUtilities.invokeLater(() -> deadlineLabel.setText(text)));
        monitor.start();
    }

    // ------------------------------------------------------------------ Time tracking

    private JPanel buildTimeTab() {
        JPanel panel = new JPanel(new BorderLayout());

        timerLabel.setFont(new Font("Monospaced", Font.BOLD, 22));
        stopBtn.setEnabled(false);
        JTextField manualHoursField = new JTextField(5);
        JButton addManualBtn = new JButton("Add Manual Entry");

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        form.add(new JLabel("Project:"));
        form.add(timeProjectCombo);
        form.add(timerLabel);
        form.add(startBtn);
        form.add(stopBtn);
        form.add(new JLabel("   Or add hours manually:"));
        form.add(manualHoursField);
        form.add(addManualBtn);
        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(new JTable(timeModel)), BorderLayout.CENTER);

        startBtn.addActionListener(e -> startTimer());
        stopBtn.addActionListener(e -> stopTimer());
        addManualBtn.addActionListener(e -> {
            Project project = (Project) timeProjectCombo.getSelectedItem();
            try {
                double hours = Validator.parsePositive(manualHoursField.getText(), "Hours");
                LocalDateTime end = LocalDateTime.now();
                LocalDateTime start = end.minusSeconds((long) (hours * 3600));
                billing.logTime(project, start, end);
                manualHoursField.setText("");
                refreshTimeEntries();
            } catch (InvalidInputException | DatabaseException ex) {
                Ui.error(this, ex.getMessage());
            }
        });
        return panel;
    }

    private void startTimer() {
        Project project = (Project) timeProjectCombo.getSelectedItem();
        if (project == null) {
            Ui.error(this, "Create a project first.");
            return;
        }
        timerProject = project;
        timerStart = LocalDateTime.now();
        timerLabel.setText("00:00:00");

        stopwatch = new StopwatchThread(seconds ->
                SwingUtilities.invokeLater(() -> timerLabel.setText(formatSeconds(seconds))));
        stopwatch.start();

        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        timeProjectCombo.setEnabled(false);
    }

    private void stopTimer() {
        if (stopwatch == null) {
            return;
        }
        stopwatch.stopTimer();
        stopwatch = null;
        LocalDateTime end = LocalDateTime.now();

        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        timeProjectCombo.setEnabled(true);

        try {
            billing.logTime(timerProject, timerStart, end);
            Ui.info(this, "Time entry saved for " + timerProject.getName());
            refreshTimeEntries();
        } catch (InvalidInputException | DatabaseException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    private static String formatSeconds(long total) {
        return String.format("%02d:%02d:%02d", total / 3600, (total % 3600) / 60, total % 60);
    }

    // ------------------------------------------------------------------ Projects

    private JPanel buildProjectsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        projectsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(projectsTable), BorderLayout.CENTER);

        JButton newBtn = new JButton("New Project");
        JButton statusBtn = new JButton("Update Status");
        newBtn.addActionListener(e -> createProject());
        statusBtn.addActionListener(e -> updateProjectStatus());

        JPanel buttons = new JPanel();
        buttons.add(newBtn);
        buttons.add(statusBtn);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void createProject() {
        try {
            List<User> clients = userDao.findByRole("CLIENT");
            if (clients.isEmpty()) {
                Ui.error(this, "No clients exist yet. Register a client from the home screen first.");
                return;
            }
            JComboBox<User> clientCombo = new JComboBox<>(clients.toArray(new User[0]));
            JTextField nameField = new JTextField();
            JTextField descField = new JTextField();
            JSpinner daysSpinner = new JSpinner(new SpinnerNumberModel(14, 1, 365, 1));
            Object[] fields = {
                    "Client:", clientCombo,
                    "Project name:", nameField,
                    "Description:", descField,
                    "Deadline (days from today):", daysSpinner
            };
            int result = JOptionPane.showConfirmDialog(this, fields, "New Project", JOptionPane.OK_CANCEL_OPTION);
            if (result != JOptionPane.OK_OPTION) {
                return;
            }
            String name = Validator.requireText(nameField.getText(), "Project name");
            String description = descField.getText().trim();
            User client = (User) clientCombo.getSelectedItem();
            int days = (Integer) daysSpinner.getValue();

            Project project = new Project(0, name, description, LocalDate.now().plusDays(days),
                    Project.Status.NOT_STARTED, freelancer.getId(), client.getId());
            projectDao.save(project);
            refreshProjects();
        } catch (InvalidInputException | DatabaseException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    private void updateProjectStatus() {
        int row = projectsTable.getSelectedRow();
        if (row < 0) {
            Ui.error(this, "Select a project in the table first.");
            return;
        }
        Project selected = projects.get(row);
        Project.Status status = (Project.Status) JOptionPane.showInputDialog(this,
                "New status for " + selected.getName() + ":", "Update Status", JOptionPane.PLAIN_MESSAGE,
                null, Project.Status.values(), selected.getStatus());
        if (status == null) {
            return;
        }
        try {
            projectDao.updateStatus(selected.getId(), status);
            refreshProjects();
        } catch (DatabaseException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    // ------------------------------------------------------------------ Invoices

    private JPanel buildInvoicesTab() {
        JPanel panel = new JPanel(new BorderLayout());

        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"HOURLY", "FIXED"});
        JTextField amountField = new JTextField(8);
        JButton generateBtn = new JButton("Generate & Send Invoice");

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        form.add(new JLabel("Project:"));
        form.add(invoiceProjectCombo);
        form.add(new JLabel("Type:"));
        form.add(typeCombo);
        form.add(new JLabel("Rate per hour (HOURLY) / flat amount (FIXED):"));
        form.add(amountField);
        form.add(generateBtn);
        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(new JTable(invoicesModel)), BorderLayout.CENTER);

        generateBtn.addActionListener(e -> {
            Project project = (Project) invoiceProjectCombo.getSelectedItem();
            String type = (String) typeCombo.getSelectedItem();
            try {
                double amount = Validator.parsePositive(amountField.getText(), "Rate / amount");
                Invoice invoice = billing.generateInvoice(project, type, amount);
                Ui.info(this, "Invoice #" + invoice.getId() + " sent to " + invoice.getClientName()
                        + "\nTotal: " + Ui.fmt(invoice.calculateTotal()));
                amountField.setText("");
                refreshInvoices();
            } catch (InvalidInputException | DatabaseException ex) {
                Ui.error(this, ex.getMessage());
            }
        });
        return panel;
    }

    // ------------------------------------------------------------------ Messages

    private JPanel buildMessagesTab() {
        JPanel panel = new JPanel(new BorderLayout());

        JTextField messageField = new JTextField(30);
        JButton sendBtn = new JButton("Send Message");

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        form.add(new JLabel("To client:"));
        form.add(messageClientCombo);
        form.add(new JLabel("Message:"));
        form.add(messageField);
        form.add(sendBtn);
        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(new JTable(messagesModel)), BorderLayout.CENTER);

        sendBtn.addActionListener(e -> {
            User client = (User) messageClientCombo.getSelectedItem();
            if (client == null) {
                Ui.error(this, "No clients exist yet.");
                return;
            }
            try {
                String text = Validator.requireText(messageField.getText(), "Message");
                messageDao.save(new Message(0, freelancer.getId(), client.getId(), text, LocalDateTime.now()));
                messageField.setText("");
                refreshMessages();
            } catch (InvalidInputException | DatabaseException ex) {
                Ui.error(this, ex.getMessage());
            }
        });
        return panel;
    }

    // ------------------------------------------------------------------ Refresh helpers

    private void refreshProjects() {
        try {
            projects = projectDao.findByFreelancer(freelancer.getId());
            Ui.fill(projectsModel, projects, proj -> new Object[]{
                    proj.getId(), proj.getName(), proj.getClientName(), proj.getDeadline(), proj.getStatus()});
            timeProjectCombo.removeAllItems();
            invoiceProjectCombo.removeAllItems();
            for (Project proj : projects) {
                timeProjectCombo.addItem(proj);
                invoiceProjectCombo.addItem(proj);
            }
        } catch (DatabaseException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    private void refreshTimeEntries() {
        try {
            List<TimeEntry> entries = timeEntryDao.findByFreelancer(freelancer.getId());
            Ui.fill(timeModel, entries, t -> new Object[]{
                    t.getProjectName(), t.getStartTime().format(FMT), t.getEndTime().format(FMT),
                    Ui.fmt(t.getHoursWorked())});
        } catch (DatabaseException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    private void refreshInvoices() {
        try {
            List<Invoice> invoices = invoiceDao.findByFreelancer(freelancer.getId());
            Ui.fill(invoicesModel, invoices, inv -> new Object[]{
                    inv.getId(), inv.getType(), inv.getClientName(), inv.getProjectName(),
                    Ui.fmt(inv.getHours()), Ui.fmt(inv.calculateTotal()), inv.getStatus()});
        } catch (DatabaseException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    private void refreshMessages() {
        try {
            List<Message> sent = messageDao.findSentBy(freelancer.getId());
            Ui.fill(messagesModel, sent, m -> new Object[]{
                    m.getReceiverName(), m.getContent(), m.getSentAt().format(FMT)});
        } catch (DatabaseException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    private void refreshClients() {
        try {
            messageClientCombo.removeAllItems();
            for (User c : userDao.findByRole("CLIENT")) {
                messageClientCombo.addItem(c);
            }
        } catch (DatabaseException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    @Override
    public void onClose() {
        if (stopwatch != null) {
            stopwatch.stopTimer();
            stopwatch = null;
        }
        monitor.shutdown();
    }
}
