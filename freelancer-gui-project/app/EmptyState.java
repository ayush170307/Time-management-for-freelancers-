package app;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Friendly "nothing here yet" screen with a hand-drawn vector icon, a title and a hint. */
public class EmptyState extends JPanel {

    public enum Kind {
        CLOCK, DOCUMENT, FOLDER, CHAT, CHART, USERS
    }

    private final Kind kind;
    private final String title;
    private final String hint;

    public EmptyState(Kind kind, String title, String hint) {
        this.kind = kind;
        this.title = title;
        this.hint = hint;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int cx = getWidth() / 2;
        int cy = Math.max(70, getHeight() / 2 - 34);

        Color p = Theme.PRIMARY;
        g2.setColor(new Color(p.getRed(), p.getGreen(), p.getBlue(), Theme.isDark() ? 40 : 24));
        g2.fillOval(cx - 54, cy - 54, 108, 108);
        paintIcon(g2, kind, cx, cy, 54);

        g2.setFont(Theme.BOLD.deriveFont(16f));
        g2.setColor(Theme.TEXT);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, cx - fm.stringWidth(title) / 2, cy + 90);

        g2.setFont(Theme.BASE);
        g2.setColor(Theme.MUTED);
        fm = g2.getFontMetrics();
        g2.drawString(hint, cx - fm.stringWidth(hint) / 2, cy + 114);
        g2.dispose();
    }

    /** Draws one of the line icons centred on (cx, cy). Also used by the charts. */
    public static void paintIcon(Graphics2D g2, Kind kind, int cx, int cy, int size) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Theme.PRIMARY);
        g.setStroke(new BasicStroke(Math.max(2f, size / 16f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int r = size / 2;

        switch (kind) {
            case CLOCK: {
                g.drawOval(cx - r, cy - r, size, size);
                g.drawLine(cx, cy, cx, cy - (int) (r * 0.55));
                g.drawLine(cx, cy, cx + (int) (r * 0.4), cy + (int) (r * 0.2));
                break;
            }
            case DOCUMENT: {
                int w = (int) (size * 0.68);
                int x = cx - w / 2;
                int y = cy - r;
                g.drawRoundRect(x, y, w, size, 8, 8);
                for (int i = 0; i < 3; i++) {
                    int ly = y + size / 4 + i * (size / 5);
                    g.drawLine(x + w / 5, ly, x + w - w / 5, ly);
                }
                break;
            }
            case FOLDER: {
                int h = (int) (size * 0.72);
                int x = cx - r;
                int y = cy - h / 2 + 5;
                g.drawRoundRect(x, y, size, h, 8, 8);
                g.drawRoundRect(x, y - 9, size / 3, 12, 6, 6);
                break;
            }
            case CHAT: {
                int h = (int) (size * 0.68);
                int x = cx - r;
                int y = cy - h / 2 - 4;
                g.drawRoundRect(x, y, size, h, 16, 16);
                g.drawLine(x + size / 4, y + h, x + size / 6, y + h + 10);
                g.drawLine(x + size / 6, y + h + 10, x + size / 2 - 2, y + h);
                int dot = Math.max(3, size / 14);
                for (int i = 0; i < 3; i++) {
                    g.fillOval(cx - size / 4 + i * (size / 4) - dot / 2, y + h / 2 - dot / 2, dot, dot);
                }
                break;
            }
            case CHART: {
                int base = cy + r - 2;
                int bw = Math.max(6, size / 5);
                double[] heights = {0.45, 0.8, 0.6};
                for (int i = 0; i < 3; i++) {
                    int bh = (int) (size * heights[i]);
                    int bx = cx - size / 2 + 3 + i * (bw + size / 8);
                    g.drawRoundRect(bx, base - bh, bw, bh, 4, 4);
                }
                g.drawLine(cx - r, base + 2, cx + r, base + 2);
                break;
            }
            case USERS: {
                int head = (int) (size * 0.34);
                g.drawOval(cx - head / 2, cy - r + 2, head, head);
                g.drawArc(cx - r + 4, cy + 2, size - 8, (int) (size * 0.7), 0, 180);
                break;
            }
            default:
                break;
        }
        g.dispose();
    }
}
