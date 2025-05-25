package com.alerts;

/**
 * Interface representing an alert in the system.
 * Provides methods to access alert details, including priority and specific information
 * that can be enhanced by decorators.
 */
public interface Alert {
    /**
     * Gets the patient ID associated with this alert.
     * @return The patient ID.
     */
    String getPatientId();

    /**
     * Gets the primary condition that triggered this alert.
     * @return The condition string.
     */
    String getCondition();

    /**
     * Gets the timestamp when the alert was generated.
     * @return The timestamp in milliseconds since epoch.
     */
    long getTimestamp();

    /**
     * Gets the priority of the alert.
     * Default priority might be "Normal". Decorators can change this.
     * @return The priority string (e.g., "Low", "Normal", "High", "Urgent").
     */
    String getPriority();

    /**
     * Gets detailed information about the alert, potentially including
     * information added by decorators (e.g., priority, repetition status).
     * @return A string containing detailed alert information.
     */
    String getDetails();

    /**
     * Indicates if this alert is a repeated occurrence of a previously triggered alert.
     * @return true if the alert is marked as repeated, false otherwise.
     */
    boolean isRepeated();

    /**
     * Sets the repeated status of this alert.
     * @param repeated true to mark as repeated, false otherwise.
     */
    void setRepeated(boolean repeated);
}
