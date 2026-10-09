package app;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/**
 * Background thread that re-checks the database every 30 seconds for projects
 * that are overdue or due within 3 days, and reports the result to the GUI.
 */
public class DeadlineMonitor extends Thread {

    private static final long INTERVAL_MS = 30_000;

    private final int freelancerId;
    private final Consumer<String> onUpdate;
    private final ProjectDao projectDao = new ProjectDao();
    private volatile boolean running = true;

    public DeadlineMonitor(int freelancerId, Consumer<String> onUpdate) {
        this.freelancerId = freelancerId;
        this.onUpdate = onUpdate;
        setDaemon(true);
        setName("deadline-monitor");
    }

    @Override
    public void run() {
        while (running) {
            check();
            try {
                Thread.sleep(INTERVAL_MS);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void check() {
        try {
            List<Project> projects = projectDao.findByFreelancer(freelancerId);
            LocalDate limit = LocalDate.now().plusDays(3);
            long due = projects.stream()
                    .filter(p -> p.getStatus() != Project.Status.COMPLETED)
                    .filter(p -> !p.getDeadline().isAfter(limit))
                    .count();
            if (due == 0) {
                onUpdate.accept("No projects due within the next 3 days.");
            } else {
                onUpdate.accept("Warning: " + due + " project(s) overdue or due within 3 days!");
            }
        } catch (DatabaseException e) {
            onUpdate.accept("Deadline check failed: " + e.getMessage());
        }
    }

    public void shutdown() {
        running = false;
        interrupt();
    }
}
