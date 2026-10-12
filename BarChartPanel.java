package app;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.Map;

/** Simple hand-drawn bar chart whose bars grow upwards whenever new data arrives. */
public class BarChartPanel extends JPanel {

    private Map<String, Double> data = new LinkedHashMap<>();
    private final Color barColor;
    private final String valueFormat;
    private final String emptyText;
    private double progress = 1.0;
    private Animator animator;

    public BarChartPanel(Color barColor, String valueFormat, String emptyText) {
        this.barColor = barColor;
        this.valueFormat = valueFormat;
        this.emptyText = emptyText;
        setOpaque(false);
        setPreferredSize(new Dimension(300, 200));
    }

    public void setData(Map<String, Double> newData) {
        this.data = new LinkedHashMap<>(newData);
        if (animator != null) {
            animator.stop();
        }
        animator = Animator.start(750, p -> {
            progress = p;
            repaint();
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (data.isEmpty()) {
            EmptyState.paintIcon(g2, EmptyState.Kind.CHART, w / 2, h / 2 - 22, 44);
            g2.setFont(Theme.BASE);
            g2.setColor(Theme.MUTED);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(emptyText, (w - fm.stringWidth(emptyText)) / 2, h / 2 + 36);
            g2.dispose();
            return;
        }

        int left = 8;
        int right = 8;
        int top = 26;
        int bottom = 30;
        int chartH = h - top - bottom;
        int chartW = w - left - right;

        double max = 0;
        for (double v : data.values()) {
            max = Math.max(max, v);
        }
        if (max <= 0) {
            max = 1;
        }

        int count = data.size();
        int slot = chartW / count;
        int barW = Math.max(14, Math.min(56, (int) (slot * 0.55)));

        g2.setColor(Theme.BORDER);
        g2.drawLine(left, top + chartH, w - right, top + chartH);

        int i = 0;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            int barH = (int) Math.round(chartH * (entry.getValue() / max) * progress);
            int x = left + i * slot + (slot - barW) / 2;
            int y = top + chartH - barH;

            g2.setColor(barColor);
            g2.fillRoundRect(x, y, barW, Math.max(barH, 3), 10, 10);

            g2.setFont(Theme.SMALL_BOLD);
            g2.setColor(Theme.TEXT);
            String value = String.format(valueFormat, entry.getValue());
            int vw = g2.getFontMetrics().stringWidth(value);
            g2.drawString(value, x + (barW - vw) / 2, y - 6);

            g2.setFont(Theme.BASE.deriveFont(12f));
            g2.setColor(Theme.MUTED);
            String label = shorten(entry.getKey(), slot / 7);
            int lw = g2.getFontMetrics().stringWidth(label);
            g2.drawString(label, left + i * slot + (slot - lw) / 2, top + chartH + 20);
            i++;
        }
        g2.dispose();
    }

    private static String shorten(String text, int maxChars) {
        int limit = Math.max(6, maxChars);
        if (text == null) {
            return "";
        }
        return text.length() <= limit ? text : text.substring(0, limit - 1) + "\u2026";
    }
}
