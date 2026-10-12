package app;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;

/**
 * Puts a small icon in the system tray so the live timer can keep running while the
 * window is hidden. Double-click the icon (or use its menu) to open the app again.
 */
public class TrayManager {

    private final MainFrame frame;
    private TrayIcon trayIcon;

    public TrayManager(MainFrame frame) {
        this.frame = frame;
    }

    /** Adds the tray icon if the operating system supports it. Returns false when it cannot. */
    public boolean install() {
        if (!SystemTray.isSupported()) {
            return false;
        }
        if (trayIcon != null) {
            return true;
        }
        PopupMenu menu = new PopupMenu();
        MenuItem open = new MenuItem("Open");
        open.addActionListener(e -> frame.restoreFromTray());
        MenuItem exit = new MenuItem("Exit");
        exit.addActionListener(e -> frame.exitApplication());
        menu.add(open);
        menu.add(exit);

        TrayIcon icon = new TrayIcon(createIcon(), "Time Management for Freelancers", menu);
        icon.setImageAutoSize(true);
        icon.addActionListener(e -> frame.restoreFromTray());
        try {
            SystemTray.getSystemTray().add(icon);
        } catch (AWTException ex) {
            return false;
        }
        trayIcon = icon;
        return true;
    }

    public void remove() {
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }
    }

    public void setTooltip(String text) {
        if (trayIcon != null) {
            trayIcon.setToolTip(text);
        }
    }

    public void notify(String title, String message) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }

    private static Image createIcon() {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(2, 132, 199));
        g2.fillOval(1, 1, 30, 30);
        g2.setColor(Color.WHITE);
        g2.setStroke(new java.awt.BasicStroke(3f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        g2.drawLine(16, 16, 16, 8);
        g2.drawLine(16, 16, 22, 19);
        g2.dispose();
        return image;
    }
}
