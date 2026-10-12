package app;

import javax.swing.border.AbstractBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/** A smooth, anti-aliased rounded outline with built-in padding (used for text fields). */
public class RoundedBorder extends AbstractBorder {

    private final Color color;
    private final int radius;
    private final int top;
    private final int left;
    private final int bottom;
    private final int right;

    public RoundedBorder(Color color, int radius, int top, int left, int bottom, int right) {
        this.color = color;
        this.radius = radius;
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setColor(color);
        g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(top, left, bottom, right);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(top, left, bottom, right);
        return insets;
    }
}
