package io.labv.sftptransfer.core;

import io.labv.sftptransfer.config.Config;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runnable that performs exactly one scan-upload cycle for a single configured folder.
 * Scheduling (periodic vs. run-once) is controlled by the caller (MainCommand).
 */
public class FolderMonitorTask implements Runnable {
    
    private final Config.FolderConfig folder;
    private final SftpUploader uploader;
    private final Logger logger;
    
    public FolderMonitorTask(Config.FolderConfig folder,
            SftpUploader uploader,
            Logger logger) {
        this.folder = folder;
        this.uploader = uploader;
        this.logger = logger;
    }
    
    @Override
    public void run() {
        try {
            logger.fine(() -> "Starting cycle for folder: " + folder.getPath());
            uploader.processFolderOnce(folder);
            logger.fine(() -> "Completed cycle for folder: " + folder.getPath());
        } catch (Exception e) {
            // Defensive catch to ensure scheduler continues even if one cycle fails
            logger.log(Level.SEVERE, "Error while processing folder " + folder.getPath() + ": " + e.getMessage(), e);
        }
    }
}
