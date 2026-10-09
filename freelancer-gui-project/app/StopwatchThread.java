package app;

import java.util.function.Consumer;

/**
 * Background thread that counts elapsed seconds for a live billable-time timer.
 * The elapsed counter is shared between this thread and the GUI thread, so access is synchronized.
 */
public class StopwatchThread extends Thread {

    private final Consumer<Long> onTick;
    private volatile boolean running = true;
    private long elapsedSeconds = 0;

    public StopwatchThread(Consumer<Long> onTick) {
        this.onTick = onTick;
        setDaemon(true);
        setName("stopwatch-thread");
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return;
            }
            if (!running) {
                break;
            }
            onTick.accept(incrementAndGet());
        }
    }

    private synchronized long incrementAndGet() {
        elapsedSeconds++;
        return elapsedSeconds;
    }

    public synchronized long getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void stopTimer() {
        running = false;
        interrupt();
    }
}
