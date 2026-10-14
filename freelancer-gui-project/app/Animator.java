package app;

import javax.swing.Timer;
import java.util.function.DoubleConsumer;

/**
 * Tiny animation helper. Calls the given function about 60 times a second with a value that
 * eases from 0.0 to 1.0 over the chosen duration (fast at first, slowing down at the end).
 * Runs on the Swing event thread, so it is safe to update components from it.
 */
public final class Animator {

    private final int durationMs;
    private final DoubleConsumer frame;
    private final Timer timer;
    private long startedAt;

    private Animator(int durationMs, DoubleConsumer frame) {
        this.durationMs = durationMs;
        this.frame = frame;
        this.timer = new Timer(16, e -> tick());
    }

    public static Animator start(int durationMs, DoubleConsumer frame) {
        Animator animator = new Animator(durationMs, frame);
        animator.begin();
        return animator;
    }

    private void begin() {
        startedAt = System.currentTimeMillis();
        frame.accept(0.0);
        timer.start();
    }

    private void tick() {
        double t = Math.min(1.0, (System.currentTimeMillis() - startedAt) / (double) durationMs);
        frame.accept(easeOut(t));
        if (t >= 1.0) {
            timer.stop();
        }
    }

    public void stop() {
        timer.stop();
    }

    /** Cubic ease-out: starts quickly and settles gently. */
    public static double easeOut(double t) {
        double inverse = 1.0 - t;
        return 1.0 - inverse * inverse * inverse;
    }
}
