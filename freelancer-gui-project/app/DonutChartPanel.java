package app;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Donut chart (paid vs pending) that sweeps around smoothly whenever new data arrives. */
public class DonutChartPanel extends JPanel {

    private double paid;
    private double pending;
    private final String emptyText;
    private double progress = 1.0;
    private Animator animator;

    public DonutChartPanel(String emptyText) {
        this.emptyText = emptyText;
        setOpaque(false);
        setPreferredSize(new Dimension(300, 200));
    }

    public void setData(double paid, double pending) {
        this.paid = paid;
        this.pending = pending;
        if (animator != null) {
            animator.stop();
        }
        animator = Animator.start(850, p -> {
            progress = p;
            repaint();
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int thick = 22;
        int size = Math.max(80, Math.min(h - 16, w / 2));
        int x = 16;
        int y = (h - size) / 2;
        int ax = x + thick / 2;
        int ay = y + thick / 2;
        int asize = size - thick;

        double total = paid + pending;
        g2.setStroke(new BasicStroke((float) thick, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));

        if (total <= 0) {
            g2.setColor(Theme.TRACK);
            g2.drawArc(ax, ay, asize, asize, 0, 360);
            g2.setStroke(new BasicStroke(1f));
            EmptyState.paintIcon(g2, EmptyState.Kind.DOCUMENT, x + size / 2, y + size / 2, size / 3);
            g2.setFont(Theme.BASE);
            g2.setColor(Theme.MUTED);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(emptyText, x + size + 24, h / 2 + fm.getAscent() / 2);
            g2.dispose();
            return;
        }

        int paidAngle = (int) Math.round(360 * (paid / total) * progress);
        int pendingAngle = (int) Math.round(360 * (pending / total) * progress);
        g2.setColor(Theme.TRACK);
        g2.drawArc(ax, ay, asize, asize, 0, 360);
        if (paidAngle > 0) {
            g2.setColor(Theme.SUCCESS);
            g2.drawArc(ax, ay, asize, asize, 90, -paidAngle);
        }
        if (pendingAngle > 0) {
            g2.setColor(Theme.WARNING);
            g2.drawArc(ax, ay, asize, asize, 90 - paidAngle, -pendingAngle);
        }
        g2.setStroke(new BasicStroke(1f));

        // percentage in the middle
        String pct = Math.round(100 * paid / total * progress) + "%";
        g2.setFont(Theme.HEADING);
        g2.setColor(Theme.TEXT);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(pct, x + (size - fm.stringWidth(pct)) / 2, y + size / 2 + fm.getAscent() / 2 - 6);
        g2.setFont(Theme.SMALL_BOLD);
        g2.setColor(Theme.MUTED);
        fm = g2.getFontMetrics();
        g2.drawString("PAID", x + (size - fm.stringWidth("PAID")) / 2, y + size / 2 + 16);

        // legend
        int lx = x + size + 28;
        int ly = h / 2 - 14;
        drawLegend(g2, lx, ly, Theme.SUCCESS, "Paid", paid);
        drawLegend(g2, lx, ly + 34, Theme.WARNING, "Pending", pending);
        g2.dispose();
    }

    private void drawLegend(Graphics2D g2, int x, int y, Color color, String label, double amount) {
        g2.setColor(color);
        g2.fillOval(x, y - 10, 12, 12);
        g2.setFont(Theme.SMALL_BOLD);
        g2.setColor(Theme.MUTED);
        g2.drawString(label.toUpperCase(), x + 20, y);
        g2.setFont(Theme.BOLD);
        g2.setColor(Theme.TEXT);
        g2.drawString(Ui.fmt(amount), x + 20, y + 17);
    }
}
