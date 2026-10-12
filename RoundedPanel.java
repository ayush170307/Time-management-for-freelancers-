package app;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

/**
 * A card with smooth rounded corners and a real blurred drop shadow.
 * The shadow is drawn into an image, blurred with a convolution filter, and cached until the size changes.
 */
public class RoundedPanel extends JPanel {

    /** Space reserved around the card for the blurred shadow. */
    public static final int SHADOW = 10;
    private static final int RADIUS = 22;
    private static final int BLUR = 7;

    private BufferedImage shadow;
    private int shadowW = -1;
    private int shadowH = -1;
    private Color gradientStart;
    private Color gradientEnd;

    public RoundedPanel() {
        setOpaque(false);
        setBackground(Theme.CARD);
        setPadding(16, 18, 18, 18);
    }

    /** Sets the inner padding; the shadow space is added automatically. */
    public void setPadding(int top, int left, int bottom, int right) {
        setBorder(BorderFactory.createEmptyBorder(
                top + SHADOW, left + SHADOW, bottom + SHADOW, right + SHADOW));
    }

    /** Fills the card with a left-to-right gradient instead of a flat colour. */
    public void setGradient(Color start, Color end) {
        this.gradientStart = start;
        this.gradientEnd = end;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        if (w <= SHADOW * 2 || h <= SHADOW * 2) {
            return;
        }
        if (shadow == null || shadowW != w || shadowH != h) {
            shadow = createShadow(w, h);
            shadowW = w;
            shadowH = h;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.drawImage(shadow, 0, 0, null);

        int x = SHADOW;
        int y = SHADOW;
        int cw = w - SHADOW * 2;
        int ch = h - SHADOW * 2;
        if (gradientStart != null) {
            g2.setPaint(new GradientPaint(x, y, gradientStart, x + cw, y, gradientEnd));
            g2.fillRoundRect(x, y, cw, ch, RADIUS, RADIUS);
        } else {
            g2.setColor(getBackground());
            g2.fillRoundRect(x, y, cw, ch, RADIUS, RADIUS);
            g2.setColor(new Color(Theme.BORDER.getRed(), Theme.BORDER.getGreen(), Theme.BORDER.getBlue(), 190));
            g2.drawRoundRect(x, y, cw - 1, ch - 1, RADIUS, RADIUS);
        }
        g2.dispose();
    }

    private static BufferedImage createShadow(int w, int h) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillRoundRect(SHADOW, SHADOW + 3, w - SHADOW * 2, h - SHADOW * 2, RADIUS, RADIUS);
        g2.dispose();

        float[] weights = new float[BLUR];
        for (int i = 0; i < BLUR; i++) {
            weights[i] = 1f / BLUR;
        }
        ConvolveOp horizontal = new ConvolveOp(new Kernel(BLUR, 1, weights), ConvolveOp.EDGE_NO_OP, null);
        ConvolveOp vertical = new ConvolveOp(new Kernel(1, BLUR, weights), ConvolveOp.EDGE_NO_OP, null);
        for (int pass = 0; pass < 2; pass++) {
            image = horizontal.filter(image, null);
            image = vertical.filter(image, null);
        }
        return image;
    }
}
