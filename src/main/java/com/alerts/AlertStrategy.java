package com.alerts;

import com.data_management.Patient;

public interface AlertStrategy {
    void checkAlert(Patient patient, AlertGenerator alertGenerator);
}
