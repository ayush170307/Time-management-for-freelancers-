package app;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds the freelancer ranking from the logged time entries. */
public class LeaderboardService {

    private final UserDao userDao = new UserDao();
    private final TimeEntryDao timeEntryDao = new TimeEntryDao();

    /**
     * @param weekOnly true to count only the last 7 days, false for all time
     * @return every registered freelancer, ranked by hours worked (most first)
     */
    public List<LeaderboardEntry> build(boolean weekOnly) throws DatabaseException {
        Map<Integer, LeaderboardEntry> byFreelancer = new LinkedHashMap<>();
        for (User freelancer : userDao.findByRole("FREELANCER")) {
            byFreelancer.put(freelancer.getId(), new LeaderboardEntry(freelancer.getId(), freelancer.getName()));
        }

        LocalDateTime since = LocalDateTime.now().minusDays(7);
        for (TimeEntry entry : timeEntryDao.findAll()) {
            if (weekOnly && !entry.getStartTime().isAfter(since)) {
                continue;
            }
            LeaderboardEntry row = byFreelancer.get(entry.getFreelancerId());
            if (row != null) {
                row.addHours(entry.getHoursWorked());
            }
        }

        List<LeaderboardEntry> ranking = new ArrayList<>(byFreelancer.values());
        ranking.sort((a, b) -> {
            int byHours = Double.compare(b.getHours(), a.getHours());
            return byHours != 0 ? byHours : a.getName().compareToIgnoreCase(b.getName());
        });
        for (int i = 0; i < ranking.size(); i++) {
            ranking.get(i).setRank(i + 1);
        }
        return ranking;
    }
}
