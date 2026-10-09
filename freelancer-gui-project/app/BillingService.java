package app;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Business rules for logging time and generating invoices. */
public class BillingService {

    /** Shared by every BillingService instance so two threads can never invoice at the same moment. */
    private static final Object INVOICE_LOCK = new Object();

    private final TimeEntryDao timeEntryDao = new TimeEntryDao();
    private final InvoiceDao invoiceDao = new InvoiceDao();

    public TimeEntry logTime(Project project, LocalDateTime start, LocalDateTime end)
            throws InvalidInputException, DatabaseException {
        if (project == null) {
            throw new InvalidInputException("Select a project first.");
        }
        if (!end.isAfter(start)) {
            throw new InvalidInputException("End time must be after start time.");
        }
        TimeEntry entry = new TimeEntry(0, project.getId(), start, end);
        timeEntryDao.save(entry);
        return entry;
    }

    /**
     * Creates an invoice. type is "HOURLY" (amount = rate per hour) or "FIXED" (amount = flat fee).
     */
    public Invoice generateInvoice(Project project, String type, double amount)
            throws InvalidInputException, DatabaseException {
        if (project == null) {
            throw new InvalidInputException("Select a project first.");
        }
        synchronized (INVOICE_LOCK) {
            double hours = timeEntryDao.getTotalHours(project.getId());
            Invoice invoice;
            if ("FIXED".equals(type)) {
                invoice = new FixedPriceInvoice(0, project.getId(), project.getFreelancerId(),
                        project.getClientId(), hours, amount, Invoice.Status.PENDING, LocalDate.now());
            } else {
                if (hours <= 0) {
                    throw new InvalidInputException("No time has been logged for this project yet.");
                }
                invoice = new HourlyInvoice(0, project.getId(), project.getFreelancerId(),
                        project.getClientId(), hours, amount, Invoice.Status.PENDING, LocalDate.now());
            }
            invoiceDao.save(invoice);
            invoice.setProjectName(project.getName());
            invoice.setClientName(project.getClientName());
            return invoice;
        }
    }
}
