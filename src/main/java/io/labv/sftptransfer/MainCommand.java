package io.labv.sftptransfer;

import io.labv.sftptransfer.config.Config;
import io.labv.sftptransfer.config.ConfigLoader;
import io.labv.sftptransfer.config.ConfigValidator;
import io.labv.sftptransfer.core.MonitorTask;
import io.labv.sftptransfer.util.LoggerInitializer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;


@Command(name = "labv-sftp-transfer",
        mixinStandardHelpOptions = true,
        version = "labv-sftp-transfer 1.0",
        description = "Transfers files from local folders to a remote SFTP server based on a YAML configuration file.")
public class MainCommand implements Callable<Integer> {
    
    @Option(names = { "--config" },
            description = "Path to YAML configuration file. Default: src/main/resources/config.yaml")
    private File configFile = new File("src/main/resources/config.yaml");
    
    @Option(names = { "--dry-run" },
            description = "Simulate the transfer without uploading or modifying any files.")
    private boolean dryRun = false;
    
    @Option(names = "--log-level",
            description = "Set the logging level (e.g., INFO, WARNING, FINE)")
    private String logLevel = null;
    
    @Override
    public Integer call() {
        
        printAsciiLogo();
        
        if (!configFile.exists()) {
            System.err.println("Configuration file not found: " + configFile.getAbsolutePath());
            return 1;
        }
        
        try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
            Config config = ConfigLoader.load(configFile);
            ConfigValidator.validate(config);
            
            Logger logger = LoggerInitializer.init(config.getLog());
            if (logLevel != null) {
                Level level = Level.parse(logLevel.toUpperCase());
                logger.setLevel(level);
                for (Handler handler : logger.getHandlers()) {
                    handler.setLevel(level);
                }
            }
            logger.info("Configuration loaded. Starting monitor...");
            
            MonitorTask task = new MonitorTask(config, logger, dryRun);
            
            executor.scheduleAtFixedRate(task, 0, config.getIntervalSeconds(), TimeUnit.SECONDS);
            logger.info("labv-sftp-transfer started. Press Ctrl+C to exit.");
            Thread.currentThread().join();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
        
        return 0;
    }
    
    private static void printAsciiLogo() {
        
        try (InputStream is = MainCommand.class.getResourceAsStream("/labv-logo.txt")) {
            assert is != null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                reader.lines().forEach(System.out::println);
            }
        } catch (Exception e) {
            // Fallback if logo is missing
            System.out.println("LabV - SFTP Transfer");
        }
    }
    
    public static void main(String[] args) {
        
        int exitCode = new CommandLine(new MainCommand()).execute(args);
        System.exit(exitCode);
    }
    
}
