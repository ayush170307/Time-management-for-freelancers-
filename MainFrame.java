package app;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * The single application window. Switches between the home screen and one dashboard at a time,
 * shows toast messages, switches light/dark mode, and can hide itself in the system tray
 * so a running timer is not lost.
 */
public class MainFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel container = new JPanel(cardLayout);
    private final ToastGlassPane toast = new ToastGlassPane();
    private final TrayManager tray = new TrayManager(this);
    private JPanel currentDashboard;

    public MainFrame() {
        super("Time Management for Freelancers");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1140, 740);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleClose();
            }

            @Override
            public void windowIconified(WindowEvent e) {
                if (isTimerRunning() && tray.install()) {
                    setVisible(false);
                    tray.notify("Timer is running", "The timer keeps running in the background.");
                }
            }
        });

        setGlassPane(toast);
        toast.setVisible(true);

        getContentPane().setBackground(Theme.BACKGROUND);
        container.setBackground(Theme.BACKGROUND);
        container.add(new HomePanel(this), "home");
        add(container);
        cardLayout.show(container, "home");
    }

    // ------------------------------------------------------------------ navigation

    public void showDashboard(JPanel dashboard) {
        if (dashboard instanceof FreelancerDashboardPanel) {
            Session.requireRole("FREELANCER");
        } else if (dashboard instanceof ClientDashboardPanel) {
            Session.requireRole("CLIENT");
        }
        closeDashboard();
        currentDashboard = dashboard;
        container.add(dashboard, "dashboard");
        cardLayout.show(container, "dashboard");
    }

    /** Logs the user out and returns to the home screen. */
    public void goHome() {
        closeDashboard();
        Session.logout();
        cardLayout.show(container, "home");
    }

    private void closeDashboard() {
        if (currentDashboard != null) {
            if (currentDashboard instanceof Closable) {
                ((Closable) currentDashboard).onClose();
            }
            container.remove(currentDashboard);
            currentDashboard = null;
            container.revalidate();
            container.repaint();
        }
    }

    // ------------------------------------------------------------------ dark / light mode

    /** Switches between light and dark mode by rebuilding the screens in the new palette. */
    public void toggleTheme() {
        if (isTimerRunning()) {
            showToast("Stop the timer before switching theme.");
            return;
        }
        String page = null;
        if (currentDashboard instanceof DashboardShell) {
            page = ((DashboardShell) currentDashboard).getCurrentPage();
        }
        closeDashboard();

        Theme.setDark(!Theme.isDark());
        Theme.install();
        getContentPane().setBackground(Theme.BACKGROUND);
        container.setBackground(Theme.BACKGROUND);
        container.removeAll();
        container.add(new HomePanel(this), "home");

        User user = Session.getUser();
        if (user instanceof Freelancer) {
            showDashboard(new FreelancerDashboardPanel(this, (Freelancer) user));
        } else if (user instanceof Client) {
            showDashboard(new ClientDashboardPanel(this, (Client) user));
        } else {
            cardLayout.show(container, "home");
        }
        if (page != null && currentDashboard instanceof DashboardShell) {
            ((DashboardShell) currentDashboard).showPage(page);
        }
        container.revalidate();
        container.repaint();
    }

    // ------------------------------------------------------------------ messages and tray

    public void showToast(String message) {
        if (isVisible()) {
            toast.show(message);
        } else {
            tray.notify("Time Management for Freelancers", message);
        }
    }

    public void updateTrayTooltip(String text) {
        tray.setTooltip(text);
    }

    private boolean isTimerRunning() {
        return currentDashboard instanceof FreelancerDashboardPanel
                && ((FreelancerDashboardPanel) currentDashboard).isTimerRunning();
    }

    private void handleClose() {
        if (isTimerRunning()) {
            if (tray.install()) {
                setVisible(false);
                tray.notify("Timer still running",
                        "It keeps running in the background. Use the tray icon to reopen or exit.");
                return;
            }
            int answer = JOptionPane.showConfirmDialog(this,
                    "A timer is still running and will be lost if you exit. Exit anyway?",
                    "Timer running", JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }
        exitApplication();
    }

    public void exitApplication() {
        tray.remove();
        dispose();
        System.exit(0);
    }

    public void restoreFromTray() {
        setVisible(true);
        setExtendedState(JFrame.NORMAL);
        toFront();
        tray.remove();
    }
}
