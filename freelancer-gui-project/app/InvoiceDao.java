package app;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class InvoiceDao extends BaseDao<Invoice> {

    private static final String BASE =
            "SELECT i.id, i.project_id, i.freelancer_id, i.client_id, i.invoice_type, i.hours, i.rate, "
                    + "i.status, i.created_date, p.name AS project_name, c.name AS client_name "
                    + "FROM invoices i "
                    + "JOIN projects p ON i.project_id = p.id "
                    + "JOIN users c ON i.client_id = c.id ";

    @Override
    public int save(Invoice invoice) throws DatabaseException {
        int id = insert(
                "INSERT INTO invoices(project_id, freelancer_id, client_id, invoice_type, hours, rate, status, created_date) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                invoice.getProjectId(), invoice.getFreelancerId(), invoice.getClientId(), invoice.getType(),
                invoice.getHours(), invoice.getRate(), invoice.getStatus().name(), invoice.getCreatedDate());
        invoice.setId(id);
        return id;
    }

    @Override
    public Optional<Invoice> findById(int id) throws DatabaseException {
        return queryOne(BASE + "WHERE i.id = ?", InvoiceDao::map, id);
    }

    @Override
    public List<Invoice> findAll() throws DatabaseException {
        return query(BASE + "ORDER BY i.id", InvoiceDao::map);
    }

    public List<Invoice> findByFreelancer(int freelancerId) throws DatabaseException {
        return query(BASE + "WHERE i.freelancer_id = ? ORDER BY i.id DESC", InvoiceDao::map, freelancerId);
    }

    public List<Invoice> findByClient(int clientId) throws DatabaseException {
        return query(BASE + "WHERE i.client_id = ? ORDER BY i.id DESC", InvoiceDao::map, clientId);
    }

    public void markPaid(int invoiceId) throws DatabaseException {
        update("UPDATE invoices SET status = ? WHERE id = ?", Invoice.Status.PAID.name(), invoiceId);
    }

    private static Invoice map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int projectId = rs.getInt("project_id");
        int freelancerId = rs.getInt("freelancer_id");
        int clientId = rs.getInt("client_id");
        double hours = rs.getDouble("hours");
        double rate = rs.getDouble("rate");
        Invoice.Status status = Invoice.Status.valueOf(rs.getString("status"));
        LocalDate created = rs.getDate("created_date").toLocalDate();

        Invoice invoice;
        if ("FIXED".equals(rs.getString("invoice_type"))) {
            invoice = new FixedPriceInvoice(id, projectId, freelancerId, clientId, hours, rate, status, created);
        } else {
            invoice = new HourlyInvoice(id, projectId, freelancerId, clientId, hours, rate, status, created);
        }
        invoice.setProjectName(rs.getString("project_name"));
        invoice.setClientName(rs.getString("client_name"));
        return invoice;
    }
}
