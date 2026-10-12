package app;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ClientDashboardPanel extends DashboardShell implements Closable {

    private final Client client;

    private final ProjectDao projectDao = new ProjectDao();
    private final InvoiceDao invoiceDao = new InvoiceDao();

    private List<Invoice> invoices = new ArrayList<>();

    private final DefaultTableModel invoicesModel =
            Ui.readOnlyModel("Invoice #", "Type", "Project", "Hours", "Total", "Status");
    private final DefaultTableModel projectsModel =
            Ui.readOnlyModel("Project", "Description", "Freelancer", "Deadline", "Status");

    private final JTable invoicesTable = new JTable(invoicesModel);

    private final AnimatedNumberLabel dueValue = new AnimatedNumberLabel("0.00");
    private final AnimatedNumberLabel openValue = new AnimatedNumberLabel("0");
    private final AnimatedNumberLabel activeValue = new AnimatedNumberLabel("0");
    private final JLabel summaryLabel = Theme.label(" ");

    private final DonutChartPanel invoiceDonut = new DonutChartPanel("No invoices yet");
    private final BarChartPanel statusChart =
            new BarChartPanel(Theme.PRIMARY, "%.0f", "No projects yet");

    private final ActivityMonitor monitor;

    public ClientDashboardPanel(MainFrame frame, Client client) {
        super(client.getName(), "Client", "CLIENT", frame::goHome, frame::toggleTheme);
        this.client = client;

        addPage("Overview", "Overview", buildOverviewPage(), this::refreshAll);
        addPage("Invoices", "My Invoices", buildInvoicesPage(), this::refreshAll);
        addPage("Project Status", "Project Status", buildProjectsPage(), this::refreshAll);
        LeaderboardPage leaderboard = new LeaderboardPage(-1);
        addPage("Top Freelancers", "Freelancer Leaderboard", leaderboard, leaderboard::refresh);
        MessagesPage messages = new MessagesPage(client, "FREELANCER", count -> setBadge("Messages", count));
        addPage("Messages", "Messages", messages, messages::refresh);

        monitor = new ActivityMonitor(client,
                text -> { },
                alert -> SwingUtilities.invokeLater(() -> Ui.toast(this, alert)),
                count -> SwingUtilities.invokeLater(() -> setBadge("Messages", count)));
        monitor.start();
    }

    // ------------------------------------------------------------------ Overview

    private JPanel buildOverviewPage() {
        JPanel page = new JPanel(new BorderLayout(0, 2));
        page.setOpaque(false);

        JPanel stats = new JPanel(new GridLayout(1, 3, 0, 0));
        stats.setOpaque(false);
        stats.add(Theme.statCard("Amount due", dueValue, Theme.DANGER));
        stats.add(Theme.statCard("Unpaid invoices", openValue, Theme.WARNING));
        stats.add(Theme.statCard("Active projects", activeValue, Theme.PRIMARY));

        JPanel top = new JPanel(new BorderLayout(0, 2));
        top.setOpaque(false);
        top.add(Theme.banner(Theme.greeting() + ", " + Theme.firstName(client.getName()),
                Theme.todayText()), BorderLayout.NORTH);
        top.add(stats, BorderLayout.CENTER);
        page.add(top, BorderLayout.NORTH);

        JPanel charts = new JPanel(new GridLayout(1, 2, 0, 0));
        charts.setOpaque(false);
        charts.add(Theme.chartCard("INVOICES: PAID VS PENDING", invoiceDonut));
        charts.add(Theme.chartCard("PROJECTS BY STATUS", statusChart));
        page.add(charts, BorderLayout.CENTER);
        return page;
    }

    // ------------------------------------------------------------------ Invoices

    private JPanel buildInvoicesPage() {
        JPanel page = new JPanel(new BorderLayout(0, 2));
        page.setOpaque(false);

        JTextField numberField = Theme.numericField(6, 0);
        RoundedButton viewBtn = new RoundedButton("View Invoice", RoundedButton.Style.PRIMARY);

        JPanel top = Theme.card();
        top.setLayout(new FlowLayout(FlowLayout.LEFT, 14, 0));
        top.add(Theme.labeled("INVOICE NUMBER", numberField));
        top.add(Theme.labeled(" ", viewBtn));

        RoundedButton paidBtn = new RoundedButton("Mark Selected as Paid", RoundedButton.Style.SECONDARY);
        RoundedButton exportBtn = new RoundedButton("Export Invoice (PDF/TXT)", RoundedButton.Style.SECONDARY);
        RoundedButton csvBtn = new RoundedButton("Export All (CSV)", RoundedButton.Style.SECONDARY);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.setOpaque(false);
        toolbar.add(paidBtn);
        toolbar.add(exportBtn);
        toolbar.add(csvBtn);

        invoicesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(toolbar, BorderLayout.NORTH);
        center.add(Theme.tableCard(invoicesTable, EmptyState.Kind.DOCUMENT,
                "No invoices yet", "Invoices from your freelancers will show up here."), BorderLayout.CENTER);

        page.add(top, BorderLayout.NORTH);
        page.add(center, BorderLayout.CENTER);

        viewBtn.addActionListener(e -> {
            try {
                int number = Validator.parsePositiveInt(numberField.getText(), "Invoice number");
                Optional<Invoice> found = invoiceDao.findById(number);
                if (!found.isPresent() || found.get().getClientId() != client.getId()) {
                    Ui.error(this, "No invoice found with number " + number + ".");
                    return;
                }
                Ui.info(this, found.get().describe());
            } catch (InvalidInputException | DatabaseException ex) {
                Ui.error(this, ex.getMessage());
            }
        });

        paidBtn.addActionListener(e -> {
            Invoice selected = selectedInvoice();
            if (selected == null) {
                Ui.error(this, "Select an invoice in the table first.");
                return;
            }
            Session.requireUser(selected.getClientId());
            if (selected.getStatus() == Invoice.Status.PAID) {
                Ui.toast(this, "Invoice #" + selected.getId() + " is already paid");
                return;
            }
            try {
                invoiceDao.markPaid(selected.getId());
                Ui.toast(this, "Invoice #" + selected.getId() + " marked as paid");
                refreshAll();
            } catch (DatabaseException ex) {
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
        csvBtn.addActionListener(e -> ReportExporter.exportEarnings(this, invoices));

        invoicesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Invoice selected = selectedInvoice();
                    if (selected != null) {
                        Ui.info(ClientDashboardPanel.this, selected.describe());
                    }
                }
            }
        });
        return page;
    }

    private Invoice selectedInvoice() {
        int viewRow = invoicesTable.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        return invoices.get(invoicesTable.convertRowIndexToModel(viewRow));
    }

    // ------------------------------------------------------------------ Projects

    private JPanel buildProjectsPage() {
        JPanel page = new JPanel(new BorderLayout(0, 2));
        page.setOpaque(false);

        JPanel top = Theme.card();
        top.setLayout(new BorderLayout());
        top.add(summaryLabel, BorderLayout.CENTER);

        page.add(top, BorderLayout.NORTH);
        page.add(Theme.tableCard(new JTable(projectsModel), EmptyState.Kind.FOLDER,
                "No projects yet", "Projects your freelancers create for you will appear here."), BorderLayout.CENTER);
        return page;
    }

    // ------------------------------------------------------------------ Refresh

    private void refreshAll() {
        try {
            invoices = invoiceDao.findByClient(client.getId());
            Ui.fill(invoicesModel, invoices, inv -> new Object[]{
                    inv.getId(), inv.getType(), inv.getProjectName(),
                    inv.getHours(), inv.calculateTotal(), Badge.of(inv.getStatus())});

            double paid = 0;
            double due = 0;
            int unpaid = 0;
            for (Invoice inv : invoices) {
                if (inv.getStatus() == Invoice.Status.PAID) {
                    paid += inv.calculateTotal();
                } else {
                    due += inv.calculateTotal();
                    unpaid++;
                }
            }

            List<Project> projects = projectDao.findByClient(client.getId());
            Ui.fill(projectsModel, projects, proj -> new Object[]{
                    proj.getName(), proj.getDescription(), proj.getFreelancerName(),
                    proj.getDeadline(), Badge.of(proj)});

            Map<Project.Status, Integer> counts = new EnumMap<>(Project.Status.class);
            for (Project.Status s : Project.Status.values()) {
                counts.put(s, 0);
            }
            for (Project proj : projects) {
                counts.merge(proj.getStatus(), 1, Integer::sum);
            }
            summaryLabel.setText("Total: " + projects.size()
                    + "     |     Not started: " + counts.get(Project.Status.NOT_STARTED)
                    + "     |     In progress: " + counts.get(Project.Status.IN_PROGRESS)
                    + "     |     Completed: " + counts.get(Project.Status.COMPLETED));

            dueValue.animateTo(due, 2);
            openValue.animateTo(unpaid, 0);
            activeValue.animateTo(projects.size() - counts.get(Project.Status.COMPLETED), 0);

            invoiceDonut.setData(paid, due);
            Map<String, Double> statusData = new LinkedHashMap<>();
            if (!projects.isEmpty()) {
                statusData.put("Not started", (double) counts.get(Project.Status.NOT_STARTED));
                statusData.put("In progress", (double) counts.get(Project.Status.IN_PROGRESS));
                statusData.put("Completed", (double) counts.get(Project.Status.COMPLETED));
            }
            statusChart.setData(statusData);
        } catch (DatabaseException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    @Override
    public void onClose() {
        monitor.shutdown();
    }
}
