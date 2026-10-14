package app;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ProjectDao extends BaseDao<Project> {

    private static final String BASE =
            "SELECT p.id, p.name, p.description, p.deadline, p.status, p.freelancer_id, p.client_id, "
                    + "c.name AS client_name, f.name AS freelancer_name "
                    + "FROM projects p "
                    + "JOIN users c ON p.client_id = c.id "
                    + "JOIN users f ON p.freelancer_id = f.id ";

    @Override
    public int save(Project project) throws DatabaseException {
        int id = insert(
                "INSERT INTO projects(name, description, deadline, status, freelancer_id, client_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                project.getName(), project.getDescription(), project.getDeadline(),
                project.getStatus().name(), project.getFreelancerId(), project.getClientId());
        project.setId(id);
        return id;
    }

    @Override
    public Optional<Project> findById(int id) throws DatabaseException {
        return queryOne(BASE + "WHERE p.id = ?", ProjectDao::map, id);
    }

    @Override
    public List<Project> findAll() throws DatabaseException {
        return query(BASE + "ORDER BY p.id", ProjectDao::map);
    }

    public List<Project> findByFreelancer(int freelancerId) throws DatabaseException {
        return query(BASE + "WHERE p.freelancer_id = ? ORDER BY p.id", ProjectDao::map, freelancerId);
    }

    public List<Project> findByClient(int clientId) throws DatabaseException {
        return query(BASE + "WHERE p.client_id = ? ORDER BY p.id", ProjectDao::map, clientId);
    }

    public void updateStatus(int projectId, Project.Status status) throws DatabaseException {
        update("UPDATE projects SET status = ? WHERE id = ?", status.name(), projectId);
    }

    private static Project map(ResultSet rs) throws SQLException {
        Project p = new Project(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getDate("deadline").toLocalDate(),
                Project.Status.valueOf(rs.getString("status")),
                rs.getInt("freelancer_id"),
                rs.getInt("client_id"));
        p.setClientName(rs.getString("client_name"));
        p.setFreelancerName(rs.getString("freelancer_name"));
        return p;
    }
}
