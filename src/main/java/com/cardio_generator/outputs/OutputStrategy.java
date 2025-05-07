package com.cardio_generator.outputs;

/**
 * Defines a strategy for outputting patient health data.
 * Implementations define how the data is delivered or stored.
 */
public interface OutputStrategy {

    /**
     * Outputs health data for a specific patient.
     *
     * @param patientId The ID of the patient.
     * @param timestamp The timestamp of the data in milliseconds.
     * @param label     A label describing the type of data.
     * @param data      The actual data to be output.
     */
    void output(int patientId, long timestamp, String label, String data);
}
