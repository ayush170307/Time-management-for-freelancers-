package app;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class MessageDao extends BaseDao<Message> {

    private static final String BASE =
            "SELECT m.id, m.sender_id, m.receiver_id, m.content, m.sent_at, "
                    + "s.name AS sender_name, r.name AS receiver_name "
                    + "FROM messages m "
                    + "JOIN users s ON m.sender_id = s.id "
                    + "JOIN users r ON m.receiver_id = r.id ";

    @Override
    public int save(Message message) throws DatabaseException {
        int id = insert("INSERT INTO messages(sender_id, receiver_id, content, sent_at) VALUES (?, ?, ?, ?)",
                message.getSenderId(), message.getReceiverId(), message.getContent(), message.getSentAt());
        message.setId(id);
        return id;
    }

    @Override
    public Optional<Message> findById(int id) throws DatabaseException {
        return queryOne(BASE + "WHERE m.id = ?", MessageDao::map, id);
    }

    @Override
    public List<Message> findAll() throws DatabaseException {
        return query(BASE + "ORDER BY m.sent_at DESC", MessageDao::map);
    }

    public List<Message> findSentBy(int userId) throws DatabaseException {
        return query(BASE + "WHERE m.sender_id = ? ORDER BY m.sent_at DESC", MessageDao::map, userId);
    }

    public List<Message> findReceivedBy(int userId) throws DatabaseException {
        return query(BASE + "WHERE m.receiver_id = ? ORDER BY m.sent_at DESC", MessageDao::map, userId);
    }

    private static Message map(ResultSet rs) throws SQLException {
        Message m = new Message(
                rs.getInt("id"),
                rs.getInt("sender_id"),
                rs.getInt("receiver_id"),
                rs.getString("content"),
                rs.getTimestamp("sent_at").toLocalDateTime());
        m.setSenderName(rs.getString("sender_name"));
        m.setReceiverName(rs.getString("receiver_name"));
        return m;
    }
}
