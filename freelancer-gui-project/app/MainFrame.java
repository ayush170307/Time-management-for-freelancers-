package app;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.CardLayout;

/** The single application window. Switches between the home screen and one dashboard at a time. */
public class MainFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel container = new JPanel(cardLayout);
    private JPanel currentDashboard;

    public MainFrame() {
        super("Time Management for Freelancers");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 680);
        setLocationRelativeTo(null);

        container.add(new HomePanel(this), "home");
        add(container);
        cardLayout.show(container, "home");
    }

    public void showDashboard(JPanel dashboard) {
        closeDashboard();
        currentDashboard = dashboard;
        container.add(dashboard, "dashboard");
        cardLayout.show(container, "dashboard");
    }

    public void goHome() {
        closeDashboard();
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
}
