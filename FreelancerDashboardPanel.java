package app;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FreelancerDashboardPanel extends DashboardShell implements Closable {

    private final MainFrame frame;
    private final Freelancer freelancer;

    private final UserDao userDao = new UserDao();
    private final ProjectDao projectDao = new ProjectDao();
    private final TimeEntryDao timeEntryDao = new TimeEntryDao();
    private final InvoiceDao invoiceDao = new InvoiceDao();
    private final BillingService billing = new BillingService();

    private List<Project> projects = new ArrayList<>();
    private List<TimeEntry> entries = new ArrayList<>();
    private List<Invoice> invoices = new ArrayList<>();

    private final DefaultTableModel timeModel = Ui.readOnlyModel("Project", "Start", "End", "Hours");
    private final DefaultTableModel projectsModel = Ui.readOnlyModel("ID", "Name", "Client", "Deadline", "Status");
    private final DefaultTableModel invoicesModel =
            Ui.readOnlyModel("Invoice #", "Type", "Client", "Project", "Hours", "Total", "Status");

    private final JTable projectsTable = new JTable(projectsModel);
    private final JTable invoicesTable = new JTable(invoicesModel);

    private final JComboBox<Project> timeProjectCombo = Theme.styleCombo(new JComboBox<Project>());
    private final JComboBox<Project> invoiceProjectCombo = Theme.styleCombo(new JComboBox<Project>());

    private final JTextField invoiceAmountField = Theme.numericField(7, 2);
    private final JTextField fromField = Theme.textField(9);
    private final JTextField toField = Theme.textField(9);
    private final JLabel rangeLabel = Theme.label("-");

    private final JLabel timerLabel = new JLabel("00:00:00", SwingConstants.CENTER);
    private final JLabel deadlineLabel = new JLabel("Checking deadlines...");
    private final AnimatedNumberLabel hoursValue = new AnimatedNumberLabel("0.00");
    private final AnimatedNumberLabel projectsValue = new AnimatedNumberLabel("0");
    private final AnimatedNumberLabel pendingValue = new AnimatedNumberLabel("0.00");

    private final BarChartPanel hoursChart =
            new BarChartPanel(Theme.PRIMARY, "%.1fh", "Log some time to see hours per project");
    private final DonutChartPanel invoiceDonut = new DonutChartPanel("No invoices yet");

    private final RoundedButton startBtn = new RoundedButton("Start Timer", RoundedButton.Style.PRIMARY);
    private final RoundedButton stopBtn = new RoundedButton("Stop & Save", RoundedButton.Style.DANGER);

    private StopwatchThread stopwatch;
    private LocalDateTime timerStart;
    private Project timerProject;
    private final ActivityMonitor monitor;

    public FreelancerDashboardPanel(MainFrame frame, Freelancer freelancer) {
        super(freelancer.getName(), "Freelancer", "FREELANCER", frame::goHome, frame::toggleTheme);
        this.frame = frame;
        this.freelancer = freelancer;

        addPage("Overview", "Overview", buildOverviewPage(), this::refreshOverview);
        addPage("Time Tracking", "Time Tracking", buildTimePage(), null);
        addPage("Projects", "My Projects", buildProjectsPage(), null);
        addPage("Invoices", "Invoices", buildInvoicesPage(), null);
        LeaderboardPage leaderboard = new LeaderboardPage(freelancer.getId());
        addPage("Leaderboard", "Leaderboard", leaderboard, leaderboard::refresh);
        MessagesPage messages = new MessagesPage(freelancer, "CLIENT", count -> setBadge("Messages", count));
        addPage("Messages", "Messages", messages, messages::refresh);

        refreshProjects();
        refreshTimeEntries();
        refreshInvoices();

        monitor = new ActivityMonitor(freelancer,
                text -> SwingUtilities.invokeLater(() -> {
                    deadlineLabel.setText(text);
                    deadlineLabel.setForeground(text.startsWith("Warning") || text.startsWith("Deadline check failed")
                            ? Theme.DANGER : Theme.SUCCESS);
                }),
                alert -> SwingUtilities.invokeLater(() -> Ui.toast(this, alert)),
                count -> SwingUtilities.invokeLater(() -> setBadge("Messages", count)));
        monitor.start();
    }

    /** True while the live timer is counting (used to keep the app alive in the system tray). */
    public boolean isTimerRunning() {
        return stopwatch != null;
    }

    private static void onChange(JTextField field, Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                action.run();
            }
        });
    }

    // ------------------------------------------------------------------ Overview

    private JPanel buildOverviewPage() {
        JPanel page = new JPanel(new BorderLayout(0, 2));
        page.setOpaque(false);

        JPanel stats = new JPanel(new GridLayout(1, 3, 0, 0));
        stats.setOpaque(false);
        stats.add(Theme.statCard("Hours logged", hoursValue, Theme.PRIMARY));
        stats.add(Theme.statCard("Active projects", projectsValue, Theme.WARNING));
        stats.add(Theme.statCard("Pending invoice amount", pendingValue, Theme.SUCCESS));

        JPanel top = new JPanel(new BorderLayout(0, 2));
        top.setOpaque(false);
        top.add(Theme.banner(Theme.greeting() + ", " + Theme.firstName(freelancer.getName()),
                Theme.todayText()), BorderLayout.NORTH);
        top.add(stats, BorderLayout.CENTER);
        page.add(top, BorderLayout.NORTH);

        deadlineLabel.setFont(Theme.BOLD);
        JPanel deadlineCard = Theme.card();
        deadlineCard.setLayout(new BorderLayout(0, 6));
        deadlineCard.add(Theme.sectionLabel("DEADLINES AND ALERTS"), BorderLayout.NORTH);
        deadlineCard.add(deadlineLabel, BorderLayout.CENTER);

        JPanel charts = new JPanel(new GridLayout(1, 2, 0, 0));
        charts.setOpaque(false);
        charts.add(Theme.chartCard("HOURS BY PROJECT", hoursChart));
        charts.add(Theme.chartCard("INVOICES: PAID VS PENDING", invoiceDonut));

        JPanel middle = new JPanel(new BorderLayout(0, 2));
        middle.setOpaque(false);
        middle.add(deadlineCard, BorderLayout.NORTH);
        middle.add(charts, BorderLayout.CENTER);
        page.add(middle, BorderLayout.CENTER);
        return page;
    }

    private void refreshOverview() {
        double hours = 0;
        Map<String, Double> hoursByProject = new LinkedHashMap<>();
        for (TimeEntry t : entries) {
            hours += t.getHoursWorked();
            hoursByProject.merge(t.getProjectName(), t.getHoursWorked(), Double::sum);
        }
        long active = projects.stream().filter(p -> p.getStatus() != Project.Status.COMPLETED).count();

        double paid = 0;
        double pending = 0;
        for (Invoice inv : invoices) {
            if (inv.getStatus() == Invoice.Status.PAID) {
                paid += inv.calculateTotal();
            } else {
                pending += inv.calculateTotal();
            }
        }
        hoursValue.animateTo(hours, 2);
        projectsValue.animateTo(active, 0);
        pendingValue.animateTo(pending, 2);
        hoursChart.setData(hoursByProject);
        invoiceDonut.setData(paid, pending);
    }

    // ------------------------------------------------------------------ Time tracking

    private JPanel buildTimePage() {
        JPanel page = new JPanel(new BorderLayout(0, 2));
        page.setOpaque(false);

        timerLabel.setFont(Theme.TIMER);
        timerLabel.setForeground(Theme.PRIMARY);
        timerLabel.setPreferredSize(new Dimension(210, 60));
        stopBtn.setEnabled(false);

        JTextField manualHoursField = Theme.numericField(5, 2);
        RoundedButton addManualBtn = new RoundedButton("Add Entry", RoundedButton.Style.SECONDARY);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row1.setOpaque(false);
        row1.add(timeProjectCombo);
        row1.add(startBtn);
        row1.add(stopBtn);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row2.setOpaque(false);
        row2.add(Theme.label("Or add hours manually:"));
        row2.add(manualHoursField);
        row2.add(addManualBtn);

        JPanel controls = new JPanel(new GridLayout(2, 1, 0, 12));
        controls.setOpaque(false);
        controls.add(row1);
        controls.add(row2);

        JPanel timerCard = Theme.card();
        timerCard.setLayout(new BorderLayout(20, 0));
        timerCard.add(timerLabel, BorderLayout.WEST);
        timerCard.add(controls, BorderLayout.CENTER);

        RoundedButton exportBtn = new RoundedButton("Export Timesheet (CSV)", RoundedButton.Style.SECONDARY);
        exportBtn.addActionListener(e -> ReportExporter.exportTimesheet(this, entries));
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.setOpaque(false);
        toolbar.add(exportBtn);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(toolbar, BorderLayout.NORTH);
        center.add(Theme.tableCard(new JTable(timeModel), EmptyState.Kind.CLOCK,
                "No time logged yet", "Start the timer or add hours manually."), BorderLayout.CENTER);

        page.add(timerCard, BorderLayout.NORTH);
        page.add(center, BorderLayout.CENTER);

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
                Ui.toast(this, "Logged " + Ui.fmt(hours) + " hours");
                refreshTimeEntries();
            } catch (InvalidInputException | DatabaseException ex) {
                Ui.error(this, ex.getMessage());
            }
        });
        return page;
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

        stopwatch = new StopwatchThread(seconds -> SwingUtilities.invokeLater(() -> {
            String shown = formatSeconds(seconds);
            timerLabel.setText(shown);
            frame.updateTrayTooltip("Timer running: " + shown + " (" + timerProject.getName() + ")");
        }));
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
            Ui.toast(this, "Time entry saved for " + timerProject.getName());
            refreshTimeEntries();
        } catch (InvalidInputException | DatabaseException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    private static String formatSeconds(long total) {
        return String.format("%02d:%02d:%02d", total / 3600, (total % 3600) / 60, total % 60);
    }

    // ------------------------------------------------------------------ Projects

    private JPanel buildProjectsPage() {
        JPanel page = new JPanel(new BorderLayout(0, 8));
        page.setOpaque(false);

        RoundedButton newBtn = new RoundedButton("+ New Project", RoundedButton.Style.PRIMARY);
        RoundedButton statusBtn = new RoundedButton("Update Status", RoundedButton.Style.SECONDARY);
        newBtn.addActionListener(e -> createProject());
        statusBtn.addActionListener(e -> updateProjectStatus());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(newBtn);
        buttons.add(statusBtn);

        projectsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        page.add(buttons, BorderLayout.NORTH);
        page.add(Theme.tableCard(projectsTable, EmptyState.Kind.FOLDER,
                "No projects yet", "Create your first project to start tracking time."), BorderLayout.CENTER);
        return page;
    }

    private void createProject() {
        try {
            Session.requireUser(freelancer.getId());
            List<User> clients = userDao.findByRole("CLIENT");
            if (clients.isEmpty()) {
                Ui.error(this, "No clients exist yet. Ask a client to register first.");
                return;
            }
            JComboBox<User> clientCombo = Theme.styleCombo(new JComboBox<User>(clients.toArray(new User[0])));
            JTextField nameField = Theme.textField(24);
            JTextField descField = Theme.textField(24);
            JSpinner daysSpinner = Theme.styleSpinner(new JSpinner(new SpinnerNumberModel(14, 1, 365, 1)));

            JPanel form = Theme.formPanel();
            Theme.addField(form, "CLIENT", clientCombo);
            Theme.addField(form, "PROJECT NAME", nameField);
            Theme.addField(form, "DESCRIPTION", descField);
            Theme.addField(form, "DEADLINE (DAYS FROM TODAY)", daysSpinner);

            int result = JOptionPane.showConfirmDialog(this, form, "New Project",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
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
            Ui.toast(this, "Project \"" + name + "\" created");
            refreshProjects();
        } catch (InvalidInputException | DatabaseException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    private void updateProjectStatus() {
        int viewRow = projectsTable.getSelectedRow();
        if (viewRow < 0) {
            Ui.error(this, "Select a project in the table first.");
            return;
        }
        Project selected = projects.get(projectsTable.convertRowIndexToModel(viewRow));
        Session.requireUser(selected.getFreelancerId());
        Project.Status status = (Project.Status) JOptionPane.showInputDialog(this,
                "New status for " + selected.getName() + ":", "Update Status", JOptionPane.PLAIN_MESSAGE,
                null, Project.Status.values(), selected.getStatus());
        if (status == null) {
            return;
        }
        try {
            projectDao.updateStatus(selected.getId(), status);
            Ui.toast(this, "Status updated");
            refreshProjects();
        } catch (DatabaseException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    // ------------------------------------------------------------------ Invoices

    private JPanel buildInvoicesPage() {
        JPanel page = new JPanel(new BorderLayout(0, 2));
        page.setOpaque(false);

        JComboBox<String> typeCombo = Theme.styleCombo(new JComboBox<String>(new String[]{"HOURLY", "FIXED"}));
        typeCombo.setPreferredSize(new Dimension(120, 42));
        invoiceProjectCombo.setPreferredSize(new Dimension(200, 42));
        RoundedButton generateBtn = new RoundedButton("Generate & Send", RoundedButton.Style.PRIMARY);
        RoundedButton monthBtn = new RoundedButton("This month", RoundedButton.Style.SECONDARY);
        RoundedButton allTimeBtn = new RoundedButton("All time", RoundedButton.Style.SECONDARY);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        row1.setOpaque(false);
        row1.add(Theme.labeled("PROJECT", invoiceProjectCombo));
        row1.add(Theme.labeled("TYPE", typeCombo));
        row1.add(Theme.labeled("RATE / AMOUNT", invoiceAmountField));
        row1.add(Theme.labeled(" ", generateBtn));

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        row2.setOpaque(false);
        row2.add(Theme.labeled("FROM (YYYY-MM-DD)", fromField));
        row2.add(Theme.labeled("TO (YYYY-MM-DD)", toField));
        row2.add(Theme.labeled(" ", monthBtn));
        row2.add(Theme.labeled(" ", allTimeBtn));
        row2.add(Theme.labeled("HOURS IN PERIOD", rangeLabel));

        JPanel form = Theme.card();
        form.setLayout(new GridLayout(2, 1, 0, 12));
        form.add(row1);
        form.add(row2);

        RoundedButton exportBtn = new RoundedButton("Export Invoice (PDF/TXT)", RoundedButton.Style.SECONDARY);
        RoundedButton earningsBtn = new RoundedButton("Export Earnings (CSV)", RoundedButton.Style.SECONDARY);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.setOpaque(false);
        toolbar.add(exportBtn);
        toolbar.add(earningsBtn);
        toolbar.add(Theme.label("Double-click an invoice to see its details."));

        invoicesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(toolbar, BorderLayout.NORTH);
        center.add(Theme.tableCard(invoicesTable, EmptyState.Kind.DOCUMENT,
                "No invoices yet", "Choose a project and period above, then generate one."), BorderLayout.CENTER);

        page.add(form, BorderLayout.NORTH);
        page.add(center, BorderLayout.CENTER);

        // live preview of the hours that will be billed
        onChange(fromField, this::updateRangePreview);
        onChange(toField, this::updateRangePreview);
        invoiceProjectCombo.addActionListener(e -> updateRangePreview());
        monthBtn.addActionListener(e -> {
            LocalDate today = LocalDate.now();
            fromField.setText(today.withDayOfMonth(1).toString());
            toField.setText(today.toString());
        });
        allTimeBtn.addActionListener(e -> {
            fromField.setText("");
            toField.setText("");
        });

        generateBtn.addActionListener(e -> {
            Project project = (Project) invoiceProjectCombo.getSelectedItem();
            String type = (String) typeCombo.getSelectedItem();
            try {
                double amount = Validator.parsePositive(invoiceAmountField.getText(), "Rate / amount");
                LocalDate from = Validator.parseOptionalDate(fromField.getText(), "From date");
                LocalDate to = Validator.parseOptionalDate(toField.getText(), "To date");
                Invoice invoice = billing.generateInvoice(project, type, amount, from, to);
                Ui.toast(this, "Invoice #" + invoice.getId() + " sent to " + invoice.getClientName()
                        + " (total " + Ui.fmt(invoice.calculateTotal()) + ")");
                invoiceAmountField.setText("");
                refreshInvoices();
            } catch (InvalidInputException | DatabaseException ex) {
                Ui.error(this, ex.getMessage());
            }
        });

        exportBtn.addActionListener(e -> {
            Invoice selected = selectedInvoice();
            if (selected == null) {
                Ui.error(this, "Select an invoice in the table first.");
                return;
            }
            InvoiceExporter.export(this, selected);
        });
        earningsBtn.addActionListener(e -> ReportExporter.exportEarnings(this, invoices));

        invoicesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Invoice selected = selectedInvoice();
                    if (selected != null) {
                        Ui.info(FreelancerDashboardPanel.this, selected.describe());
                    }
                }
            }
        });
        return page;
    }

    private void updateRangePreview() {
        Project project = (Project) invoiceProjectCombo.getSelectedItem();
        if (project == null) {
            rangeLabel.setForeground(Theme.MUTED);
            rangeLabel.setText("-");
            return;
        }
        try {
            LocalDate from = Validator.parseOptionalDate(fromField.getText(), "From date");
            LocalDate to = Validator.parseOptionalDate(toField.getText(), "To date");
            double hours = BillingService.sumHours(entries, project.getId(), from, to);
            rangeLabel.setForeground(Theme.TEXT);
            rangeLabel.setText(Ui.fmt(hours) + " h");
        } catch (InvalidInputException ex) {
            rangeLabel.setForeground(Theme.DANGER);
            rangeLabel.setText("Check the dates");
        }
    }

    private Invoice selectedInvoice() {
        int viewRow = invoicesTable.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        return invoices.get(invoicesTable.convertRowIndexToModel(viewRow));
    }

    // ------------------------------------------------------------------ Refresh helpers

    private void refreshProjects() {
        try {
            projects = projectDao.findByFreelancer(freelancer.getId());
            Ui.fill(projectsModel, projects, proj -> new Object[]{
                    proj.getId(), proj.getName(), proj.getClientName(), proj.getDeadline(), Badge.of(proj)});
            timeProjectCombo.removeAllItems();
            invoiceProjectCombo.removeAllItems();
            for (Project proj : projects) {
                timeProjectCombo.addItem(proj);
                invoiceProjectCombo.addItem(proj);
            }
            updateRangePreview();
            refreshOverview();
        } catch (DatabaseException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    private void refreshTimeEntries() {
        try {
            entries = timeEntryDao.findByFreelancer(freelancer.getId());
            Ui.fill(timeModel, entries, t -> new Object[]{
                    t.getProjectName(), t.getStartTime(), t.getEndTime(), t.getHoursWorked()});
            updateRangePreview();
            refreshOverview();
        } catch (DatabaseException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    private void refreshInvoices() {
        try {
            invoices = invoiceDao.findByFreelancer(freelancer.getId());
            Ui.fill(invoicesModel, invoices, inv -> new Object[]{
                    inv.getId(), inv.getType(), inv.getClientName(), inv.getProjectName(),
                    inv.getHours(), inv.calculateTotal(), Badge.of(inv.getStatus())});
            refreshOverview();
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
