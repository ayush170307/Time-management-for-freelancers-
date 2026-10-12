package app;

import javax.swing.JButton;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A button that paints itself with smooth rounded corners and a hover colour that fades in and out.
 * Sidebar buttons can also show a small red counter badge (for unread messages).
 */
public class RoundedButton extends JButton {

    public enum Style {
        PRIMARY, SECONDARY, DANGER, SIDEBAR, SEGMENT, LINK
    }

    private final Style style;
    private float hoverProgress;
    private boolean active;
    private int badge;
    private Animator hoverAnimation;

    public RoundedButton(String text, Style style) {
        super(text);
        this.style = style;
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(Theme.BOLD);
        setHorizontalAlignment(style == Style.SIDEBAR ? SwingConstants.LEFT : SwingConstants.CENTER);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                animateHover(1f);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                animateHover(0f);
            }
        });
    }

    private void animateHover(float target) {
        if (hoverAnimation != null) {
            hoverAnimation.stop();
        }
        final float from = hoverProgress;
        hoverAnimation = Animator.start(150, p -> {
            hoverProgress = from + (target - from) * (float) p;
            repaint();
        });
    }

    /** Highlights the button (current sidebar page or selected toggle). */
    public void setActive(boolean active) {
        this.active = active;
        repaint();
    }

    /** Shows a red counter on the right side of a sidebar button; 0 hides it. */
    public void setBadge(int count) {
        this.badge = count;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        FontMetrics fm = getFontMetrics(getFont());
        int width = fm.stringWidth(getText()) + 36;
        int height = fm.getHeight() + 18;
        if (style == Style.SIDEBAR) {
            height = fm.getHeight() + 22;
        }
        return new Dimension(width, height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Color bg = backgroundColor();
        if (bg != null) {
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
        }

        if (style == Style.SIDEBAR && active) {
            g2.setColor(new Color(125, 211, 252));
            g2.fillRoundRect(0, 9, 4, getHeight() - 18, 4, 4);
        }

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(getText());
        int x = style == Style.SIDEBAR ? 16 : (getWidth() - textWidth) / 2;
        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.setColor(foregroundColor());
        g2.drawString(getText(), x, y);

        if (badge > 0 && style == Style.SIDEBAR) {
            String count = badge > 9 ? "9+" : String.valueOf(badge);
            g2.setFont(Theme.SMALL_BOLD);
            FontMetrics bm = g2.getFontMetrics();
            int bw = Math.max(22, bm.stringWidth(count) + 14);
            int bh = 22;
            int bx = getWidth() - bw - 12;
            int by = (getHeight() - bh) / 2;
            g2.setColor(new Color(239, 68, 68));
            g2.fillRoundRect(bx, by, bw, bh, bh, bh);
            g2.setColor(Color.WHITE);
            g2.drawString(count, bx + (bw - bm.stringWidth(count)) / 2, by + (bh - bm.getHeight()) / 2 + bm.getAscent());
        }
        g2.dispose();
    }

    private Color backgroundColor() {
        if (!isEnabled()) {
            return Theme.TRACK;
        }
        Color base;
        Color hover;
        switch (style) {
            case PRIMARY:
                base = Theme.PRIMARY;
                hover = Theme.PRIMARY_DARK;
                break;
            case DANGER:
                base = Theme.DANGER;
                hover = new Color(185, 28, 28);
                break;
            case SECONDARY:
                base = Theme.SECONDARY_BG;
                hover = Theme.SECONDARY_HOVER;
                break;
            case SIDEBAR:
                base = active ? Theme.SIDEBAR_ACTIVE : null;
                hover = active ? Theme.SIDEBAR_ACTIVE : Theme.SIDEBAR_HOVER;
                break;
            case SEGMENT:
                base = active ? Theme.PRIMARY : Theme.SECONDARY_BG;
                hover = active ? Theme.PRIMARY : Theme.SECONDARY_HOVER;
                break;
            case LINK:
                base = null;
                hover = Theme.HIGHLIGHT;
                break;
            default:
                base = null;
                hover = null;
                break;
        }
        return Theme.blend(base, hover, hoverProgress);
    }

    private Color foregroundColor() {
        if (!isEnabled()) {
            return Theme.MUTED;
        }
        switch (style) {
            case PRIMARY:
            case DANGER:
                return Color.WHITE;
            case SIDEBAR:
                return active ? Color.WHITE : new Color(186, 230, 253);
            case SEGMENT:
                return active ? Color.WHITE : Theme.TEXT;
            case LINK:
                return Theme.PRIMARY;
            default:
                return Theme.TEXT;
        }
    }
}
