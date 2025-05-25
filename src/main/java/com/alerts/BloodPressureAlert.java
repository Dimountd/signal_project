package com.alerts;

public class BloodPressureAlert extends BaseAlert {
    public BloodPressureAlert(String patientId, String condition, long timestamp) {
        super(patientId, "BloodPressure: " + condition, timestamp);
    }
}
