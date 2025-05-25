package com.alerts;

import com.data_management.Patient;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap; // For managing repeated alerts
import java.util.Map;    // For managing repeated alerts

/**
 * The {@code AlertGenerator} class is responsible for monitoring patient data
 * and generating alerts when certain predefined conditions are met.
 * It uses various AlertStrategy implementations to check for conditions
 * and AlertFactory implementations (indirectly via strategies) to create alerts.
 * It can also apply decorators to alerts.
 */
public class AlertGenerator {
    private List<Alert> triggeredAlerts;
    private List<AlertStrategy> strategies;
    private Map<String, Long> lastAlertTimestamps; // patientId_condition -> timestamp, for repeated alerts

    /**
     * Constructs an {@code AlertGenerator}.
     * Initializes with a set of alert strategies.
     */
    public AlertGenerator() {
        this.triggeredAlerts = new ArrayList<>();
        this.strategies = new ArrayList<>();
        this.lastAlertTimestamps = new HashMap<>();
        // Add default strategies
        strategies.add(new BloodPressureStrategy());
        strategies.add(new OxygenSaturationStrategy());
        strategies.add(new ECGStrategy());
        strategies.add(new HypotensiveHypoxemiaStrategy());
    }
    
    /**
     * Adds an alert strategy to the generator.
     * @param strategy The alert strategy to add.
     */
    public void addStrategy(AlertStrategy strategy) {
        this.strategies.add(strategy);
    }

    /**
     * Evaluates the specified patient's data to determine if any alert conditions
     * are met by delegating to its configured strategies.
     *
     * @param patient the patient data to evaluate for alert conditions
     */
    public void evaluateData(Patient patient) {
        if (patient == null) {
            System.err.println("Cannot evaluate data for a null patient.");
            return;
        }
        for (AlertStrategy strategy : strategies) {
            strategy.checkAlert(patient, this);
        }
    }

    /**
     * Triggers an alert. This method can apply decorators and manage repeated alerts.
     *
     * @param alert The alert to trigger.
     */
    public void triggerAlert(Alert alert) {
        // Logic for handling repeated alerts
        String alertKey = alert.getPatientId() + "_" + alert.getCondition();
        long currentTime = System.currentTimeMillis();
        long repeatInterval = 5 * 60 * 1000; // 5 minutes, example

        if (lastAlertTimestamps.containsKey(alertKey)) {
            long lastTime = lastAlertTimestamps.get(alertKey);
            if (currentTime - lastTime < repeatInterval) {
                System.out.println("Alert suppressed (repeated too soon): " + alert.getDetails());
                return; // Suppress alert if triggered again within the interval
            } else {
                // It's a valid repeat after the interval
                alert = new RepeatedAlertDecorator(alert);
            }
        }
        
        lastAlertTimestamps.put(alertKey, currentTime);

        // Example of applying PriorityDecorator based on condition (could be more sophisticated)
        if (alert.getCondition().toLowerCase().contains("critical") || 
            alert.getCondition().toLowerCase().contains("urgent") ||
            alert.getPriority().equalsIgnoreCase("High") || // If already set by strategy
            alert.getPriority().equalsIgnoreCase("Urgent")) {
            // Ensure it's not already a PriorityAlertDecorator to avoid multiple priority decorations
            if (!(alert instanceof PriorityAlertDecorator) && !alert.getPriority().equals("Urgent")) {
                 // If strategy set High, and we want Urgent, this logic needs refinement.
                 // For now, if it contains critical/urgent words, make it Urgent.
                alert = new PriorityAlertDecorator(alert, "Urgent");
            }
        }


        this.triggeredAlerts.add(alert);
        System.out.println("ALERT TRIGGERED: " + alert.getDetails());
    }

    /**
     * Retrieves the list of alerts triggered during evaluation.
     * @return A list of triggered Alert objects.
     */
    public List<Alert> getAlerts() {
        return new ArrayList<>(triggeredAlerts); // Return a copy
    }

    /**
     * Clears the list of triggered alerts and last alert timestamps.
     * Primarily for resetting state between tests.
     */
    public void clearAlerts() {
        this.triggeredAlerts.clear();
        this.lastAlertTimestamps.clear();
    }

    // Removed specific check methods as they are now in strategies.
    // Removed filterAndSortRecords and findClosestRecord as they are utility methods for strategies.
}
