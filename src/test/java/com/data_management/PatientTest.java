package com.data_management;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class PatientTest {

    private Patient patient;

    @BeforeEach
    void setUp() {
        patient = new Patient(1); // Patient ID 1
    }

    @Test
    void testAddRecord_AddsRecordToPatient() {
        patient.addRecord(120.0, "SystolicPressure", 1700000000000L);
        List<PatientRecord> records = patient.getRecords(0L, Long.MAX_VALUE);
        assertEquals(1, records.size());
        PatientRecord record = records.get(0);
        assertEquals(1, record.getPatientId());
        assertEquals(120.0, record.getMeasurementValue());
        assertEquals("SystolicPressure", record.getRecordType());
        assertEquals(1700000000000L, record.getTimestamp());
    }

    @Test
    void testGetRecords_FiltersByTimeRangeCorrectly() {
        // Add records with various timestamps
        patient.addRecord(100.0, "HeartRate", 1000L); // In range
        patient.addRecord(101.0, "HeartRate", 1500L); // In range (boundary)
        patient.addRecord(102.0, "HeartRate", 2000L); // In range (boundary)
        patient.addRecord(103.0, "HeartRate", 2500L); // In range
        patient.addRecord(99.0, "HeartRate", 500L);   // Out of range (before)
        patient.addRecord(104.0, "HeartRate", 3000L); // Out of range (after)

        List<PatientRecord> filteredRecords = patient.getRecords(1500L, 2000L);
        assertEquals(2, filteredRecords.size());

        // Check if correct records are retrieved (order might depend on insertion, but values should match)
        assertTrue(filteredRecords.stream().anyMatch(r -> r.getMeasurementValue() == 101.0 && r.getTimestamp() == 1500L));
        assertTrue(filteredRecords.stream().anyMatch(r -> r.getMeasurementValue() == 102.0 && r.getTimestamp() == 2000L));
    }

    @Test
    void testGetRecords_NoRecordsInRange() {
        patient.addRecord(100.0, "HeartRate", 1000L);
        patient.addRecord(101.0, "HeartRate", 2000L);

        List<PatientRecord> filteredRecords = patient.getRecords(1200L, 1800L);
        assertTrue(filteredRecords.isEmpty());
    }

    @Test
    void testGetRecords_AllRecordsInRange() {
        patient.addRecord(100.0, "HeartRate", 1000L);
        patient.addRecord(101.0, "HeartRate", 1500L);
        patient.addRecord(102.0, "HeartRate", 2000L);

        List<PatientRecord> filteredRecords = patient.getRecords(500L, 2500L);
        assertEquals(3, filteredRecords.size());
    }

    @Test
    void testGetRecords_EmptyPatientRecords() {
        List<PatientRecord> filteredRecords = patient.getRecords(0L, Long.MAX_VALUE);
        assertTrue(filteredRecords.isEmpty());
    }

    @Test
    void testGetRecords_ExactTimestampMatch() {
        patient.addRecord(100.0, "HeartRate", 1000L);
        List<PatientRecord> filteredRecords = patient.getRecords(1000L, 1000L);
        assertEquals(1, filteredRecords.size());
        assertEquals(100.0, filteredRecords.get(0).getMeasurementValue());
    }
}
