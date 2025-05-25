package com.data_management;

import java.io.IOException;

/**
 * Interface for reading patient data from a source.
 */
public interface DataReader {
    /**
     * Reads data from the source and stores it in the provided DataStorage.
     *
     * @param dataStorage The DataStorage instance to populate with patient data.
     * @throws IOException If an error occurs during data reading.
     */
    void readData(DataStorage dataStorage) throws IOException;
}
