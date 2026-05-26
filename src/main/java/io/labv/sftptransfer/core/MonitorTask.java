package io.labv.sftptransfer.core;

import io.labv.sftptransfer.config.Config;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;


public class MonitorTask implements Runnable {
    
    private final Config config;
    private final SftpUploader sftpUploader;
    private final Logger logger;
    private final boolean dryRun;
    
    public MonitorTask(Config config, Logger logger, boolean dryRun) {
        
        this.config = config;
        this.logger = logger;
        this.dryRun = dryRun;
        this.sftpUploader = new SftpUploader(config, logger, dryRun);
    }
    
    @Override
    public void run() {
        
        for (Config.FolderConfig folder : config.getFolders()) {
            try {
                List<File> matchedFiles = findMatchingFiles(folder);
                for (File file : matchedFiles) {
                    processFile(file, folder);
                }
            } catch (Exception e) {
                logger.severe("Error processing folder: " + folder.getPath() + " – " + e.getMessage());
            }
        }
    }
    
    private List<File> findMatchingFiles(Config.FolderConfig folder) {
        
        File dir = new File(folder.getPath());
        if (!dir.exists() || !dir.isDirectory()) {
            logger.warning("Folder not found or not a directory: " + folder.getPath());
            return Collections.emptyList();
        }
        
        List<File> matchingFiles = new ArrayList<>();
        for (String pattern : folder.getPattern()) {
            @SuppressWarnings("deprecation")
            File[] files = dir.listFiles((FileFilter) new WildcardFileFilter(pattern, IOCase.SYSTEM));
            if (files != null) {
                matchingFiles.addAll(Arrays.asList(files));
            }
        }
        return matchingFiles;
    }
    
    private void processFile(File file, Config.FolderConfig folder) {
        
        logger.info("Processing file: " + file.getAbsolutePath());
        
        if (dryRun) {
            logger.info("[DryRun] Would upload: " + file.getName());
            logPostAction(file, folder, true);
            return;
        }
        
        boolean success = sftpUploader.upload(file); // <- Methode heißt korrekt "upload"
        if (success) {
            logPostAction(file, folder, false);
            handlePostAction(file, folder);
        } else {
            logger.warning("Upload failed: " + file.getName());
        }
    }
    
    private void handlePostAction(File file, Config.FolderConfig folder) {
        
        switch (folder.getPostAction().toLowerCase()) {
            case "delete" -> {
                if (file.delete()) {
                    logger.info("Deleted file: " + file.getName());
                } else {
                    logger.warning("Could not delete file: " + file.getName());
                }
            }
            case "archive" -> {
                File archiveDir = new File(folder.getArchiveDir());
                if (!archiveDir.exists()) {
                    archiveDir.mkdirs();
                }
                File dest = new File(archiveDir, file.getName());
                try {
                    Files.move(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    logger.info("Archived file to: " + dest.getAbsolutePath());
                } catch (IOException e) {
                    logger.warning("Failed to archive file: " + file.getName() + " – " + e.getMessage());
                }
            }
            default -> logger.warning("Unknown postAction: " + folder.getPostAction());
        }
    }
    
    private void logPostAction(File file, Config.FolderConfig folder, boolean isDryRun) {
        
        String action = folder.getPostAction();
        String msg = switch (action.toLowerCase()) {
            case "delete" -> "Would delete";
            case "archive" -> "Would archive to: " + folder.getArchiveDir();
            default -> "Unknown postAction";
        };
        if (isDryRun) {
            logger.info("[DryRun] " + msg + " " + file.getName());
        }
    }
    
}
