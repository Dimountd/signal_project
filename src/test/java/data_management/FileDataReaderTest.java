package data_management;

import static org.junit.jupiter.api.Assertions.*;

import com.data_management.DataReader;
import com.data_management.DataStorage;
import com.data_management.FileDataReader;
import com.data_management.PatientRecord;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class FileDataReaderTest {

    @TempDir
    Path tempDir; // JUnit 5 temporary directory

    private DataStorage storage;
    private DataReader reader;

    @BeforeEach
    void setUp() {
        storage = new DataStorage();
    }

    private File createTestFile(String fileName, String... lines) throws IOException {
        Path filePath = tempDir.resolve(fileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
        return filePath.toFile();
    }

    @Test
    void testReadData_ValidFile_CorrectlyParsesAndStoresData() throws IOException {
        createTestFile("testData1.txt",
                "Patient ID: 1, Timestamp: 1000, Label: ECG, Data: 0.5",
                "Patient ID: 1, Timestamp: 2000, Label: Temperature, Data: 36.6",
                "Patient ID: 2, Timestamp: 1500, Label: ECG, Data: 0.8");

        reader = new FileDataReader(tempDir.toString());
        reader.readData(storage);

        List<PatientRecord> patient1Records = storage.getRecords(1, 0, 3000);
        assertEquals(2, patient1Records.size());
        assertEquals(0.5, patient1Records.get(0).getMeasurementValue());
        assertEquals("ECG", patient1Records.get(0).getRecordType());

        List<PatientRecord> patient2Records = storage.getRecords(2, 0, 3000);
        assertEquals(1, patient2Records.size());
        assertEquals(0.8, patient2Records.get(0).getMeasurementValue());
    }

    @Test
    void testReadData_EmptyFile_NoDataStored() throws IOException {
        createTestFile("emptyData.txt"); // Empty file

        reader = new FileDataReader(tempDir.toString());
        reader.readData(storage);

        assertTrue(storage.getAllPatients().isEmpty());
    }

    @Test
    void testReadData_MalformedData_SkipsMalformedEntries() throws IOException {
        createTestFile("malformedData.txt",
                "Patient ID: 1, Timestamp: 1000, Label: ECG, Data: 0.5",
                "This is a malformed line",
                "Patient ID: 2, Timestamp: 1500, Label: ECG, Data: 0.8",
                "Patient ID: 3, Timestamp: 2000, Label: Alert, Data: triggered", // Should be skipped by current parser
                "Patient ID: 4, Timestamp: invalid, Label: ECG, Data: 0.9");


        reader = new FileDataReader(tempDir.toString());
        reader.readData(storage);

        List<PatientRecord> patient1Records = storage.getRecords(1, 0, 3000);
        assertEquals(1, patient1Records.size());

        List<PatientRecord> patient2Records = storage.getRecords(2, 0, 3000);
        assertEquals(1, patient2Records.size());
        
        assertNull(storage.getRecords(3, 0, 3000).stream().findFirst().orElse(null)); // Patient 3 data should be skipped
        assertNull(storage.getRecords(4, 0, 3000).stream().findFirst().orElse(null)); // Patient 4 data should be skipped
    }

    @Test
    void testReadData_DirectoryWithMultipleFiles() throws IOException {
        createTestFile("testDataFile1.txt", "Patient ID: 1, Timestamp: 1000, Label: ECG, Data: 0.1");
        createTestFile("testDataFile2.txt", "Patient ID: 1, Timestamp: 2000, Label: ECG, Data: 0.2");

        reader = new FileDataReader(tempDir.toString());
        reader.readData(storage);

        List<PatientRecord> patient1Records = storage.getRecords(1, 0, 3000);
        assertEquals(2, patient1Records.size());
    }

    @Test
    void testReadData_NonExistentDirectory() {
        reader = new FileDataReader(tempDir.resolve("nonExistentDir").toString());
        // Should not throw an exception here, but log an error, and storage should be empty
        assertDoesNotThrow(() -> reader.readData(storage));
        assertTrue(storage.getAllPatients().isEmpty());
    }
     @Test
    void testReadData_SaturationWithPercentageSign() throws IOException {
        createTestFile("saturationData.txt",
                "Patient ID: 1, Timestamp: 1000, Label: Saturation, Data: 95.0%");

        reader = new FileDataReader(tempDir.toString());
        reader.readData(storage);

        List<PatientRecord> patient1Records = storage.getRecords(1, 0, 3000);
        assertEquals(1, patient1Records.size());
        assertEquals(95.0, patient1Records.get(0).getMeasurementValue());
        assertEquals("Saturation", patient1Records.get(0).getRecordType());
    }
}
