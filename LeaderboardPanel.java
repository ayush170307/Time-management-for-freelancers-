package app;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws the leaderboard: a podium for the top three and a ranked list with progress bars.
 * Everything grows into place with an animation whenever new data is set.
 */
public class LeaderboardPanel extends JPanel implements Scrollable {

    private static final int PODIUM_H = 220;
    private static final int ROW_H = 56;

    private static final Color GOLD = new Color(245, 158, 11);
    private static final Color SILVER = new Color(148, 163, 184);
    private static final Color BRONZE = new Color(205, 127, 50);

    private List<LeaderboardEntry> entries = new ArrayList<>();
    private int currentUserId = -1;
    private double progress = 1.0;
    private Animator animator;

    public LeaderboardPanel() {
        setOpaque(false);
    }

    public void setData(List<LeaderboardEntry> data, int currentUserId) {
        this.entries = new ArrayList<>(data);
        this.currentUserId = currentUserId;
        revalidate();
        if (animator != null) {
            animator.stop();
        }
        animator = Animator.start(900, p -> {
            progress = p;
            repaint();
        });
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, PODIUM_H + 16 + entries.size() * ROW_H + 8);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        if (entries.isEmpty()) {
            g2.setFont(Theme.BASE);
            g2.setColor(Theme.MUTED);
            String text = "No freelancers have registered yet.";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (w - fm.stringWidth(text)) / 2, 80);
        } else {
            paintPodium(g2, w);
            paintRows(g2, w);
        }
        g2.dispose();
    }

    // ------------------------------------------------------------------ podium

    private void paintPodium(Graphics2D g2, int w) {
        int blockW = Math.max(80, Math.min(140, w / 4));
        int gap = 14;
        int[] order = {1, 0, 2};          // left = 2nd, middle = 1st, right = 3rd
        int[] heights = {104, 76, 54};    // block height for 1st, 2nd, 3rd
        int baseY = PODIUM_H - 8;

        for (int k = 0; k < order.length; k++) {
            int idx = order[k];
            if (idx >= entries.size()) {
                continue;
            }
            LeaderboardEntry e = entries.get(idx);
            int bx = w / 2 - blockW / 2 + (k - 1) * (blockW + gap);
            int bh = (int) Math.round(heights[idx] * progress);
            int by = baseY - bh;
            Color medal = medalColor(idx);

            // block with rounded top corners and a flat bottom
            g2.setColor(medal);
            g2.fillRoundRect(bx, by, blockW, bh, 18, 18);
            int flat = Math.min(bh, 18);
            g2.fillRect(bx, by + bh - flat, blockW, flat);

            // big rank number inside the block
            if (bh > 30) {
                g2.setFont(Theme.HEADING);
                g2.setColor(new Color(255, 255, 255, 235));
                String rank = String.valueOf(e.getRank());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(rank, bx + (blockW - fm.stringWidth(rank)) / 2,
                        by + (bh - fm.getHeight()) / 2 + fm.getAscent());
            }

            // hours above the block
            g2.setFont(Theme.BOLD);
            g2.setColor(Theme.TEXT);
            String hours = Ui.fmt(e.getHours()) + " h";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(hours, bx + (blockW - fm.stringWidth(hours)) / 2, by - 10);

            // name above the hours
            g2.setFont(Theme.BASE);
            g2.setColor(Theme.MUTED);
            String name = fit(g2, e.getName(), blockW + gap - 4);
            fm = g2.getFontMetrics();
            g2.drawString(name, bx + (blockW - fm.stringWidth(name)) / 2, by - 32);

            // avatar circle with the first letter
            int d = 46;
            int ax = bx + (blockW - d) / 2;
            int ay = by - 32 - 20 - d;
            g2.setColor(medal);
            g2.fillOval(ax, ay, d, d);
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(Color.WHITE);
            g2.drawOval(ax + 1, ay + 1, d - 2, d - 2);
            g2.setStroke(new BasicStroke(1f));
            g2.setFont(Theme.HEADING.deriveFont(18f));
            String initial = e.getName().isEmpty() ? "?" : e.getName().substring(0, 1).toUpperCase();
            fm = g2.getFontMetrics();
            g2.drawString(initial, ax + (d - fm.stringWidth(initial)) / 2,
                    ay + (d - fm.getHeight()) / 2 + fm.getAscent());
        }
    }

    // ------------------------------------------------------------------ list

    private void paintRows(Graphics2D g2, int w) {
        double max = 0;
        for (LeaderboardEntry e : entries) {
            max = Math.max(max, e.getHours());
        }
        if (max <= 0) {
            max = 1;
        }
        int top = PODIUM_H + 16;

        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntry e = entries.get(i);
            int y = top + i * ROW_H;
            boolean me = e.getId() == currentUserId;

            if (me) {
                g2.setColor(Theme.HIGHLIGHT);
                g2.fillRoundRect(4, y + 2, w - 8, ROW_H - 4, 16, 16);
            } else {
                g2.setColor(Theme.BORDER);
                g2.drawLine(12, y + ROW_H - 1, w - 12, y + ROW_H - 1);
            }

            // rank badge
            int bd = 30;
            int bx = 16;
            int by = y + (ROW_H - bd) / 2;
            g2.setColor(i < 3 ? medalColor(i) : Theme.TRACK);
            g2.fillOval(bx, by, bd, bd);
            g2.setFont(Theme.SMALL_BOLD);
            g2.setColor(i < 3 ? Color.WHITE : Theme.MUTED);
            String rank = String.valueOf(e.getRank());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(rank, bx + (bd - fm.stringWidth(rank)) / 2, by + (bd - fm.getHeight()) / 2 + fm.getAscent());

            // name and hours
            g2.setFont(Theme.BOLD);
            g2.setColor(Theme.TEXT);
            String name = fit(g2, e.getName() + (me ? "  (You)" : ""), w - 250);
            g2.drawString(name, 62, y + 24);
            String hours = Ui.fmt(e.getHours()) + " h";
            fm = g2.getFontMetrics();
            g2.drawString(hours, w - 16 - fm.stringWidth(hours), y + 24);

            // progress bar
            int barX = 62;
            int barW = Math.max(20, w - 62 - 16);
            int barY = y + 34;
            int barH = 8;
            g2.setColor(Theme.TRACK);
            g2.fillRoundRect(barX, barY, barW, barH, 8, 8);
            int fill = (int) Math.round(barW * (e.getHours() / max) * progress);
            if (fill > 0) {
                g2.setColor(i < 3 ? medalColor(i) : Theme.PRIMARY);
                g2.fillRoundRect(barX, barY, Math.max(fill, 8), barH, 8, 8);
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private static Color medalColor(int index) {
        if (index == 0) {
            return GOLD;
        }
        return index == 1 ? SILVER : BRONZE;
    }

    /** Shortens text with an ellipsis until it fits the given width. */
    private static String fit(Graphics2D g2, String text, int maxWidth) {
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String cut = text;
        while (cut.length() > 1 && fm.stringWidth(cut + "\u2026") > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "\u2026";
    }

    // ------------------------------------------------------------------ Scrollable

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(400, 360);
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 24;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 120;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height;
    }
}
