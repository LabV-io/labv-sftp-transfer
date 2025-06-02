package io.labv.sftptransfer.core;

import io.labv.sftptransfer.config.Config;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;

import java.io.File;
import java.io.IOException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;


public class SftpUploader {
    
    private final Config.SftpConfig config;
    private final Logger logger;
    private final boolean dryRun;
    
    public SftpUploader(Config.SftpConfig config, Logger logger, boolean dryRun) {
        
        this.config = config;
        this.logger = logger;
        this.dryRun = dryRun;
    }
    
    public boolean upload(File file) {
        
        if (dryRun) {
            logger.info("[DRY-RUN] Would upload to SFTP: " + config.getRemoteDir() + "/" + file.getName());
            return true;
        }
        
        SSHClient ssh = new SSHClient();
        try {
            configureHostKeyVerification(ssh, config);
            
            ssh.connect(config.getHost(), config.getPort());
            ssh.authPublickey(config.getUsername(), ssh.loadKeys(config.getPrivateKeyPath()));
            
            try (SFTPClient sftp = ssh.newSFTPClient()) {
                String remotePath = config.getRemoteDir() + File.separator + file.getName();
                logger.info(remotePath);
                logger.info(file.getAbsolutePath());
                sftp.put(file.getAbsolutePath(), remotePath);
                logger.info("Uploaded to SFTP: " + remotePath);
            }
            
            return true;
            
        } catch (IOException e) {
            logger.warning("Upload failed for file: " + file.getName() + " – " + e.getMessage());
            return false;
            
        } finally {
            try {
                ssh.disconnect();
                ssh.close();
            } catch (IOException e) {
                logger.fine("SSHClient cleanup failed: " + e.getMessage());
            }
        }
    }
    
    
    private void configureHostKeyVerification(SSHClient ssh, Config.SftpConfig config) throws IOException {
        
        boolean hasKnownHosts = config.getKnownHostsPath() != null && !config.getKnownHostsPath().isEmpty();
        boolean hasTrustedKey = config.getTrustedHostPublicKey() != null && !config.getTrustedHostPublicKey().isEmpty();
        
        if (hasKnownHosts) {
            File knownHostsFile = new File(config.getKnownHostsPath());
            if (!knownHostsFile.exists()) {
                throw new IOException("Known hosts file not found: " + knownHostsFile.getAbsolutePath());
            }
            ssh.addHostKeyVerifier(new OpenSSHKnownHosts(knownHostsFile));
        } else if (hasTrustedKey) {
            String expected = config.getTrustedHostPublicKey().trim();
            
            ssh.addHostKeyVerifier(new HostKeyVerifier() {
                
                @Override
                public boolean verify(String hostname, int port, PublicKey key) {
                    
                    String actual = key.getAlgorithm() + " " + Base64.getEncoder().encodeToString(key.getEncoded());
                    return expected.equals(actual);
                }
                
                @Override
                public List<String> findExistingAlgorithms(String hostname, int port) {
                    
                    return Collections.emptyList();
                }
                
                @Override
                public String toString() {
                    
                    return "ExplicitPublicKeyVerifier";
                }
            });
        } else {
            throw new IllegalStateException(
                    "No host key verification configured. Set either knownHostsPath or trustedHostPublicKey.");
        }
    }
    
}
