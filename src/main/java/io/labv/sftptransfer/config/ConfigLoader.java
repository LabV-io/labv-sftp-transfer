package io.labv.sftptransfer.config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class ConfigLoader {
    
    public static Config load(File configFile) throws IOException {
        
        LoaderOptions options = new LoaderOptions();
        Constructor constructor = new Constructor(Config.class, options);
        Yaml yaml = new Yaml(constructor);
        try (FileInputStream input = new FileInputStream(configFile)) {
            return yaml.load(input);
        }
    }
    
}
