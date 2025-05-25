package com.alerts;

import com.data_management.Patient;
import com.data_management.PatientRecord;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class BloodPressureStrategy implements AlertStrategy {

    private final AlertFactory factory = new BloodPressureAlertFactory();

    @Override
    public void checkAlert(Patient patient, AlertGenerator alertGenerator) {
        List<PatientRecord> allRecords = patient.getRecords(0, Long.MAX_VALUE);

        List<PatientRecord> systolicRecords = filterAndSortRecords(allRecords, "SystolicPressure");
        List<PatientRecord> diastolicRecords = filterAndSortRecords(allRecords, "DiastolicPressure");

        checkBloodPressureTrends(patient, systolicRecords, diastolicRecords, alertGenerator);

        if (!systolicRecords.isEmpty() && !diastolicRecords.isEmpty()) {
            PatientRecord latestSystolic = systolicRecords.get(systolicRecords.size() - 1);
            PatientRecord latestDiastolic = findClosestRecord(diastolicRecords, latestSystolic.getTimestamp());

            if (latestDiastolic != null) {
                checkBloodPressureCritical(patient, latestSystolic, latestDiastolic, alertGenerator);

                // Hypotensive Hypoxemia check requires saturation data, handle in a combined strategy or ensure data availability
                // For now, this strategy focuses only on BP. HypotensiveHypoxemia could be its own strategy
                // or AlertGenerator could orchestrate strategies that depend on each other.
                // Let's assume a separate strategy or a more complex orchestration for combined alerts.
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

    private void checkBloodPressureTrends(Patient patient, List<PatientRecord> systolicRecords, List<PatientRecord> diastolicRecords, AlertGenerator alertGenerator) {
        String patientIdStr = String.valueOf(patient.getPatientId());

        // Systolic trend
        if (systolicRecords.size() >= 3) {
            List<Double> last3SystolicValues = systolicRecords.subList(systolicRecords.size() - 3, systolicRecords.size())
                    .stream().map(PatientRecord::getMeasurementValue).collect(Collectors.toList());
            String systolicTrend = getTrendType(last3SystolicValues, 10);
            if (systolicTrend != null) {
                alertGenerator.triggerAlert(factory.createAlert(patientIdStr, "Systolic BP " + systolicTrend + " trend", System.currentTimeMillis()));
            }
        }

        // Diastolic trend
        if (diastolicRecords.size() >= 3) {
            List<Double> last3DiastolicValues = diastolicRecords.subList(diastolicRecords.size() - 3, diastolicRecords.size())
                    .stream().map(PatientRecord::getMeasurementValue).collect(Collectors.toList());
            String diastolicTrend = getTrendType(last3DiastolicValues, 10);
            if (diastolicTrend != null) {
                alertGenerator.triggerAlert(factory.createAlert(patientIdStr, "Diastolic BP " + diastolicTrend + " trend", System.currentTimeMillis()));
            }
        }
    }

    private String getTrendType(List<Double> values, double thresholdDelta) {
        if (values.size() < 3) return null;
        double val1 = values.get(0);
        double val2 = values.get(1);
        double val3 = values.get(2);

        boolean increasingTrend = (val2 - val1 > thresholdDelta) && (val3 - val2 > thresholdDelta);
        boolean decreasingTrend = (val1 - val2 > thresholdDelta) && (val2 - val3 > thresholdDelta);

        if (increasingTrend) return "increasing";
        if (decreasingTrend) return "decreasing";
        return null;
    }

    private void checkBloodPressureCritical(Patient patient, PatientRecord systolicRecord, PatientRecord diastolicRecord, AlertGenerator alertGenerator) {
        double systolic = systolicRecord.getMeasurementValue();
        double diastolic = diastolicRecord.getMeasurementValue();
        long timestamp = Math.max(systolicRecord.getTimestamp(), diastolicRecord.getTimestamp());
        String patientIdStr = String.valueOf(patient.getPatientId());

        if (systolic > 180 || systolic < 90 || diastolic > 120 || diastolic < 60) {
            Alert alert = factory.createAlert(patientIdStr, "Critical BP: " + systolic + "/" + diastolic + " mmHg", timestamp);
            if (systolic > 180 || diastolic > 120) {
                 alert = new PriorityAlertDecorator(alert, "High");
            }
            alertGenerator.triggerAlert(alert);
        }
    }
}
