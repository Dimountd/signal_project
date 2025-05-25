package com.alerts;

public interface AlertFactory {
    Alert createAlert(String patientId, String condition, long timestamp);
}
