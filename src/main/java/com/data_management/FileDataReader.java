package com.data_management;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Implementation of DataReader that reads patient data from a file.
 * Assumes each line in the file is formatted as:
 * patientId,measurementValue,measurementType,timestamp
 */
public class FileDataReader implements DataReader {

    private String directoryPath;

    /**
     * Constructs a FileDataReader for the specified file.
     * @param directoryPath the path to the output file containing patient data
     */
    public FileDataReader(String directoryPath) {
        this.directoryPath = directoryPath;
    }

    @Override
    public void readData(DataStorage dataStorage) throws IOException {
        Path dir = Paths.get(directoryPath);
        if (!Files.isDirectory(dir)) {
            System.err.println("Error: Provided path is not a directory or does not exist: " + directoryPath);
            return;
        }

        try (Stream<Path> paths = Files.list(dir)) {
            paths.filter(Files::isRegularFile).forEach(filePath -> {
                try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
                    String line;
                    // Example line: Patient ID: 1, Timestamp: 1714376789050, Label: WhiteBloodCells, Data: 100.0
                    // Regex to capture the necessary parts
                    Pattern pattern = Pattern.compile(
                        "Patient ID: (\\d+), Timestamp: (\\d+), Label: ([a-zA-Z]+), Data: ([\\d.]+)(?:%)?");
                    
                    while ((line = reader.readLine()) != null) {
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            try {
                                int patientId = Integer.parseInt(matcher.group(1));
                                long timestamp = Long.parseLong(matcher.group(2));
                                String recordType = matcher.group(3);
                                double measurementValue = Double.parseDouble(matcher.group(4));
                                
                                dataStorage.addPatientData(patientId, measurementValue, recordType, timestamp);
                            } catch (NumberFormatException e) {
                                System.err.println("Skipping malformed data line (number format error): " + line + " - " + e.getMessage());
                            }
                        } else {
                            // Handle lines that are not data, e.g., "Alert: triggered" or "Alert: resolved"
                            // These are currently skipped if they don't match the primary data pattern.
                            // If "Alert" type data needs to be stored differently, this is where it would be handled.
                            if (line.contains("Alert") && (line.contains("triggered") || line.contains("resolved"))) {
                                // System.out.println("Skipping Alert status line: " + line);
                            } else if (!line.trim().isEmpty()){
                                System.err.println("Skipping unrecognized line format: " + line);
                            }
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error reading file " + filePath + ": " + e.getMessage());
                }
            });
        }
    }
}
