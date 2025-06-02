package io.labv.sftptransfer.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class KeyPathValidator {
    
    public void validate(String path) throws IOException {
        String resolvedPath = resolveHome(path);
        File keyFile = new File(resolvedPath);
        
        if (!keyFile.exists()) {
            throw new IOException("Key file does not exist: " + keyFile.getAbsolutePath());
        }
        
        if (!keyFile.isFile()) {
            throw new IOException("Key path is not a file: " + keyFile.getAbsolutePath());
        }
        
        if (!keyFile.canRead()) {
            throw new IOException("Key file is not readable: " + keyFile.getAbsolutePath());
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(keyFile))) {
            String firstLine = reader.readLine();
            if (firstLine == null || !firstLine.contains("PRIVATE KEY")) {
                throw new IOException("Key file does not appear to be a valid private key. First line: " + firstLine);
            }
        }
    }
    
    private String resolveHome(String path) {
        if (path.startsWith("~")) {
            return path.replaceFirst("^~", System.getProperty("user.home"));
        }
        return path;
    }
}
