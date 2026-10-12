package app;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Transparent layer on top of the window that shows short messages that slide up and fade out.
 * It never blocks mouse clicks, so the app stays usable while a toast is visible.
 */
public class ToastGlassPane extends JComponent {

    private String message = "";
    private float alpha = 0f;
    private long shownAt;
    private Timer timer;

    public ToastGlassPane() {
        setOpaque(false);
    }

    @Override
    public boolean contains(int x, int y) {
        return false;
    }

    public void show(String text) {
        message = text;
        alpha = 0f;
        shownAt = System.currentTimeMillis();
        if (timer != null) {
            timer.stop();
        }
        timer = new Timer(16, e -> tick());
        timer.start();
    }

    private void tick() {
        long elapsed = System.currentTimeMillis() - shownAt;
        if (elapsed < 300) {
            alpha = (float) Animator.easeOut(elapsed / 300.0);
        } else if (elapsed < 2900) {
            alpha = 1f;
        } else if (elapsed < 3300) {
            alpha = 1f - (elapsed - 2900) / 400f;
        } else {
            alpha = 0f;
            timer.stop();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (alpha <= 0f || message.isEmpty()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, alpha)));

        g2.setFont(Theme.BOLD);
        FontMetrics fm = g2.getFontMetrics();
        int padX = 26;
        int padY = 14;
        int w = fm.stringWidth(message) + padX * 2;
        int h = fm.getHeight() + padY * 2;
        int slide = Math.round((1f - Math.min(1f, alpha)) * 16);
        int x = (getWidth() - w) / 2;
        int y = getHeight() - h - 36 + slide;

        // soft blurred-looking shadow made of a few translucent layers
        for (int i = 5; i >= 1; i--) {
            g2.setColor(new Color(0, 0, 0, 14));
            g2.fillRoundRect(x - i, y - i + 4, w + i * 2, h + i * 2, 26 + i, 26 + i);
        }
        g2.setColor(new Color(10, 25, 47));
        g2.fillRoundRect(x, y, w, h, 26, 26);
        g2.setColor(Color.WHITE);
        g2.drawString(message, x + padX, y + padY + fm.getAscent());
        g2.dispose();
    }
}
