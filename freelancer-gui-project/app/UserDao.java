package app;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class UserDao extends BaseDao<User> {

    private static final String BASE = "SELECT id, name, email, role, password_hash FROM users ";

    @Override
    public int save(User user) throws DatabaseException {
        int id = insert("INSERT INTO users(name, email, role, password_hash) VALUES (?, ?, ?, ?)",
                user.getName(), user.getEmail(), user.getRole(), user.getPasswordHash());
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

    public Optional<User> findByEmail(String email) throws DatabaseException {
        return queryOne(BASE + "WHERE LOWER(email) = LOWER(?)", UserDao::map, email);
    }

    public List<User> findByRole(String role) throws DatabaseException {
        return query(BASE + "WHERE role = ? ORDER BY name", UserDao::map, role);
    }

    private static User map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String role = rs.getString("role");
        User user;
        if ("CLIENT".equals(role)) {
            user = new Client(id, name, email);
        } else {
            user = new Freelancer(id, name, email);
        }
        user.setPasswordHash(rs.getString("password_hash"));
        return user;
    }
}
