package com.splunk.opentelemetry.profiler;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordingFileNamingConventionTest {

    Path outputDir = Paths.get("/path/to/outdir");

    @Test
    void testNewPath() {
        RecordingFileNamingConvention convention = new RecordingFileNamingConvention(outputDir);
        LocalDateTime now = LocalDateTime.of(1999, Month.FEBRUARY, 12, 17, 3, 21);
        Path expected = Paths.get("/path/to/outdir/otel-profiler-1999-02-12T17:03:21.jfr");

        Path path = convention.newOutputPath(now);

        assertEquals(expected, path);
    }

    @Test
    void testMatches() {
        RecordingFileNamingConvention convention = new RecordingFileNamingConvention(outputDir);
        LocalDateTime now = LocalDateTime.of(1999, Month.FEBRUARY, 12, 17, 3, 21);
        Path doesMatch = Paths.get("/path/to/outdir/otel-profiler-1999-02-12T17:03:21.jfr");
        Path differentDir = Paths.get("/no/way/out/otel-profiler-1999-02-12T17:03:21.jfr");
        Path badFilename = Paths.get("/path/to/outdir/tugboat-1999-02-12T17:03:21.jfr");

        assertTrue(convention.matches(doesMatch));
        assertFalse(convention.matches(differentDir));
        assertFalse(convention.matches(badFilename));
    }

}