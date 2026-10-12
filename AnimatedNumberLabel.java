package app;

import javax.swing.JLabel;

/** A label that counts up (or down) to a new number instead of jumping straight to it. */
public class AnimatedNumberLabel extends JLabel {

    private double shown;
    private int decimals;
    private Animator animator;

    public AnimatedNumberLabel(String initialText) {
        super(initialText);
    }

    public void animateTo(double target, int decimalPlaces) {
        this.decimals = decimalPlaces;
        if (animator != null) {
            animator.stop();
        }
        final double from = shown;
        if (Math.abs(target - from) < 0.0001) {
            display(target);
            return;
        }
        animator = Animator.start(700, p -> display(from + (target - from) * p));
    }

    private void display(double value) {
        shown = value;
        setText(String.format("%." + decimals + "f", value));
    }
}
