package com.splunk.opentelemetry.profiler.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public interface FileDeleter extends Consumer<Path> {

    Logger logger = LoggerFactory.getLogger(FileDeleter.class);

    static FileDeleter newDeleter(){
        return path -> {
            try {
                Files.delete(path);
            } catch (IOException e) {
                logger.warn("Could not delete: " + path, e);
            }
        };
    }

    static FileDeleter noopFileDeleter(){
        return path -> {
            logger.warn("Leaving " + path + " on disk.");
        };
    }

}
