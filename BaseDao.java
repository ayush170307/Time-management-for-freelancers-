package app;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared JDBC plumbing (PreparedStatements, try-with-resources, error wrapping).
 * Each concrete DAO only has to supply SQL and a row mapper.
 */
public abstract class BaseDao<T> implements Dao<T> {

    /** Converts one row of a ResultSet into an object. */
    @FunctionalInterface
    protected interface RowMapper<R> {
        R map(ResultSet rs) throws SQLException;
    }

    protected List<T> query(String sql, RowMapper<T> mapper, Object... params) throws DatabaseException {
        List<T> result = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapper.map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Database read failed: " + e.getMessage(), e);
        }
        return result;
    }

    protected Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) throws DatabaseException {
        List<T> rows = query(sql, mapper, params);
        return rows.isEmpty() ? Optional.<T>empty() : Optional.of(rows.get(0));
    }

    /** Runs an INSERT and returns the generated primary key. */
    protected int insert(String sql, Object... params) throws DatabaseException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                throw new DatabaseException(
                        "That record conflicts with existing data (for example, the email may already be registered).", e);
            }
            throw new DatabaseException("Database write failed: " + e.getMessage(), e);
        }
    }

    protected int update(String sql, Object... params) throws DatabaseException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Database update failed: " + e.getMessage(), e);
        }
    }

    private void bind(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p instanceof LocalDate) {
                ps.setDate(i + 1, Date.valueOf((LocalDate) p));
            } else if (p instanceof LocalDateTime) {
                ps.setTimestamp(i + 1, Timestamp.valueOf((LocalDateTime) p));
            } else {
                ps.setObject(i + 1, p);
            }
        }
    }
}
