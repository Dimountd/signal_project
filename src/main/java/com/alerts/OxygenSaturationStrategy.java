package com.alerts;

import com.data_management.Patient;
import com.data_management.PatientRecord;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class OxygenSaturationStrategy implements AlertStrategy {

    private final AlertFactory factory = new BloodOxygenAlertFactory();

    @Override
    public void checkAlert(Patient patient, AlertGenerator alertGenerator) {
        List<PatientRecord> allRecords = patient.getRecords(0, Long.MAX_VALUE);
        List<PatientRecord> saturationRecords = filterAndSortRecords(allRecords, "Saturation");

        if (!saturationRecords.isEmpty()) {
            checkBloodSaturationLow(patient, saturationRecords.get(saturationRecords.size() - 1), alertGenerator);
            checkBloodSaturationRapidDrop(patient, saturationRecords, alertGenerator);
        }
    }

    private List<PatientRecord> filterAndSortRecords(List<PatientRecord> records, String recordType) {
        return records.stream()
                .filter(r -> r.getRecordType().equals(recordType))
                .sorted(Comparator.comparingLong(PatientRecord::getTimestamp))
                .collect(Collectors.toList());
    }

    private void checkBloodSaturationLow(Patient patient, PatientRecord saturationRecord, AlertGenerator alertGenerator) {
        double saturation = saturationRecord.getMeasurementValue();
        String patientIdStr = String.valueOf(patient.getPatientId());
        if (saturation < 92) {
            Alert alert = factory.createAlert(patientIdStr, "Low SpO2: " + saturation + "%", saturationRecord.getTimestamp());
            if (saturation < 88) { // Example: very low saturation gets high priority
                alert = new PriorityAlertDecorator(alert, "High");
            }
            alertGenerator.triggerAlert(alert);
        }
    }

    private void checkBloodSaturationRapidDrop(Patient patient, List<PatientRecord> saturationRecords, AlertGenerator alertGenerator) {
        if (saturationRecords.size() < 2) return;
        String patientIdStr = String.valueOf(patient.getPatientId());

        PatientRecord latestRecord = saturationRecords.get(saturationRecords.size() - 1);
        long tenMinutesInMillis = 10 * 60 * 1000;

        for (int i = saturationRecords.size() - 2; i >= 0; i--) {
            PatientRecord previousRecord = saturationRecords.get(i);
            if ((latestRecord.getTimestamp() - previousRecord.getTimestamp()) <= tenMinutesInMillis) {
                if (previousRecord.getMeasurementValue() - latestRecord.getMeasurementValue() >= 5) {
                    Alert alert = factory.createAlert(patientIdStr,
                            "Rapid SpO2 drop: " + previousRecord.getMeasurementValue() + "% to " + latestRecord.getMeasurementValue() + "%",
                            latestRecord.getTimestamp());
                    alert = new PriorityAlertDecorator(alert, "High"); // Rapid drops are usually high priority
                    alertGenerator.triggerAlert(alert);
                    return; 
                }
            } else {
                break;
            }
        }
    }
}
