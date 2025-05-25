package com.alerts;

public class ECGAlert extends BaseAlert {
    public ECGAlert(String patientId, String condition, long timestamp) {
        super(patientId, "ECG: " + condition, timestamp);
    }
}
