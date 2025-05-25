package com.alerts;

public class BaseAlert implements Alert {
    private String patientId;
    private String condition;
    private long timestamp;
    private String priority;
    private boolean repeated;

    public BaseAlert(String patientId, String condition, long timestamp) {
        this.patientId = patientId;
        this.condition = condition;
        this.timestamp = timestamp;
        this.priority = "Normal"; // Default priority
        this.repeated = false; // Default not repeated
    }

    @Override
    public String getPatientId() {
        return patientId;
    }

    @Override
    public String getCondition() {
        return condition;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    @Override
    public boolean isRepeated() {
        return repeated;
    }

    @Override
    public void setRepeated(boolean repeated) {
        this.repeated = repeated;
    }

    @Override
    public String getDetails() {
        StringBuilder details = new StringBuilder();
        details.append("Patient ID: ").append(patientId);
        details.append(", Condition: ").append(condition);
        details.append(", Timestamp: ").append(timestamp);
        details.append(", Priority: ").append(priority);
        if (repeated) {
            details.append(" (Repeated)");
        }
        return details.toString();
    }
}
