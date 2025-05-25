package com.alerts;

import com.data_management.Patient;
import com.data_management.PatientRecord;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.crypto.Data;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * The {@code AlertGenerator} class is responsible for monitoring patient data
 * and generating alerts when certain predefined conditions are met. This class
 * now directly uses Patient objects to access their records.
 */
public class AlertGenerator {
    private List<Alert> triggeredAlerts; // Store triggered alerts

    /**
     * Constructs an {@code AlertGenerator}.
     * This constructor no longer requires DataStorage.
     */
    public AlertGenerator() { // Modified constructor
        this.triggeredAlerts = new ArrayList<>();
    }
    

    /**
     * Evaluates the specified patient's data to determine if any alert conditions
     * are met. If a condition is met, an alert is triggered.
     *
     * @param patient the patient data to evaluate for alert conditions
     */
    public void evaluateData(Patient patient) {
        if (patient == null) {
            System.err.println("Cannot evaluate data for a null patient.");
            return;
        }

        List<PatientRecord> allRecords = patient.getRecords(0, Long.MAX_VALUE); // Get all records

        List<PatientRecord> systolicRecords = filterAndSortRecords(allRecords, "SystolicPressure");
        List<PatientRecord> diastolicRecords = filterAndSortRecords(allRecords, "DiastolicPressure");
        List<PatientRecord> saturationRecords = filterAndSortRecords(allRecords, "Saturation");
        List<PatientRecord> ecgRecords = filterAndSortRecords(allRecords, "ECG");
        
        // Blood Pressure Alerts
        checkBloodPressureTrends(systolicRecords, diastolicRecords);
        if (!systolicRecords.isEmpty() && !diastolicRecords.isEmpty()) {
            // Assuming latest systolic and diastolic are relevant for critical check
            // This might need more sophisticated pairing if timestamps can mismatch significantly
            PatientRecord latestSystolic = systolicRecords.get(systolicRecords.size() - 1);
            PatientRecord latestDiastolic = findClosestRecord(diastolicRecords, latestSystolic.getTimestamp());
            if (latestDiastolic != null) {
                checkBloodPressureCritical(latestSystolic, latestDiastolic);
                
                // Combined Alert: Hypotensive Hypoxemia
                if (!saturationRecords.isEmpty()) {
                    PatientRecord latestSaturation = saturationRecords.get(saturationRecords.size() - 1);
                    checkHypotensiveHypoxemia(latestSystolic, latestSaturation);
                }
            }
        }


        // Blood Saturation Alerts
        if (!saturationRecords.isEmpty()) {
            checkBloodSaturationLow(saturationRecords.get(saturationRecords.size() - 1));
            checkBloodSaturationRapidDrop(saturationRecords);
        }

        // ECG Alerts
        if (!ecgRecords.isEmpty()) {
            checkECGAbnormal(ecgRecords);
        }


    }

    private List<PatientRecord> filterAndSortRecords(List<PatientRecord> records, String recordType) {
        return records.stream()
                .filter(r -> r.getRecordType().equals(recordType))
                .sorted(Comparator.comparingLong(PatientRecord::getTimestamp))
                .collect(Collectors.toList());
    }
    
    private PatientRecord findClosestRecord(List<PatientRecord> records, long targetTimestamp) {
        if (records.isEmpty()) return null;
        return records.stream()
                .min(Comparator.comparingLong(r -> Math.abs(r.getTimestamp() - targetTimestamp)))
                .orElse(null);
    }


    private void checkBloodPressureTrends(List<PatientRecord> systolicRecords, List<PatientRecord> diastolicRecords) {
        if (systolicRecords.isEmpty() && diastolicRecords.isEmpty()) {
            return;
        }
        // Assuming all records in a list belong to the same patient.
        // Get patientId from the first record if list is not empty.
        int patientId = -1; 
        if (!systolicRecords.isEmpty()) {
            patientId = systolicRecords.get(0).getPatientId();
        } else if (!diastolicRecords.isEmpty()) {
            patientId = diastolicRecords.get(0).getPatientId();
        }
        if (patientId == -1) return; // Should not happen if lists are not empty

        // Systolic trend (last 3 readings)
        if (systolicRecords.size() >= 3) {
            List<Double> last3SystolicValues = systolicRecords.subList(Math.max(0, systolicRecords.size() - 3), systolicRecords.size())
                    .stream().map(PatientRecord::getMeasurementValue).collect(Collectors.toList());
            String systolicTrend = getTrendType(last3SystolicValues, 10);
            if (systolicTrend != null) {
                triggerAlert(new Alert(String.valueOf(patientId), "Systolic BP " + systolicTrend + " trend detected.", System.currentTimeMillis()));
            }
        }

        // Diastolic trend (last 3 readings)
        if (diastolicRecords.size() >= 3) {
            List<Double> last3DiastolicValues = diastolicRecords.subList(Math.max(0, diastolicRecords.size() - 3), diastolicRecords.size())
                    .stream().map(PatientRecord::getMeasurementValue).collect(Collectors.toList());
            String diastolicTrend = getTrendType(last3DiastolicValues, 10);
            if (diastolicTrend != null) {
                triggerAlert(new Alert(String.valueOf(patientId), "Diastolic BP " + diastolicTrend + " trend detected.", System.currentTimeMillis()));
            }
        }
    }

    /**
     * Determines if there is a consistent trend in the list of values.
     * @param values List of measurement values.
     * @param thresholdDelta The minimum change between consecutive values to be considered part of a trend.
     * @return "increasing", "decreasing", or null if no consistent trend.
     */
    private String getTrendType(List<Double> values, double thresholdDelta) {
        if (values.size() < 3) return null;

        double val1 = values.get(values.size() - 3);
        double val2 = values.get(values.size() - 2);
        double val3 = values.get(values.size() - 1);

        boolean increasingTrend = (val2 - val1 > thresholdDelta) && (val3 - val2 > thresholdDelta);
        boolean decreasingTrend = (val1 - val2 > thresholdDelta) && (val2 - val3 > thresholdDelta);

        if (increasingTrend) return "increasing";
        if (decreasingTrend) return "decreasing";
        return null;
    }

    private void checkBloodPressureCritical(PatientRecord systolicRecord, PatientRecord diastolicRecord) {
        double systolic = systolicRecord.getMeasurementValue();
        double diastolic = diastolicRecord.getMeasurementValue();
        long timestamp = Math.max(systolicRecord.getTimestamp(), diastolicRecord.getTimestamp());
        int patientId = systolicRecord.getPatientId(); // Get patientId from record

        if (systolic > 180 || systolic < 90 || diastolic > 120 || diastolic < 60) {
            triggerAlert(new Alert(String.valueOf(patientId), "Critical BP: " + systolic + "/" + diastolic + " mmHg", timestamp));
        }
    }

    private void checkBloodSaturationLow(PatientRecord saturationRecord) {
        double saturation = saturationRecord.getMeasurementValue();
        int patientId = saturationRecord.getPatientId(); // Get patientId from record
        if (saturation < 92) {
            triggerAlert(new Alert(String.valueOf(patientId), "Low blood oxygen saturation: " + saturation + "%", saturationRecord.getTimestamp()));
        }
    }

    private void checkBloodSaturationRapidDrop(List<PatientRecord> saturationRecords) {
        if (saturationRecords.size() < 2) return;
        int patientId = saturationRecords.get(0).getPatientId(); // Get patientId from first record

        PatientRecord latestRecord = saturationRecords.get(saturationRecords.size() - 1);
        long tenMinutesInMillis = 10 * 60 * 1000;

        for (int i = saturationRecords.size() - 2; i >= 0; i--) {
            PatientRecord previousRecord = saturationRecords.get(i);
            if ((latestRecord.getTimestamp() - previousRecord.getTimestamp()) <= tenMinutesInMillis) {
                if (previousRecord.getMeasurementValue() - latestRecord.getMeasurementValue() >= 5) {
                    triggerAlert(new Alert(String.valueOf(patientId),
                            "Rapid drop in SpO2: " + previousRecord.getMeasurementValue() + "% to " + latestRecord.getMeasurementValue() + "% within 10 minutes",
                            latestRecord.getTimestamp()));
                    return; // Alert once for the rapid drop condition
                }
            } else {
                // Records are sorted by time, so no need to check further back if outside 10 min window
                break; 
            }
        }
    }

    private void checkHypotensiveHypoxemia(PatientRecord bpSystolicRecord, PatientRecord satRecord) {
        double systolic = bpSystolicRecord.getMeasurementValue();
        double saturation = satRecord.getMeasurementValue();
        long timestamp = Math.max(bpSystolicRecord.getTimestamp(), satRecord.getTimestamp());
        int patientId = bpSystolicRecord.getPatientId(); // Get patientId from record

        if (systolic < 90 && saturation < 92) {
            triggerAlert(new Alert(String.valueOf(patientId), "Hypotensive Hypoxemia: BP Systolic " + systolic + ", SpO2 " + saturation + "%", timestamp));
        }
    }

    private void checkECGAbnormal(List<PatientRecord> ecgRecords) {
        // This is a simplified check. Real ECG analysis is complex.
        // Example: Check for unusually high or low heart rate based on peak intervals, or abnormal peak values.
        // Here, we'll adapt the previous logic of checking for a peak significantly above average.
        int windowSize = 10; // Number of recent readings to consider for average
        if (ecgRecords.size() < windowSize) return;
        int patientId = ecgRecords.get(0).getPatientId(); // Get patientId from first record

        List<Double> lastWindowValues = ecgRecords.subList(ecgRecords.size() - windowSize, ecgRecords.size())
                .stream().map(PatientRecord::getMeasurementValue).collect(Collectors.toList());
        
        double sum = 0;
        for (double val : lastWindowValues) {
            sum += val;
        }
        double average = sum / windowSize;
        
        double sumOfSquares = 0;
        for (double val : lastWindowValues) {
            sumOfSquares += Math.pow(val - average, 2);
        }
        double stdDev = Math.sqrt(sumOfSquares / windowSize);

        PatientRecord latestEcgRecord = ecgRecords.get(ecgRecords.size() - 1);
        double latestValue = latestEcgRecord.getMeasurementValue();
        // Example threshold: 2 standard deviations above average
        double threshold = average + (2 * stdDev); 

        if (latestValue > threshold) {
            triggerAlert(new Alert(String.valueOf(patientId),
                    "ECG Abnormal Peak: " + String.format("%.2f", latestValue) + " (Avg: " + String.format("%.2f", average) + ", StdDev: " + String.format("%.2f", stdDev) + ")",
                    latestEcgRecord.getTimestamp()));
        }
    }


    /**
     * Triggers an alert.
     * In a real system, this would likely involve sending the alert to a notification system.
     * For this example, it prints to the console.
     *
     * @param alert The alert to trigger.
     */
    private void triggerAlert(Alert alert) {
        this.triggeredAlerts.add(alert); // Add to list for testing
        System.out.println("ALERT TRIGGERED: Patient ID " + alert.getPatientId() +
                ", Condition: " + alert.getCondition() +
                ", Timestamp: " + alert.getTimestamp());
    }

    /**
     * Retrieves the list of alerts triggered during evaluation.
     * Primarily for testing purposes.
     * @return A list of triggered Alert objects.
     */
    public List<Alert> getAlerts() {
        return new ArrayList<>(triggeredAlerts); // Return a copy
    }

    /**
     * Clears the list of triggered alerts.
     * Primarily for resetting state between tests.
     */
    public void clearAlerts() {
        this.triggeredAlerts.clear();
    }

    // The checkAlert method that depended on AlertStrategy has been removed
    // as AlertStrategy is not fully defined and initialized in this context,
    // and evaluateData is the primary focus of this refactoring.
}
