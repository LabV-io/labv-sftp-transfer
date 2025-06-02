package io.labv.sftptransfer.config;

import java.io.IOException;
import java.util.List;

import io.labv.sftptransfer.util.KeyPathValidator;


public class ConfigValidator {
    
    private ConfigValidator() {
    
    }
    
    public static void validate(Config config) {
        
        validateInterval(config.getIntervalSeconds());
        validateFolders(config.getFolders());
        validateSftp(config.getSftp());
    }
    
    private static void validateInterval(int intervalSeconds) {
        
        if (intervalSeconds <= 0) {
            throw new IllegalArgumentException("intervalSeconds must be greater than 0.");
        }
    }
    
    private static void validateFolders(List<Config.FolderConfig> folders) {
        
        if (folders == null || folders.isEmpty()) {
            throw new IllegalArgumentException("At least one folder must be configured.");
        }
        
        for (Config.FolderConfig folder : folders) {
            validateSingleFolder(folder);
        }
    }
    
    private static void validateSingleFolder(Config.FolderConfig folder) {
        
        if (folder.getPath() == null || folder.getPath().isEmpty()) {
            throw new IllegalArgumentException("Folder path must be set.");
        }
        
        if (folder.getPattern() == null || folder.getPattern().isEmpty()) {
            throw new IllegalArgumentException("Pattern list must not be empty. Folder: " + folder.getPath());
        }
        
        if (folder.getPostAction() == null || folder.getPostAction().isEmpty()) {
            throw new IllegalArgumentException("postAction must be set for folder: " + folder.getPath());
        }
        
        if ("archive".equalsIgnoreCase(folder.getPostAction()) && (folder.getArchiveDir() == null
                || folder.getArchiveDir().isEmpty())) {
            throw new IllegalArgumentException(
                    "archiveDir must be set for archived folders. Folder: " + folder.getPath());
        }
        
    }
    
    private static void validateSftp(Config.SftpConfig sftp) {
        
        if (sftp == null) {
            throw new IllegalArgumentException("SFTP config must be provided.");
        }
        
        validateSftpBasic(sftp);
        validateSftpAuthentication(sftp);
    }
    
    private static void validateSftpBasic(Config.SftpConfig sftp) {
        
        if (sftp.getHost() == null || sftp.getHost().isEmpty()) {
            throw new IllegalArgumentException("SFTP host is required.");
        }
        
        if (sftp.getPort() <= 0) {
            throw new IllegalArgumentException("SFTP port must be > 0.");
        }
        
        if (sftp.getUsername() == null || sftp.getUsername().isEmpty()) {
            throw new IllegalArgumentException("SFTP username is required.");
        }
        
        if (sftp.getRemoteDir() == null || sftp.getRemoteDir().isEmpty()) {
            throw new IllegalArgumentException("Remote directory must be set.");
        }
    }
    
    private static void validateSftpAuthentication(Config.SftpConfig sftp) {
        
        try {
            KeyPathValidator validator = new KeyPathValidator();
            validator.validate(sftp.getPrivateKeyPath());
        } catch (IOException e) {
            throw new IllegalArgumentException("Private key validation failed: " + e.getMessage(), e);
        }
        
        boolean hasKnownHosts = sftp.getKnownHostsPath() != null && !sftp.getKnownHostsPath().isEmpty();
        boolean hasTrustedKey = sftp.getTrustedHostPublicKey() != null && !sftp.getTrustedHostPublicKey().isEmpty();
        
        if (!hasKnownHosts && !hasTrustedKey) {
            throw new IllegalArgumentException("Either knownHostsPath or trustedHostPublicKey must be set.");
        }
    }
    
}
