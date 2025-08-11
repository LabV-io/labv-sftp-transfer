package io.labv.sftptransfer;

import io.labv.sftptransfer.config.Config;
import io.labv.sftptransfer.config.ConfigLoader;
import io.labv.sftptransfer.config.ConfigValidator;
import io.labv.sftptransfer.core.FolderMonitorTask;
import io.labv.sftptransfer.core.SftpUploader;
import io.labv.sftptransfer.util.LoggerInitializer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

@Command(
        name = "labv-sftp-transfer",
        mixinStandardHelpOptions = true,
        version = "labv-sftp-transfer 1.0",
        description = "Transfers files from local folders to a remote SFTP server based on a YAML configuration file."
)
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
        
        System.out.println("LabV SFTP Transfer");
        System.out.println("==================");
        System.out.println();
        
        if (!configFile.exists()) {
            System.err.println("Configuration file not found: " + configFile.getAbsolutePath());
            return 1;
        }
        
        try {
            // 1) Load + validate
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
            logger.info("Configuration loaded.");
            
            final int globalInterval = config.getIntervalSeconds();
            final List<Config.FolderConfig> folders = config.getFolders();
            
            // 2) Run-once mode if global == -1 (per-folder intervals are ignored)
            if (globalInterval == -1) {
                logger.info("Running in single-run mode for all folders (intervalSeconds = -1).");
                SftpUploader uploader = new SftpUploader(config, logger, dryRun);
                for (Config.FolderConfig f : folders) {
                    new FolderMonitorTask(f, uploader, logger).run();
                }
                logger.info("Single-run completed for all folders. Exiting.");
                return 0;
            }
            
            // 3) Periodic scheduling per folder
            ScheduledExecutorService executor =
                    Executors.newScheduledThreadPool(Math.max(1, folders.size()));
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutdown requested. Stopping scheduler...");
                executor.shutdownNow();
                try {
                    if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                        logger.warning("Scheduler did not terminate within timeout.");
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                logger.info("Shutdown complete.");
            }));
            
            SftpUploader uploader = new SftpUploader(config, logger, dryRun);
            
            for (Config.FolderConfig f : folders) {
                long interval = effectiveIntervalForFolder(f, globalInterval);
                executor.scheduleAtFixedRate(
                        new FolderMonitorTask(f, uploader, logger),
                        0, interval, TimeUnit.SECONDS
                );
                logger.info(() -> String.format("Scheduled folder '%s' every %d seconds", f.getPath(), interval));
            }
            
            logger.info("labv-sftp-transfer started. Press Ctrl+C to exit.");
            
            // Keep process alive
            while (true) {
                TimeUnit.HOURS.sleep(24);
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
    
    /**
     * Returns the effective interval for a folder.
     * Global rule: globalInterval >= 1 in periodic mode.
     * Per-folder interval applies only if set (>=1). Otherwise use global.
     */
    private long effectiveIntervalForFolder(Config.FolderConfig folder, long globalInterval) {
        Integer folderIv = folder.getIntervalSeconds();
        if (Objects.nonNull(folderIv) && folderIv >= 1) {
            return folderIv;
        }
        return globalInterval;
    }
    
    public static void main(String[] args) {
        int exitCode = new CommandLine(new MainCommand()).execute(args);
        System.exit(exitCode);
    }
}
