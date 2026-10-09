package app;

import java.time.LocalDate;

/**
 * Abstract invoice. Subclasses decide HOW the total is calculated (polymorphism).
 */
public abstract class Invoice implements Billable {

    public enum Status {
        PENDING, PAID
    }

    protected int id;
    protected final int projectId;
    protected final int freelancerId;
    protected final int clientId;
    protected final double hours;
    protected final double rate;
    protected final Status status;
    protected final LocalDate createdDate;
    protected String projectName;
    protected String clientName;

    protected Invoice(int id, int projectId, int freelancerId, int clientId,
                      double hours, double rate, Status status, LocalDate createdDate) {
        this.id = id;
        this.projectId = projectId;
        this.freelancerId = freelancerId;
        this.clientId = clientId;
        this.hours = hours;
        this.rate = rate;
        this.status = status;
        this.createdDate = createdDate;
    }

    public abstract String getType();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProjectId() {
        return projectId;
    }

    public int getFreelancerId() {
        return freelancerId;
    }

    public int getClientId() {
        return clientId;
    }

    public double getHours() {
        return hours;
    }

    public double getRate() {
        return rate;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String describe() {
        return String.format(
                "Invoice #%d%nType: %s%nClient: %s%nProject: %s%nHours logged: %.2f%nRate / amount: %.2f%nTOTAL DUE: %.2f%nStatus: %s%nDate: %s",
                id, getType(), clientName, projectName, hours, rate, calculateTotal(), status, createdDate);
    }
}
