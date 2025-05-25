package com.cardio_generator;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Corrected import for AlertGenerator from its own package
import com.cardio_generator.generators.AlertGenerator;

import com.cardio_generator.generators.BloodPressureDataGenerator;
import com.cardio_generator.generators.BloodSaturationDataGenerator;
import com.cardio_generator.generators.BloodLevelsDataGenerator;
import com.cardio_generator.generators.ECGDataGenerator;
import com.cardio_generator.outputs.ConsoleOutputStrategy;
import com.cardio_generator.outputs.FileOutputStrategy;
import com.cardio_generator.outputs.OutputStrategy;
import com.cardio_generator.outputs.TCPOutputStrategy;
import com.cardio_generator.outputs.WebSocketOutputStrategy;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Simulates health data for many patients.
 * Has manageable output strategies and patient counts.
 * Implemented as a Singleton.
 */
public class HealthDataSimulator {

    private static HealthDataSimulator instance;
    private int patientCount = 50; // Default number of patients
    private ScheduledExecutorService scheduler;
    private OutputStrategy outputStrategy = new ConsoleOutputStrategy(); // Default output strategy
    private final Random random = new Random();

    /**
     * Private constructor to prevent instantiation from outside.
     */
    private HealthDataSimulator() {
        // Initialization logic that might have been in a static block or main before running tasks
    }

    /**
     * Returns the singleton instance of HealthDataSimulator.
     *
     * @return the singleton instance
     */
    public static synchronized HealthDataSimulator getInstance() {
        if (instance == null) {
            instance = new HealthDataSimulator();
        }
        return instance;
    }

    /**
     * Initializes and starts the simulation.
     *
     * @param args Command-line arguments to configure the simulator.
     * @throws IOException If an error occurs while creating output directories.
     */
    public void start(String[] args) throws IOException {
        parseArguments(args); // Use instance fields
        this.scheduler = Executors.newScheduledThreadPool(this.patientCount * 4); // Use instance field
        List<Integer> patientIds = initializePatientIds(this.patientCount); // Use instance field
        Collections.shuffle(patientIds); // Randomize the order of patient IDs
        scheduleTasksForPatients(patientIds);
    }


    /**
     * Entry point for the health data simulator.
     *
     * @param args Command-line arguments to configure the simulator.
     * @throws IOException If an error occurs while creating output directories.
     */
    public static void main(String[] args) throws IOException {
        HealthDataSimulator simulator = HealthDataSimulator.getInstance();
        simulator.start(args);
    }

    /**
     * Parses command-line arguments to configure the simulator.
     *
     * @param args The command-line arguments.
     * @throws IOException If an error occurs while setting up.
     */
    private void parseArguments(String[] args) throws IOException { // Made non-static
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                    printHelp();
                    System.exit(0);
                    break;
                case "--patient-count":
                    if (i + 1 < args.length) {
                        try {
                            this.patientCount = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            System.err
                                    .println("Error: Invalid number of patients. Using default value: " + this.patientCount);
                        }
                    }
                    break;
                case "--output":
                    if (i + 1 < args.length) {
                        String outputArg = args[++i];
                        if (outputArg.equals("console")) {
                            this.outputStrategy = new ConsoleOutputStrategy();
                        } else if (outputArg.startsWith("file:")) {
                            String baseDirectory = outputArg.substring(5);
                            Path outputPath = Paths.get(baseDirectory);
                            if (!Files.exists(outputPath)) {
                                Files.createDirectories(outputPath);
                            }
                            this.outputStrategy = new FileOutputStrategy(baseDirectory);
                        } else if (outputArg.startsWith("websocket:")) {
                            try {
                                int port = Integer.parseInt(outputArg.substring(10));
                                this.outputStrategy = new WebSocketOutputStrategy(port);
                                System.out.println("WebSocket output will be on port: " + port);
                            } catch (NumberFormatException e) {
                                System.err.println(
                                        "Invalid port for WebSocket output. Please specify a valid port number.");
                            }
                        } else if (outputArg.startsWith("tcp:")) {
                            try {
                                int port = Integer.parseInt(outputArg.substring(4));
                                this.outputStrategy = new TCPOutputStrategy(port);
                                System.out.println("TCP socket output will be on port: " + port);
                            } catch (NumberFormatException e) {
                                System.err.println("Invalid port for TCP output. Please specify a valid port number.");
                            }
                        } else {
                            System.err.println("Unknown output type. Using default (console).");
                        }
                    }
                    break;
                default:
                    System.err.println("Unknown option '" + args[i] + "'");
                    printHelp();
                    System.exit(1);
            }
        }
    }

    /**
     * Prints the help message.
     */
    private static void printHelp() { // Can remain static as it doesn't use instance members
        System.out.println("Usage: java HealthDataSimulator [options]");
        System.out.println("Options:");
        System.out.println("  -h                       Show help and exit.");
        System.out.println(
                "  --patient-count <count>  Specify the number of patients to simulate data for (default: 50).");
        System.out.println("  --output <type>          Define the output method. Options are:");
        System.out.println("                             'console' for console output,");
        System.out.println("                             'file:<directory>' for file output,");
        System.out.println("                             'websocket:<port>' for WebSocket output,");
        System.out.println("                             'tcp:<port>' for TCP socket output.");
        System.out.println("Example:");
        System.out.println("  java HealthDataSimulator --patient-count 100 --output websocket:8080");
        System.out.println(
                "  This command simulates data for 100 patients and sends the output to WebSocket clients connected to port 8080.");
    }

    /**
     * Initializes a list of patient IDs.
     *
     * @param patientCount The number of patients to simulate.
     * @return A list of patient IDs.
     */
    private List<Integer> initializePatientIds(int patientCount) { // Made non-static
        List<Integer> patientIds = new ArrayList<>();
        for (int i = 1; i <= patientCount; i++) {
            patientIds.add(i);
        }
        return patientIds;
    }

    /**
     * Schedules data generation tasks for all patients.
     *
     * @param patientIds The list of patient IDs.
     */
    private void scheduleTasksForPatients(List<Integer> patientIds) { // Made non-static
        ECGDataGenerator ecgDataGenerator = new ECGDataGenerator(this.patientCount);
        BloodSaturationDataGenerator bloodSaturationDataGenerator = new BloodSaturationDataGenerator(this.patientCount);
        BloodPressureDataGenerator bloodPressureDataGenerator = new BloodPressureDataGenerator(this.patientCount);
        BloodLevelsDataGenerator bloodLevelsDataGenerator = new BloodLevelsDataGenerator(this.patientCount);
        AlertGenerator alertGenerator = new AlertGenerator(this.patientCount); // This is com.cardio_generator.generators.AlertGenerator

        for (int patientId : patientIds) {
            scheduleTask(() -> ecgDataGenerator.generate(patientId, this.outputStrategy), 1, TimeUnit.SECONDS);
            scheduleTask(() -> bloodSaturationDataGenerator.generate(patientId, this.outputStrategy), 1, TimeUnit.SECONDS);
            scheduleTask(() -> bloodPressureDataGenerator.generate(patientId, this.outputStrategy), 1, TimeUnit.MINUTES);
            scheduleTask(() -> bloodLevelsDataGenerator.generate(patientId, this.outputStrategy), 2, TimeUnit.MINUTES);
            scheduleTask(() -> alertGenerator.generate(patientId, this.outputStrategy), 20, TimeUnit.SECONDS);
        }
    }

    /**
     * Schedules a task to run at a fixed rate with a random delay.
     *
     * @param task     The task to schedule.
     * @param period   The period between executions.
     * @param timeUnit The time unit of the period.
     */
    private void scheduleTask(Runnable task, long period, TimeUnit timeUnit) { // Made non-static
        this.scheduler.scheduleAtFixedRate(task, this.random.nextInt(5), period, timeUnit);
    }
}
