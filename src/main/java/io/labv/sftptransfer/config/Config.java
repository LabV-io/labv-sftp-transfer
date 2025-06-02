package io.labv.sftptransfer.config;

import java.util.List;


public class Config {
    
    private int intervalSeconds;
    private List<FolderConfig> folders;
    private SftpConfig sftp;
    private LogConfig log;
    
    public int getIntervalSeconds() {
        
        return intervalSeconds;
    }
    
    public void setIntervalSeconds(int intervalSeconds) {
        
        this.intervalSeconds = intervalSeconds;
    }
    
    public List<FolderConfig> getFolders() {
        
        return folders;
    }
    
    public void setFolders(List<FolderConfig> folders) {
        
        this.folders = folders;
    }
    
    public SftpConfig getSftp() {
        
        return sftp;
    }
    
    public void setSftp(SftpConfig sftp) {
        
        this.sftp = sftp;
    }
    
    public LogConfig getLog() {
        
        return log;
    }
    
    public void setLog(LogConfig log) {
        
        this.log = log;
    }
    
    public static class FolderConfig {
        
        private String path;
        private List<String> pattern;
        private String postAction;
        private String archiveDir;
        
        public String getPath() {
            
            return path;
        }
        
        public void setPath(String path) {
            
            this.path = path;
        }
        
        public List<String> getPattern() {
            
            return pattern;
        }
        
        public void setPattern(List<String> pattern) {
            
            this.pattern = pattern;
        }
        
        public String getPostAction() {
            
            return postAction;
        }
        
        public void setPostAction(String postAction) {
            
            this.postAction = postAction;
        }
        
        public String getArchiveDir() {
            
            return archiveDir;
        }
        
        public void setArchiveDir(String archiveDir) {
            
            this.archiveDir = archiveDir;
        }
        
    }
    
    public static class SftpConfig {
        
        private String host;
        private int port;
        private String username;
        private String privateKeyPath;
        private String remoteDir;
        private String knownHostsPath;
        private String trustedHostPublicKey;
        
        public String getHost() {
            
            return host;
        }
        
        public void setHost(String host) {
            
            this.host = host;
        }
        
        public int getPort() {
            
            return port;
        }
        
        public void setPort(int port) {
            
            this.port = port;
        }
        
        public String getUsername() {
            
            return username;
        }
        
        public void setUsername(String username) {
            
            this.username = username;
        }
        
        public String getPrivateKeyPath() {
            
            return privateKeyPath;
        }
        
        public void setPrivateKeyPath(String privateKeyPath) {
            
            this.privateKeyPath = privateKeyPath;
        }
        
        public String getRemoteDir() {
            
            return remoteDir;
        }
        
        public void setRemoteDir(String remoteDir) {
            
            this.remoteDir = remoteDir;
        }
        
        public String getKnownHostsPath() {
            
            return knownHostsPath;
        }
        
        public void setKnownHostsPath(String knownHostsPath) {
            
            this.knownHostsPath = knownHostsPath;
        }
        
        public String getTrustedHostPublicKey() {
            
            return trustedHostPublicKey;
        }
        
        public void setTrustedHostPublicKey(String trustedHostPublicKey) {
            
            this.trustedHostPublicKey = trustedHostPublicKey;
        }
        
    }
    
    public static class LogConfig {
        
        private String directory;
        private boolean enableFileLogging = true;
        private int retentionDays = 14;
        
        public String getDirectory() {
            
            return directory;
        }
        
        public void setDirectory(String directory) {
            
            this.directory = directory;
        }
        
        public boolean isEnableFileLogging() {
            
            return enableFileLogging;
        }
        
        public void setEnableFileLogging(boolean enableFileLogging) {
            
            this.enableFileLogging = enableFileLogging;
        }
        
        public int getRetentionDays() {
            
            return retentionDays;
        }
        
        public void setRetentionDays(int retentionDays) {
            
            this.retentionDays = retentionDays;
        }
        
    }
    
}
