package com.alerts;

public class BloodOxygenAlertFactory implements AlertFactory {
    @Override
    public Alert createAlert(String patientId, String condition, long timestamp) {
        return new BloodOxygenAlert(patientId, condition, timestamp);
    }
}
