package app;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Business rules for logging time and generating invoices. Every action checks the session first. */
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
        Session.requireUser(project.getFreelancerId());
        if (!end.isAfter(start)) {
            throw new InvalidInputException("End time must be after start time.");
        }
        TimeEntry entry = new TimeEntry(0, project.getId(), start, end);
        timeEntryDao.save(entry);
        return entry;
    }

    /**
     * Adds up the hours logged on one project between two dates (inclusive).
     * A null date means "no limit" on that side.
     */
    public static double sumHours(List<TimeEntry> entries, int projectId, LocalDate from, LocalDate to) {
        double total = 0;
        for (TimeEntry entry : entries) {
            if (entry.getProjectId() != projectId) {
                continue;
            }
            LocalDate day = entry.getStartTime().toLocalDate();
            if (from != null && day.isBefore(from)) {
                continue;
            }
            if (to != null && day.isAfter(to)) {
                continue;
            }
            total += entry.getHoursWorked();
        }
        return total;
    }

    public Invoice generateInvoice(Project project, String type, double amount)
            throws InvalidInputException, DatabaseException {
        return generateInvoice(project, type, amount, null, null);
    }

    /**
     * Creates an invoice from the time logged in a date range.
     * type is "HOURLY" (amount = rate per hour) or "FIXED" (amount = flat fee).
     */
    public Invoice generateInvoice(Project project, String type, double amount, LocalDate from, LocalDate to)
            throws InvalidInputException, DatabaseException {
        if (project == null) {
            throw new InvalidInputException("Select a project first.");
        }
        Session.requireUser(project.getFreelancerId());
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidInputException("The start date must not be after the end date.");
        }
        synchronized (INVOICE_LOCK) {
            double hours = sumHours(timeEntryDao.findByProject(project.getId()), project.getId(), from, to);
            Invoice invoice;
            if ("FIXED".equals(type)) {
                invoice = new FixedPriceInvoice(0, project.getId(), project.getFreelancerId(),
                        project.getClientId(), hours, amount, Invoice.Status.PENDING, LocalDate.now());
            } else {
                if (hours <= 0) {
                    throw new InvalidInputException("No time was logged on this project in that period.");
                }
                invoice = new HourlyInvoice(0, project.getId(), project.getFreelancerId(),
                        project.getClientId(), hours, amount, Invoice.Status.PENDING, LocalDate.now());
            }
            invoiceDao.save(invoice);
            invoice.setProjectName(project.getName());
            invoice.setClientName(project.getClientName());
            invoice.setFreelancerName(project.getFreelancerName());
            return invoice;
        }
    }
}
