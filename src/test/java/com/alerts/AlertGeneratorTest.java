package com.alerts;

import static org.junit.jupiter.api.Assertions.*;

import com.data_management.Patient;
import com.data_management.PatientRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class AlertGeneratorTest {

    private AlertGenerator alertGenerator;
    private Patient patient;

    @BeforeEach
    void setUp() {
        alertGenerator = new AlertGenerator();
        patient = new Patient(1); // Assuming patient ID 1 for tests
        alertGenerator.clearAlerts(); // Clear alerts before each test
    }

    private void assertAlertTriggered(String expectedConditionSubstring, String expectedPatientId) {
        List<Alert> alerts = alertGenerator.getAlerts();
        assertTrue(alerts.stream()
                        .anyMatch(alert -> alert.getCondition().contains(expectedConditionSubstring) &&
                                           alert.getPatientId().equals(expectedPatientId)),
                "Expected alert with condition containing '" + expectedConditionSubstring + "' for patient " + expectedPatientId + " was not triggered.");
    }

    private void assertNoAlertTriggered() {
        assertTrue(alertGenerator.getAlerts().isEmpty(), "Expected no alerts to be triggered.");
    }

    // Blood Pressure Alerts
    @Test
    void testIncreasingBloodPressureTrend_TriggersAlert() {
        long currentTime = System.currentTimeMillis();
        patient.addRecord(100, "SystolicPressure", currentTime - 20000);
        patient.addRecord(115, "SystolicPressure", currentTime - 10000); // +15
        patient.addRecord(130, "SystolicPressure", currentTime);          // +15
        alertGenerator.evaluateData(patient);
        assertAlertTriggered("Systolic BP increasing trend", "1");
    }

    @Test
    void testDecreasingBloodPressureTrend_TriggersAlert() {
        long currentTime = System.currentTimeMillis();
        patient.addRecord(130, "DiastolicPressure", currentTime - 20000);
        patient.addRecord(115, "DiastolicPressure", currentTime - 10000); // -15
        patient.addRecord(100, "DiastolicPressure", currentTime);          // -15
        alertGenerator.evaluateData(patient);
        assertAlertTriggered("Diastolic BP decreasing trend", "1");
    }

    @Test
    void testCriticalSystolicHighBloodPressure_TriggersAlert() {
        patient.addRecord(181, "SystolicPressure", System.currentTimeMillis());
        patient.addRecord(80, "DiastolicPressure", System.currentTimeMillis());
        alertGenerator.evaluateData(patient);
        assertAlertTriggered("Critical BP: 181.0/80.0", "1");
    }
    
    @Test
    void testCriticalDiastolicHighBloodPressure_TriggersAlert() {
        patient.addRecord(120, "SystolicPressure", System.currentTimeMillis());
        patient.addRecord(121, "DiastolicPressure", System.currentTimeMillis());
        alertGenerator.evaluateData(patient);
        assertAlertTriggered("Critical BP: 120.0/121.0", "1");
    }

    @Test
    void testCriticalSystolicLowBloodPressure_TriggersAlert() {
        patient.addRecord(89, "SystolicPressure", System.currentTimeMillis());
        patient.addRecord(80, "DiastolicPressure", System.currentTimeMillis());
        alertGenerator.evaluateData(patient);
        assertAlertTriggered("Critical BP: 89.0/80.0", "1");
    }

    @Test
    void testCriticalDiastolicLowBloodPressure_TriggersAlert() {
        patient.addRecord(120, "SystolicPressure", System.currentTimeMillis());
        patient.addRecord(59, "DiastolicPressure", System.currentTimeMillis());
        alertGenerator.evaluateData(patient);
        assertAlertTriggered("Critical BP: 120.0/59.0", "1");
    }


    // Blood Oxygen Saturation Alerts
    @Test
    void testLowBloodSaturation_TriggersAlert() {
        patient.addRecord(91, "Saturation", System.currentTimeMillis());
        alertGenerator.evaluateData(patient);
        assertAlertTriggered("Low blood oxygen saturation: 91.0%", "1");
    }

    @Test
    void testRapidBloodSaturationDrop_TriggersAlert() {
        long currentTime = System.currentTimeMillis();
        patient.addRecord(98, "Saturation", currentTime - (5 * 60 * 1000)); // 5 minutes ago
        patient.addRecord(92, "Saturation", currentTime);                   // Drop of 6%
        alertGenerator.evaluateData(patient);
        assertAlertTriggered("Rapid drop in SpO2", "1");
    }
    
    @Test
    void testBloodSaturationDrop_NotRapidEnough_NoAlert() {
        long currentTime = System.currentTimeMillis();
        patient.addRecord(98, "Saturation", currentTime - (12 * 60 * 1000)); // 12 minutes ago
        patient.addRecord(93, "Saturation", currentTime);                   // Drop of 5% but over 10 mins
        alertGenerator.evaluateData(patient);
        assertNoAlertTriggered();
    }


    // Hypotensive Hypoxemia Alert
    @Test
    void testHypotensiveHypoxemia_TriggersAlert() {
        long currentTime = System.currentTimeMillis();
        patient.addRecord(89, "SystolicPressure", currentTime); // Hypotensive
        patient.addRecord(70, "DiastolicPressure", currentTime);
        patient.addRecord(91, "Saturation", currentTime);     // Hypoxemia
        alertGenerator.evaluateData(patient);
        assertAlertTriggered("Hypotensive Hypoxemia", "1");
    }

    // ECG Alerts
    @Test
    void testECGAbnormalPeak_TriggersAlert() {
        long currentTime = System.currentTimeMillis();
        // Baseline ECG data
        for (int i = 0; i < 9; i++) {
            patient.addRecord(0.1 + (i * 0.01), "ECG", currentTime - ( (10 - i) * 1000)); // Small, slightly increasing values
        }
        // Abnormal peak
        patient.addRecord(2.5, "ECG", currentTime); // Significantly higher peak
        alertGenerator.evaluateData(patient);
        assertAlertTriggered("ECG Abnormal Peak", "1");
    }
    
    @Test
    void testECGNormalData_NoAlert() {
        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            patient.addRecord(0.1 + Math.random() * 0.1, "ECG", currentTime - ((10 - i) * 1000));
        }
        alertGenerator.evaluateData(patient);
        assertNoAlertTriggered();
    }


    // Evaluate Data Method
    @Test
    void testEvaluateData_NoAlertConditions_NoAlertsTriggered() {
        long currentTime = System.currentTimeMillis();
        // Normal data
        patient.addRecord(120, "SystolicPressure", currentTime);
        patient.addRecord(80, "DiastolicPressure", currentTime);
        patient.addRecord(98, "Saturation", currentTime);
        for (int i = 0; i < 10; i++) {
            patient.addRecord(0.1, "ECG", currentTime - i * 1000);
        }
        alertGenerator.evaluateData(patient);
        assertNoAlertTriggered();
    }
}
