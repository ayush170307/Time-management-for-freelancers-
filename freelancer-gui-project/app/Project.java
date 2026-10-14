package app;

import java.time.LocalDate;

public class Project {

    public enum Status {
        NOT_STARTED, IN_PROGRESS, COMPLETED
    }

    private int id;
    private final String name;
    private final String description;
    private final LocalDate deadline;
    private final Status status;
    private final int freelancerId;
    private final int clientId;
    private String clientName;
    private String freelancerName;

    public Project(int id, String name, String description, LocalDate deadline,
                   Status status, int freelancerId, int clientId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.deadline = deadline;
        this.status = status;
        this.freelancerId = freelancerId;
        this.clientId = clientId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public Status getStatus() {
        return status;
    }

    public int getFreelancerId() {
        return freelancerId;
    }

    public int getClientId() {
        return clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getFreelancerName() {
        return freelancerName;
    }

    public void setFreelancerName(String freelancerName) {
        this.freelancerName = freelancerName;
    }

    @Override
    public String toString() {
        return name + " (#" + id + ")";
    }
}
