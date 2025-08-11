package io.labv.sftptransfer.core;

import io.labv.sftptransfer.config.Config;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.security.PublicKey;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SftpUploader {
    
    private final Config config;
    private final Logger logger;
    private final boolean dryRun;
    
    public SftpUploader(Config config, Logger logger, boolean dryRun) {
        this.config = Objects.requireNonNull(config, "config");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.dryRun = dryRun;
    }
    
    /** Performs exactly one scan-upload cycle for the given folder. */
    public void processFolderOnce(Config.FolderConfig folder) {
        final Path localDir = Paths.get(folder.getPath());
        if (!Files.isDirectory(localDir)) {
            logger.warning(() -> "Not a directory: " + localDir);
            return;
        }
        
        final List<PathMatcher> matchers = buildMatchers(localDir.getFileSystem(), folder.getPattern());
        final List<Path> files = listMatchingFiles(localDir, matchers);
        if (files.isEmpty()) {
            logger.fine(() -> "No matching files in " + localDir);
            return;
        }
        
        final String foundMsg = "Found " + files.size() + " file(s) in " + localDir;
        logger.info(foundMsg::toString);
        
        final SSHClient ssh = new SSHClient();
        try {
            configureHostKeyVerification(ssh, config.getSftp());
            ssh.connect(config.getSftp().getHost(), config.getSftp().getPort());
            ssh.authPublickey(
                    config.getSftp().getUsername(),
                    ssh.loadKeys(config.getSftp().getPrivateKeyPath())
            );
            try (SFTPClient sftp = ssh.newSFTPClient()) {
                final String remoteRoot = normalizeRemoteDir(config.getSftp().getRemoteDir());
                ensureRemoteDir(sftp, remoteRoot);
                
                for (Path p : files) {
                    if (!Files.isRegularFile(p)) continue;
                    try {
                        uploadOne(sftp, remoteRoot, p);
                        postAction(folder, p);
                    } catch (Exception ex) {
                        final String err = "Failed to process " + p + ": " + ex.getMessage();
                        logger.log(Level.SEVERE, err, ex);
                    }
                }
            }
        } catch (IOException e) {
            final String err = "SFTP session failed: " + e.getMessage();
            logger.log(Level.SEVERE, err, e);
        } finally {
            try { ssh.disconnect(); } catch (IOException ignored) {}
            try { ssh.close(); } catch (IOException ignored) {}
        }
    }
    
    /** Legacy single-file upload kept for compatibility. */
    public boolean upload(File file) {
        if (dryRun) {
            final String remoteFinal = joinRemote(normalizeRemoteDir(config.getSftp().getRemoteDir()), file.getName());
            final String msg = "[DRY-RUN] Would upload to SFTP: " + remoteFinal;
            logger.info(msg::toString);
            return true;
        }
        
        final SSHClient ssh = new SSHClient();
        try {
            configureHostKeyVerification(ssh, config.getSftp());
            ssh.connect(config.getSftp().getHost(), config.getSftp().getPort());
            ssh.authPublickey(
                    config.getSftp().getUsername(),
                    ssh.loadKeys(config.getSftp().getPrivateKeyPath())
            );
            try (SFTPClient sftp = ssh.newSFTPClient()) {
                final String remoteRoot  = normalizeRemoteDir(config.getSftp().getRemoteDir());
                ensureRemoteDir(sftp, remoteRoot);
                final String remoteFinal = joinRemote(remoteRoot, file.getName());
                final String remoteTemp  = remoteFinal + ".part";
                
                final String startMsg = "Uploading " + file.getAbsolutePath() + " -> " + remoteFinal;
                logger.info(startMsg::toString);
                
                sftp.put(file.getAbsolutePath(), remoteTemp);
                try { sftp.rm(remoteFinal); } catch (IOException ignored) {}
                sftp.rename(remoteTemp, remoteFinal);
                
                final String doneMsg = "Uploaded to SFTP: " + remoteFinal;
                logger.info(doneMsg::toString);
            }
            return true;
        } catch (IOException e) {
            final String warn = "Upload failed for file: " + file.getName() + " – " + e.getMessage();
            logger.warning(warn);
            return false;
        } finally {
            try { ssh.disconnect(); } catch (IOException ignored) {}
            try { ssh.close(); } catch (IOException ignored) {}
        }
    }
    
    /* ----------------------- Helpers ----------------------- */
    
    private void uploadOne(SFTPClient sftp, String remoteDir, Path localFile) throws IOException {
        final String filename    = localFile.getFileName().toString();
        final String remoteFinal = joinRemote(remoteDir, filename);
        final String remoteTemp  = remoteFinal + ".part";
        
        if (dryRun) {
            final String msg = "[DRY-RUN] Would upload " + localFile + " -> " + remoteFinal;
            logger.info(msg::toString);
            return;
        }
        
        final String startMsg = "Uploading " + localFile + " -> " + remoteFinal;
        logger.info(startMsg::toString);
        sftp.put(localFile.toString(), remoteTemp);
        try { sftp.rm(remoteFinal); } catch (IOException ignored) {}
        sftp.rename(remoteTemp, remoteFinal);
    }
    
    private void postAction(Config.FolderConfig folder, Path localFile) throws IOException {
        String action = folder.getPostAction();
        if (action == null) action = "none";
        
        switch (action.toLowerCase()) {
            case "archive": {
                final String archiveDirStr = folder.getArchiveDir();
                if (archiveDirStr == null || archiveDirStr.isEmpty()) {
                    final String warn = "archiveDir not set; skipping archive for " + localFile;
                    logger.warning(warn);
                    return;
                }
                final Path archiveDir = Paths.get(archiveDirStr);
                Path target = archiveDir.resolve(localFile.getFileName());
                
                if (dryRun) {
                    final String msg = "[DRY-RUN] Would move " + localFile + " -> " + target;
                    logger.info(msg::toString);
                    return;
                }
                
                Files.createDirectories(archiveDir);
                if (Files.exists(target)) {
                    final String name = localFile.getFileName().toString();
                    final int dot = name.lastIndexOf('.');
                    final String base = (dot > 0) ? name.substring(0, dot) : name;
                    final String ext  = (dot > 0) ? name.substring(dot) : "";
                    target = archiveDir.resolve(base + "_" + Instant.now().toEpochMilli() + ext);
                }
                Files.move(localFile, target, StandardCopyOption.ATOMIC_MOVE);
                
                final String fine = "Archived " + localFile + " -> " + target;
                logger.fine(fine);
                break;
            }
            case "delete": {
                if (dryRun) {
                    final String msg = "[DRY-RUN] Would delete " + localFile;
                    logger.info(msg::toString);
                    return;
                }
                Files.deleteIfExists(localFile);
                final String fine = "Deleted " + localFile;
                logger.fine(fine);
                break;
            }
            case "none":
            default: {
                final String fine = "Post action 'none' for " + localFile;
                logger.fine(fine);
            }
        }
    }
    
    private void ensureRemoteDir(SFTPClient sftp, String remoteDir) throws IOException {
        if (dryRun) {
            final String msg = "[DRY-RUN] Would ensure remote dir: " + remoteDir;
            logger.fine(msg::toString);
            return;
        }
        try {
            sftp.stat(remoteDir);
        } catch (IOException e) {
            sftp.mkdirs(remoteDir);
        }
    }
    
    /** Always uses '/' for remote paths (SFTP servers expect POSIX-style separators). */
    private static String joinRemote(String dir, String name) {
        return dir.endsWith("/") ? dir + name : dir + "/" + name; // always '/' for SFTP paths
    }
    
    /** Normalizes remote root to POSIX style and collapses duplicate slashes. */
    private static String normalizeRemoteDir(String dir) {
        if (dir == null || dir.isEmpty()) return "/";
        String d = dir.replace('\\', '/').trim();
        // collapse multiple slashes except a possible leading double slash used by some servers
        return d.replaceAll("(?<!^)/{2,}", "/");
    }
    
    private static List<PathMatcher> buildMatchers(FileSystem fs, List<String> patterns) {
        final List<PathMatcher> matchers = new ArrayList<>();
        for (String p : patterns) {
            final String spec = (p.contains(":")) ? p : "glob:" + p; // default to glob
            matchers.add(fs.getPathMatcher(spec));
        }
        return matchers;
    }
    
    private static List<Path> listMatchingFiles(Path dir, List<PathMatcher> matchers) {
        final List<Path> result = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                if (Files.isRegularFile(p) && matchesAny(p.getFileName(), matchers)) {
                    result.add(p);
                }
            }
        } catch (IOException ignored) {
            // Listing failure is non-fatal; caller logs cycle-level errors.
        }
        return result;
    }
    
    private static boolean matchesAny(Path filename, List<PathMatcher> matchers) {
        for (PathMatcher m : matchers) {
            if (m.matches(filename)) return true;
        }
        return false;
    }
    
    /* ---------------- Host key verification ---------------- */
    
    private void configureHostKeyVerification(SSHClient ssh, Config.SftpConfig sftp) throws IOException {
        final boolean hasKnownHosts = sftp.getKnownHostsPath() != null && !sftp.getKnownHostsPath().isEmpty();
        final boolean hasTrustedKey = sftp.getTrustedHostPublicKey() != null && !sftp.getTrustedHostPublicKey().isEmpty();
        
        if (hasKnownHosts) {
            final File knownHostsFile = new File(sftp.getKnownHostsPath());
            if (!knownHostsFile.exists()) {
                throw new IOException("Known hosts file not found: " + knownHostsFile.getAbsolutePath());
            }
            ssh.addHostKeyVerifier(new OpenSSHKnownHosts(knownHostsFile));
        } else if (hasTrustedKey) {
            ssh.addHostKeyVerifier(new TolerantSingleKeyVerifier(sftp.getTrustedHostPublicKey().trim()));
        } else {
            throw new IllegalStateException("No host key verification configured. Set either knownHostsPath or trustedHostPublicKey.");
        }
    }
    
    /** Accepts "ssh-ed25519 AAAA..." or just the base64 payload; matches algorithm loosely. */
    private static final class TolerantSingleKeyVerifier implements HostKeyVerifier {
        private final String expectedType;   // may be null
        private final String expectedBase64; // base64 payload only
        
        TolerantSingleKeyVerifier(String trustedKey) {
            String type = null;
            String b64  = trustedKey;
            final String[] parts = trustedKey.split("\\s+");
            if (parts.length >= 2 && parts[1].matches("^[A-Za-z0-9+/=]+$")) {
                type = parts[0].toLowerCase(Locale.ROOT);
                b64  = parts[1];
            }
            this.expectedType = type;
            this.expectedBase64 = b64;
        }
        
        @Override
        public boolean verify(String hostname, int port, PublicKey key) {
            try {
                final String algo = (key.getAlgorithm() == null) ? "" : key.getAlgorithm().toLowerCase(Locale.ROOT);
                final String actualB64 = Base64.getEncoder().encodeToString(key.getEncoded());
                
                final boolean typeOk =
                        (expectedType == null)
                                || (expectedType.contains("ed25519") && algo.contains("ed25519"))
                                || (expectedType.contains("rsa")     && algo.contains("rsa"))
                                || ((expectedType.contains("dss") || expectedType.contains("dsa"))
                                && (algo.contains("dsa") || algo.contains("dss")));
                
                final boolean payloadOk = actualB64.contains(expectedBase64);
                
                return typeOk && payloadOk;
            } catch (Exception e) {
                return false;
            }
        }
        
        @Override
        public List<String> findExistingAlgorithms(String hostname, int port) {
            return Collections.emptyList();
        }
        
        @Override
        public String toString() {
            return "TolerantSingleKeyVerifier";
        }
    }
}
