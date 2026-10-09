package app;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ClientDashboardPanel extends JPanel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final MainFrame frame;
    private final Client client;

    private final ProjectDao projectDao = new ProjectDao();
    private final InvoiceDao invoiceDao = new InvoiceDao();
    private final MessageDao messageDao = new MessageDao();

    private List<Invoice> invoices = new ArrayList<>();

    private final DefaultTableModel invoicesModel =
            Ui.readOnlyModel("Invoice #", "Type", "Project", "Hours", "Total", "Status");
    private final DefaultTableModel projectsModel =
            Ui.readOnlyModel("Project", "Description", "Freelancer", "Deadline", "Status");
    private final DefaultTableModel messagesModel = Ui.readOnlyModel("From", "Message", "Received");

    private final JTable invoicesTable = new JTable(invoicesModel);
    private final JLabel summaryLabel = new JLabel(" ");

    public ClientDashboardPanel(MainFrame frame, Client client) {
        this.frame = frame;
        this.client = client;
        setLayout(new BorderLayout());

        JLabel header = new JLabel(client.getDashboardTitle(), SwingConstants.CENTER);
        header.setFont(new Font("SansSerif", Font.BOLD, 18));
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("My Invoices", buildInvoicesTab());
        tabs.addTab("Project Status", buildProjectsTab());
        tabs.addTab("Messages", buildMessagesTab());
        add(tabs, BorderLayout.CENTER);

        JButton logoutBtn = new JButton("Log Out");
        logoutBtn.addActionListener(e -> this.frame.goHome());
        JPanel bottom = new JPanel();
        bottom.add(logoutBtn);
        add(bottom, BorderLayout.SOUTH);

        refreshAll();
    }

    // ------------------------------------------------------------------ Invoices

    private JPanel buildInvoicesTab() {
        JPanel panel = new JPanel(new BorderLayout());

        JTextField numberField = new JTextField(6);
        JButton viewBtn = new JButton("View Invoice");
        JButton paidBtn = new JButton("Mark Selected as Paid");
        JButton refreshBtn = new JButton("Refresh");

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.add(new JLabel("Invoice number:"));
        top.add(numberField);
        top.add(viewBtn);
        top.add(paidBtn);
        top.add(refreshBtn);
        panel.add(top, BorderLayout.NORTH);

        invoicesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(invoicesTable), BorderLayout.CENTER);

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
            int row = invoicesTable.getSelectedRow();
            if (row < 0) {
                Ui.error(this, "Select an invoice in the table first.");
                return;
            }
            Invoice selected = invoices.get(row);
            if (selected.getStatus() == Invoice.Status.PAID) {
                Ui.info(this, "Invoice #" + selected.getId() + " is already paid.");
                return;
            }
            try {
                invoiceDao.markPaid(selected.getId());
                refreshAll();
            } catch (DatabaseException ex) {
                Ui.error(this, ex.getMessage());
            }
        });

        refreshBtn.addActionListener(e -> refreshAll());
        return panel;
    }

    // ------------------------------------------------------------------ Projects

    private JPanel buildProjectsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshAll());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.add(summaryLabel);
        top.add(refreshBtn);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(new JTable(projectsModel)), BorderLayout.CENTER);
        return panel;
    }

    // ------------------------------------------------------------------ Messages

    private JPanel buildMessagesTab() {
        JPanel panel = new JPanel(new BorderLayout());
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshAll());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.add(refreshBtn);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(new JTable(messagesModel)), BorderLayout.CENTER);
        return panel;
    }

    // ------------------------------------------------------------------ Refresh

    private void refreshAll() {
        try {
            invoices = invoiceDao.findByClient(client.getId());
            Ui.fill(invoicesModel, invoices, inv -> new Object[]{
                    inv.getId(), inv.getType(), inv.getProjectName(),
                    Ui.fmt(inv.getHours()), Ui.fmt(inv.calculateTotal()), inv.getStatus()});

            List<Project> projects = projectDao.findByClient(client.getId());
            Ui.fill(projectsModel, projects, proj -> new Object[]{
                    proj.getName(), proj.getDescription(), proj.getFreelancerName(),
                    proj.getDeadline(), proj.getStatus()});

            Map<Project.Status, Integer> counts = new EnumMap<>(Project.Status.class);
            for (Project.Status s : Project.Status.values()) {
                counts.put(s, 0);
            }
            for (Project proj : projects) {
                counts.merge(proj.getStatus(), 1, Integer::sum);
            }
            summaryLabel.setText("Total: " + projects.size()
                    + "   |   Not started: " + counts.get(Project.Status.NOT_STARTED)
                    + "   |   In progress: " + counts.get(Project.Status.IN_PROGRESS)
                    + "   |   Completed: " + counts.get(Project.Status.COMPLETED));

            List<Message> received = messageDao.findReceivedBy(client.getId());
            Ui.fill(messagesModel, received, m -> new Object[]{
                    m.getSenderName(), m.getContent(), m.getSentAt().format(FMT)});
        } catch (DatabaseException ex) {
            Ui.error(this, ex.getMessage());
        }
    }
}
