package app;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared layout for both dashboards: a dark sidebar with navigation on the left,
 * and a header plus the current page on the right. Pages fade in when you switch,
 * and every page change is checked against the logged-in user's role.
 */
public class DashboardShell extends JPanel {

    /** Page container that can draw a fading veil over its children (used for the page transition). */
    private static class FadePanel extends JPanel {
        private float fade = 1f;

        FadePanel(CardLayout layout) {
            super(layout);
        }

        void setFade(float value) {
            this.fade = value;
            repaint();
        }

        @Override
        protected void paintChildren(Graphics g) {
            super.paintChildren(g);
            if (fade < 1f) {
                Graphics2D g2 = (Graphics2D) g.create();
                int alpha = Math.max(0, Math.min(255, Math.round(255 * (1f - fade))));
                Color bg = Theme.BACKGROUND;
                g2.setColor(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), alpha));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        }
    }

    private final CardLayout cards = new CardLayout();
    private final FadePanel content = new FadePanel(cards);
    private final JPanel nav = new JPanel(new GridLayout(0, 1, 0, 6));
    private final JLabel pageTitle = new JLabel();
    private final String requiredRole;

    private final Map<String, RoundedButton> buttons = new LinkedHashMap<>();
    private final Map<String, String> titles = new HashMap<>();
    private final Map<String, Runnable> onShow = new HashMap<>();
    private String current;
    private Animator fadeAnimation;

    /**
     * @param requiredRole  the role that is allowed to see this dashboard ("FREELANCER" or "CLIENT")
     * @param onToggleTheme called when the user clicks the light/dark mode button
     */
    public DashboardShell(String userName, String roleText, String requiredRole,
                          Runnable onLogout, Runnable onToggleTheme) {
        this.requiredRole = requiredRole;
        setLayout(new BorderLayout());

        // ---- sidebar
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Theme.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(24, 14, 20, 14));

        JPanel profile = new JPanel(new GridLayout(2, 1, 0, 2));
        profile.setOpaque(false);
        profile.setBorder(BorderFactory.createEmptyBorder(0, 8, 24, 8));
        JLabel nameLabel = new JLabel(userName);
        nameLabel.setFont(Theme.BOLD.deriveFont(17f));
        nameLabel.setForeground(Color.WHITE);
        JLabel roleLabel = new JLabel(roleText.toUpperCase());
        roleLabel.setFont(Theme.SMALL_BOLD);
        roleLabel.setForeground(new Color(125, 211, 252));
        profile.add(nameLabel);
        profile.add(roleLabel);
        sidebar.add(profile, BorderLayout.NORTH);

        nav.setOpaque(false);
        JPanel navHolder = new JPanel(new BorderLayout());
        navHolder.setOpaque(false);
        navHolder.add(nav, BorderLayout.NORTH);
        sidebar.add(navHolder, BorderLayout.CENTER);

        RoundedButton themeBtn = new RoundedButton(Theme.isDark() ? "Light mode" : "Dark mode",
                RoundedButton.Style.SIDEBAR);
        themeBtn.addActionListener(e -> onToggleTheme.run());
        RoundedButton logout = new RoundedButton("Log out", RoundedButton.Style.SIDEBAR);
        logout.addActionListener(e -> onLogout.run());
        JPanel bottom = new JPanel(new GridLayout(2, 1, 0, 6));
        bottom.setOpaque(false);
        bottom.add(themeBtn);
        bottom.add(logout);
        sidebar.add(bottom, BorderLayout.SOUTH);

        // ---- main area
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(18, 26, 18, 26)));
        pageTitle.setFont(Theme.HEADING);
        pageTitle.setForeground(Theme.TEXT);
        header.add(pageTitle, BorderLayout.WEST);

        content.setBackground(Theme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Theme.BACKGROUND);
        main.add(header, BorderLayout.NORTH);
        main.add(content, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(main, BorderLayout.CENTER);
    }

    /**
     * Adds a page and a sidebar button for it.
     *
     * @param onShowAction optional code to run every time the page is opened (may be null)
     */
    public void addPage(String name, String title, JComponent page, Runnable onShowAction) {
        RoundedButton button = new RoundedButton(name, RoundedButton.Style.SIDEBAR);
        button.addActionListener(e -> showPage(name));
        nav.add(button);
        buttons.put(name, button);
        titles.put(name, title);
        if (onShowAction != null) {
            onShow.put(name, onShowAction);
        }
        content.add(page, name);
        if (current == null) {
            showPage(name);
        }
    }

    public void showPage(String name) {
        // session check on every navigation event: only the right role may open these pages
        Session.requireRole(requiredRole);

        current = name;
        cards.show(content, name);
        pageTitle.setText(titles.get(name));
        for (Map.Entry<String, RoundedButton> entry : buttons.entrySet()) {
            entry.getValue().setActive(entry.getKey().equals(name));
        }
        Runnable action = onShow.get(name);
        if (action != null) {
            action.run();
        }
        if (fadeAnimation != null) {
            fadeAnimation.stop();
        }
        fadeAnimation = Animator.start(280, p -> content.setFade((float) p));
    }

    public String getCurrentPage() {
        return current;
    }

    /** Shows a red counter next to a sidebar entry; 0 hides it. */
    public void setBadge(String pageName, int count) {
        RoundedButton button = buttons.get(pageName);
        if (button != null) {
            button.setBadge(count);
        }
    }
}
