package com.cardio_generator.generators;

import com.cardio_generator.outputs.OutputStrategy;

/**
 * Defines a generator for patient health data.
 * Implementatioons generate data for a given patient and output it using a specified strategy.
 */
public interface PatientDataGenerator {

    /**
     * Generates health data for a patient.
     *
     * @param patientId      The ID of the patient the data is generated for.
     * @param outputStrategy The strategy used to output the generated data.
     */
    void generate(int patientId, OutputStrategy outputStrategy);
}
