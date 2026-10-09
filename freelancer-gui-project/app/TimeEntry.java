package app;

import java.time.Duration;
import java.time.LocalDateTime;

public class TimeEntry {

    private int id;
    private final int projectId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private String projectName;

    public TimeEntry(int id, int projectId, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.projectId = projectId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProjectId() {
        return projectId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /** Hours worked as a decimal, e.g. 1.5 = ninety minutes. */
    public double getHoursWorked() {
        return Duration.between(startTime, endTime).toMillis() / 3600000.0;
    }
}
