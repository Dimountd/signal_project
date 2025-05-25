package com.alerts;

import com.data_management.Patient;
import com.data_management.PatientRecord;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class HypotensiveHypoxemiaStrategy implements AlertStrategy {

    // This strategy might use a generic factory or a specific one if HypotensiveHypoxemiaAlert is a distinct type
    private final AlertFactory bloodPressureFactory = new BloodPressureAlertFactory(); // Or a combined alert factory
    private final AlertFactory bloodOxygenFactory = new BloodOxygenAlertFactory();


    @Override
    public void checkAlert(Patient patient, AlertGenerator alertGenerator) {
        List<PatientRecord> allRecords = patient.getRecords(0, Long.MAX_VALUE);

        List<PatientRecord> systolicRecords = filterAndSortRecords(allRecords, "SystolicPressure");
        List<PatientRecord> saturationRecords = filterAndSortRecords(allRecords, "Saturation");

        if (!systolicRecords.isEmpty() && !saturationRecords.isEmpty()) {
            PatientRecord latestSystolic = systolicRecords.get(systolicRecords.size() - 1);
            // Find the saturation record closest in time to the latest systolic record
            PatientRecord relevantSaturation = findClosestRecord(saturationRecords, latestSystolic.getTimestamp());

            if (relevantSaturation != null) {
                // Check if the records are reasonably close in time (e.g., within 1 minute)
                long timeDifference = Math.abs(latestSystolic.getTimestamp() - relevantSaturation.getTimestamp());
                if (timeDifference <= 60 * 1000) { // 1 minute
                    checkCondition(patient, latestSystolic, relevantSaturation, alertGenerator);
                }
            }
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

    private void checkCondition(Patient patient, PatientRecord bpSystolicRecord, PatientRecord satRecord, AlertGenerator alertGenerator) {
        double systolic = bpSystolicRecord.getMeasurementValue();
        double saturation = satRecord.getMeasurementValue();
        long timestamp = Math.max(bpSystolicRecord.getTimestamp(), satRecord.getTimestamp());
        String patientIdStr = String.valueOf(patient.getPatientId());

        if (systolic < 90 && saturation < 92) {
            // For combined alerts, you might want a more generic alert type or a specific one.
            // Using BloodPressureFactory as an example, but a "CombinedAlertFactory" might be better.
            Alert alert = bloodPressureFactory.createAlert(patientIdStr,
                    "Hypotensive Hypoxemia: BP Systolic " + systolic + ", SpO2 " + saturation + "%", timestamp);
            alert = new PriorityAlertDecorator(alert, "Urgent"); // This is a critical combined condition
            alertGenerator.triggerAlert(alert);
        }
    }
}
