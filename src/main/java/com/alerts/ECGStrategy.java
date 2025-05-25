package com.alerts;

import com.data_management.Patient;
import com.data_management.PatientRecord;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ECGStrategy implements AlertStrategy {

    private final AlertFactory factory = new ECGAlertFactory();

    @Override
    public void checkAlert(Patient patient, AlertGenerator alertGenerator) {
        List<PatientRecord> allRecords = patient.getRecords(0, Long.MAX_VALUE);
        List<PatientRecord> ecgRecords = filterAndSortRecords(allRecords, "ECG");

        if (!ecgRecords.isEmpty()) {
            checkECGAbnormal(patient, ecgRecords, alertGenerator);
        }
    }

    private List<PatientRecord> filterAndSortRecords(List<PatientRecord> records, String recordType) {
        return records.stream()
                .filter(r -> r.getRecordType().equals(recordType))
                .sorted(Comparator.comparingLong(PatientRecord::getTimestamp))
                .collect(Collectors.toList());
    }

    private void checkECGAbnormal(Patient patient, List<PatientRecord> ecgRecords, AlertGenerator alertGenerator) {
        int windowSize = 10; 
        if (ecgRecords.size() < windowSize) return;
        String patientIdStr = String.valueOf(patient.getPatientId());

        List<Double> lastWindowValues = ecgRecords.subList(ecgRecords.size() - windowSize, ecgRecords.size())
                .stream().map(PatientRecord::getMeasurementValue).collect(Collectors.toList());
        
        double sum = lastWindowValues.stream().mapToDouble(Double::doubleValue).sum();
        double average = sum / windowSize;
        
        double sumOfSquares = lastWindowValues.stream().mapToDouble(val -> Math.pow(val - average, 2)).sum();
        double stdDev = Math.sqrt(sumOfSquares / windowSize);

        PatientRecord latestEcgRecord = ecgRecords.get(ecgRecords.size() - 1);
        double latestValue = latestEcgRecord.getMeasurementValue();
        
        // Example threshold: value is more than 3 standard deviations from the average
        // or an absolute abnormal value (e.g. > 2 mV or < -1mV for typical ECGs, simplified here)
        boolean abnormalPeak = Math.abs(latestValue - average) > 3 * stdDev && stdDev > 0.05; // Check stdDev to avoid issues with flat lines
        boolean absoluteAbnormal = latestValue > 2.0 || latestValue < -1.0; // Simplified absolute check

        if (abnormalPeak || absoluteAbnormal) {
            String reason = abnormalPeak ? "relative peak" : "absolute abnormal value";
            Alert alert = factory.createAlert(patientIdStr,
                    "Abnormal ECG data ("+reason+"): " + String.format("%.2f", latestValue) + " (Avg: " + String.format("%.2f", average) + ", StdDev: " + String.format("%.2f", stdDev) + ")",
                    latestEcgRecord.getTimestamp());
            alert = new PriorityAlertDecorator(alert, "High");
            alertGenerator.triggerAlert(alert);
        }
    }
}
