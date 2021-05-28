package com.splunk.opentelemetry.profiler;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JfrSettingsReaderTest {

    @Test
    void testReader() {
        String content = "jdk.EvacuationFailed#enabled=true\n" +
                "# lines can start with comments\n" +
                "jdk.ClassLoad#threshold=0 ms\n" +
                "  # and comments can be indented\n" +
                "jdk.ReservedStackActivation#enabled=true\n";

        Map<String,String> expected = new HashMap<>();
        expected.put("jdk.EvacuationFailed#enabled", "true");
        expected.put("jdk.ClassLoad#threshold", "0 ms");
        expected.put("jdk.ReservedStackActivation#enabled", "true");

        BufferedReader reader = new BufferedReader(new StringReader(content));
        JfrSettingsReader settingsReader = new JfrSettingsReader() {
            @Override
            BufferedReader openResource(String resourceName) {
                return reader;
            }
        };
        Map<String, String> result = settingsReader.read();
        assertEquals(expected, result);
    }

    @Test
    void testCannotFindResource() {
        JfrSettingsReader settingsReader = new JfrSettingsReader() {
            @Override
            BufferedReader openResource(String resourceName) {
                return null;
            }
        };
        Map<String, String> result = settingsReader.read();
        assertTrue(result.isEmpty());
    }

}