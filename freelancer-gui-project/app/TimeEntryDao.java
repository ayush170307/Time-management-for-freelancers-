package app;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class TimeEntryDao extends BaseDao<TimeEntry> {

    private static final String BASE =
            "SELECT t.id, t.project_id, t.start_time, t.end_time, p.name AS project_name "
                    + "FROM time_entries t JOIN projects p ON t.project_id = p.id ";

    @Override
    public int save(TimeEntry entry) throws DatabaseException {
        int id = insert("INSERT INTO time_entries(project_id, start_time, end_time) VALUES (?, ?, ?)",
                entry.getProjectId(), entry.getStartTime(), entry.getEndTime());
        entry.setId(id);
        return id;
    }

    @Override
    public Optional<TimeEntry> findById(int id) throws DatabaseException {
        return queryOne(BASE + "WHERE t.id = ?", TimeEntryDao::map, id);
    }

    @Override
    public List<TimeEntry> findAll() throws DatabaseException {
        return query(BASE + "ORDER BY t.start_time DESC", TimeEntryDao::map);
    }

    public List<TimeEntry> findByProject(int projectId) throws DatabaseException {
        return query(BASE + "WHERE t.project_id = ? ORDER BY t.start_time DESC", TimeEntryDao::map, projectId);
    }

    public List<TimeEntry> findByFreelancer(int freelancerId) throws DatabaseException {
        return query(BASE + "WHERE p.freelancer_id = ? ORDER BY t.start_time DESC", TimeEntryDao::map, freelancerId);
    }

    /** Sum of all logged hours for one project. */
    public double getTotalHours(int projectId) throws DatabaseException {
        double total = 0;
        for (TimeEntry entry : findByProject(projectId)) {
            total += entry.getHoursWorked();
        }
        return total;
    }

    private static TimeEntry map(ResultSet rs) throws SQLException {
        TimeEntry t = new TimeEntry(
                rs.getInt("id"),
                rs.getInt("project_id"),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime());
        t.setProjectName(rs.getString("project_name"));
        return t;
    }
}
