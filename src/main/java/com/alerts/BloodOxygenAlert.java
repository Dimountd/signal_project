package com.alerts;

public class BloodOxygenAlert extends BaseAlert {
    public BloodOxygenAlert(String patientId, String condition, long timestamp) {
        super(patientId, "BloodOxygen: " + condition, timestamp);
    }
}
