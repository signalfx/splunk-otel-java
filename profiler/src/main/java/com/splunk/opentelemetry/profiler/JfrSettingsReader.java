package com.splunk.opentelemetry.profiler;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;

public class JfrSettingsReader {

    private static final Logger logger = LoggerFactory.getLogger(JfrSettingsReader.class.getName());
    private static final String DEFAULT_JFR_SETTINGS = "jfr.settings";

    public Map<String, String> read() {
        return read(DEFAULT_JFR_SETTINGS);
    }

    public Map<String, String> read(String resourceName) {
        Map<String, String> result = new HashMap<>();
        try(BufferedReader reader = openResource(resourceName)){
            if(reader == null){
                return emptyMap();
            }
            reader.lines()
                    .filter(line -> !line.trim().startsWith("#"))   // ignore commented lines
                    .forEach(line -> {
                        String[] kv = line.split("=");
                        result.put(kv[0], kv[1]);
                    });
            logger.debug("Read " + result.size() + " JFR settings entries.");
            return result;
        } catch (IOException e) {
            logger.warn("Error handling settings", e);
            return emptyMap();
        }
    }

    @VisibleForTesting
    BufferedReader openResource(String resourceName) {
        InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resourceName);
        if(in == null){
            logger.error("Error reading jfr settings, resource " + resourceName + " not found!");
            return null;
        }
        return new BufferedReader(new InputStreamReader(in));
    }
}
