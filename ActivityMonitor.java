package app;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Background thread that checks the database every 15 seconds and reports:
 * deadline status and alerts (freelancers), invoices marked as paid (freelancers),
 * new invoices (clients), and the number of unread messages (everyone).
 * Callbacks run on this thread, so the GUI must hop to the Swing thread itself.
 */
public class ActivityMonitor extends Thread {

    private static final long INTERVAL_MS = 15_000;

    private final User user;
    private final Consumer<String> onStatus;
    private final Consumer<String> onAlert;
    private final IntConsumer onUnread;

    private final ProjectDao projectDao = new ProjectDao();
    private final InvoiceDao invoiceDao = new InvoiceDao();
    private final MessageDao messageDao = new MessageDao();

    private final Set<Integer> alertedProjects = new HashSet<>();
    private final Set<Integer> knownInvoices = new HashSet<>();
    private boolean firstRun = true;
    private volatile boolean running = true;

    public ActivityMonitor(User user, Consumer<String> onStatus, Consumer<String> onAlert, IntConsumer onUnread) {
        this.user = user;
        this.onStatus = onStatus;
        this.onAlert = onAlert;
        this.onUnread = onUnread;
        setDaemon(true);
        setName("activity-monitor");
    }

    @Override
    public void run() {
        while (running) {
            poll();
            try {
                Thread.sleep(INTERVAL_MS);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void poll() {
        try {
            if ("FREELANCER".equals(user.getRole())) {
                pollFreelancer();
            } else {
                pollClient();
            }
            onUnread.accept(countUnread());
        } catch (DatabaseException e) {
            onStatus.accept("Deadline check failed: " + e.getMessage());
        }
        firstRun = false;
    }

    private int countUnread() throws DatabaseException {
        int unread = 0;
        for (Message message : messageDao.findReceivedBy(user.getId())) {
            if (message.getReadAt() == null) {
                unread++;
            }
        }
        return unread;
    }

    private void pollFreelancer() throws DatabaseException {
        List<Project> projects = projectDao.findByFreelancer(user.getId());
        LocalDate today = LocalDate.now();
        long needsAttention = 0;
        for (Project project : projects) {
            if (project.getStatus() == Project.Status.COMPLETED) {
                continue;
            }
            long days = ChronoUnit.DAYS.between(today, project.getDeadline());
            if (days <= 3) {
                needsAttention++;
            }
            // one alert per project when it comes within 48 hours (or is already late)
            if (days <= 2 && alertedProjects.add(project.getId())) {
                if (days < 0) {
                    onAlert.accept("Overdue: \"" + project.getName() + "\" was due " + (-days) + " day(s) ago");
                } else if (days == 0) {
                    onAlert.accept("Deadline today: \"" + project.getName() + "\"");
                } else {
                    onAlert.accept("Deadline in " + days + " day(s): \"" + project.getName() + "\"");
                }
            }
        }
        if (needsAttention == 0) {
            onStatus.accept("No projects due within the next 3 days.");
        } else {
            onStatus.accept("Warning: " + needsAttention + " project(s) overdue or due within 3 days!");
        }

        for (Invoice invoice : invoiceDao.findByFreelancer(user.getId())) {
            if (invoice.getStatus() == Invoice.Status.PAID && knownInvoices.add(invoice.getId()) && !firstRun) {
                onAlert.accept(invoice.getClientName() + " marked invoice #" + invoice.getId() + " as paid");
            }
        }
    }

    private void pollClient() throws DatabaseException {
        for (Invoice invoice : invoiceDao.findByClient(user.getId())) {
            if (knownInvoices.add(invoice.getId()) && !firstRun) {
                onAlert.accept("New invoice #" + invoice.getId() + " for \"" + invoice.getProjectName() + "\"");
            }
        }
    }

    public void shutdown() {
        running = false;
        interrupt();
    }
}
