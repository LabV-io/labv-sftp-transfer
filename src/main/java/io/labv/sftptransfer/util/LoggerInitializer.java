package io.labv.sftptransfer.util;

import io.labv.sftptransfer.config.Config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;


public class LoggerInitializer {
    
    private LoggerInitializer() {
    
    }
    
    public static void configureLogging() {
        
        try (InputStream configFile = LoggerInitializer.class.getResourceAsStream("/logging.properties")) {
            if (configFile != null) {
                LogManager.getLogManager().readConfiguration(configFile);
            } else {
                System.err.println("logging.properties not found – using default logging config.");
            }
        } catch (IOException e) {
            System.err.println("Failed to configure logging: " + e.getMessage());
        }
    }
    
    public static Logger init(Config.LogConfig logConfig) {
        
        configureLogging();
        
        Logger logger = Logger.getLogger("labv-sftp-transfer");
        logger.setUseParentHandlers(false);
        
        Formatter formatter = new SimpleFormatter();
        
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.INFO);
        consoleHandler.setFormatter(formatter);
        logger.addHandler(consoleHandler);
        
        if (logConfig != null && logConfig.isEnableFileLogging()) {
            String dir = logConfig.getDirectory() != null ? logConfig.getDirectory() : "log";
            String logFilePath = buildLogFilePath(dir);
            
            try {
                File logDir = new File(dir);
                if (!logDir.exists()) {
                    logDir.mkdirs();
                }
                
                FileHandler fileHandler = new FileHandler(logFilePath, true);
                fileHandler.setFormatter(formatter);
                fileHandler.setLevel(Level.INFO);
                logger.addHandler(fileHandler);
                
                deleteOldLogFiles(logDir, logConfig.getRetentionDays(), logger);
                
            } catch (IOException e) {
                logger.warning("Failed to create log file: " + logFilePath);
                e.printStackTrace();
            }
        }
        
        logger.setLevel(Level.INFO);
        return logger;
    }
    
    private static String buildLogFilePath(String directory) {
        
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        return directory + "/" + date + "-labv-sftp-transfer.log";
    }
    
    private static void deleteOldLogFiles(File dir, int retentionDays, Logger logger) {
        
        long cutoff = System.currentTimeMillis() - (retentionDays * 86400_000L);
        File[] files = dir.listFiles((d, name) -> name.endsWith("-labv-sftp-transfer.log"));
        
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.lastModified() < cutoff) {
                if (file.delete()) {
                    logger.info("Deleted old log file: " + file.getName());
                } else {
                    logger.warning("Could not delete log file: " + file.getName());
                }
            }
        }
    }
    
}
