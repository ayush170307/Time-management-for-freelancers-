package app;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.List;

/**
 * The full leaderboard screen: a summary card with a "This week / All time" switch,
 * and the animated ranking below it. Used by both dashboards.
 */
public class LeaderboardPage extends JPanel {

    private final int currentUserId;
    private final LeaderboardService service = new LeaderboardService();
    private final LeaderboardPanel board = new LeaderboardPanel();

    private final JLabel summary = new JLabel(" ");
    private final JLabel detail = Theme.label(" ");
    private final RoundedButton weekBtn = new RoundedButton("This week", RoundedButton.Style.SEGMENT);
    private final RoundedButton allBtn = new RoundedButton("All time", RoundedButton.Style.SEGMENT);
    private boolean weekOnly = false;

    /** @param currentUserId the logged-in freelancer's id, or -1 when a client is looking */
    public LeaderboardPage(int currentUserId) {
        super(new BorderLayout(0, 4));
        this.currentUserId = currentUserId;
        setOpaque(false);

        summary.setFont(Theme.HEADING.deriveFont(18f));
        summary.setForeground(Theme.TEXT);
        JPanel texts = new JPanel(new GridLayout(2, 1, 0, 2));
        texts.setOpaque(false);
        texts.add(summary);
        texts.add(detail);

        weekBtn.addActionListener(e -> setPeriod(true));
        allBtn.addActionListener(e -> setPeriod(false));
        JPanel toggles = new JPanel(new GridLayout(1, 2, 8, 0));
        toggles.setOpaque(false);
        toggles.add(weekBtn);
        toggles.add(allBtn);
        JPanel togglesWrap = new JPanel(new GridBagLayout());
        togglesWrap.setOpaque(false);
        togglesWrap.add(toggles);

        JPanel top = Theme.card();
        top.setLayout(new BorderLayout(16, 0));
        top.add(texts, BorderLayout.CENTER);
        top.add(togglesWrap, BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(board);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        Theme.styleScroll(scroll);

        JPanel boardCard = Theme.card();
        boardCard.setLayout(new BorderLayout());
        boardCard.add(scroll, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(boardCard, BorderLayout.CENTER);
        updateToggles();
    }

    private void setPeriod(boolean week) {
        weekOnly = week;
        updateToggles();
        refresh();
    }

    private void updateToggles() {
        weekBtn.setActive(weekOnly);
        allBtn.setActive(!weekOnly);
    }

    /** Reloads the ranking from the database and replays the animation. */
    public void refresh() {
        try {
            List<LeaderboardEntry> ranking = service.build(weekOnly);
            board.setData(ranking, currentUserId);
            updateSummary(ranking);
        } catch (DatabaseException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    private void updateSummary(List<LeaderboardEntry> ranking) {
        String period = weekOnly ? "this week" : "of all time";
        if (ranking.isEmpty()) {
            summary.setText("No freelancers yet");
            detail.setText("Freelancers appear here as soon as they register.");
            return;
        }
        LeaderboardEntry me = null;
        for (LeaderboardEntry e : ranking) {
            if (e.getId() == currentUserId) {
                me = e;
            }
        }
        if (me == null) {
            summary.setText("Top freelancers " + period);
            LeaderboardEntry first = ranking.get(0);
            detail.setText(first.getHours() > 0
                    ? first.getName() + " leads with " + Ui.fmt(first.getHours()) + " hours"
                    : "No hours have been logged yet");
        } else {
            summary.setText("You are #" + me.getRank() + " of " + ranking.size() + " " + period);
            if (me.getRank() == 1) {
                detail.setText(me.getHours() > 0
                        ? "You are in the lead with " + Ui.fmt(me.getHours()) + " hours. Keep it up!"
                        : "Log some time to take the lead.");
            } else {
                LeaderboardEntry ahead = ranking.get(me.getRank() - 2);
                detail.setText(Ui.fmt(ahead.getHours() - me.getHours()) + " hours behind " + ahead.getName());
            }
        }
    }
}
