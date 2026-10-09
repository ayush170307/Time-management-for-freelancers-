package app;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class UserDao extends BaseDao<User> {

    private static final String BASE = "SELECT id, name, email, role FROM users ";

    @Override
    public int save(User user) throws DatabaseException {
        int id = insert("INSERT INTO users(name, email, role) VALUES (?, ?, ?)",
                user.getName(), user.getEmail(), user.getRole());
        user.setId(id);
        return id;
    }

    @Override
    public Optional<User> findById(int id) throws DatabaseException {
        return queryOne(BASE + "WHERE id = ?", UserDao::map, id);
    }

    @Override
    public List<User> findAll() throws DatabaseException {
        return query(BASE + "ORDER BY id", UserDao::map);
    }

    public List<User> findByRole(String role) throws DatabaseException {
        return query(BASE + "WHERE role = ? ORDER BY name", UserDao::map, role);
    }

    private static User map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String role = rs.getString("role");
        if ("CLIENT".equals(role)) {
            return new Client(id, name, email);
        }
        return new Freelancer(id, name, email);
    }
}
