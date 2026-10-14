package app;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/** Slim modern scroll bar: no arrow buttons, a rounded thumb and an invisible track. */
public class ThinScrollBarUI extends BasicScrollBarUI {

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return invisibleButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return invisibleButton();
    }

    private JButton invisibleButton() {
        JButton button = new JButton();
        Dimension zero = new Dimension(0, 0);
        button.setPreferredSize(zero);
        button.setMinimumSize(zero);
        button.setMaximumSize(zero);
        return button;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        // nothing: the track stays invisible
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumb) {
        if (thumb.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(isThumbRollover() ? Theme.MUTED : Theme.THUMB);
        g2.fillRoundRect(thumb.x + 2, thumb.y + 2, thumb.width - 4, thumb.height - 4, 10, 10);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return new Dimension(12, 12);
    }
}
