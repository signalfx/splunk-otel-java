package com.splunk.opentelemetry.profiler;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

class RecordingFileNamingConvention {

    private final static String PREFIX = "otel-profiler";
    private final Path outputDir;

    public RecordingFileNamingConvention(Path outputDir) {
        this.outputDir = outputDir;
    }

    /**
     * Constructs a full path for a new jfr file using the current time.
     */
    public Path newOutputPath(){
        return newOutputPath(LocalDateTime.now());
    }

    Path newOutputPath(LocalDateTime dateTime){
        return newOutputPath(buildRecordingName(dateTime));
    }

    private Path newOutputPath(Path recordingFile){
        return outputDir.resolve(recordingFile);
    }

    private Path buildRecordingName(LocalDateTime dateTime) {
        String timestamp =
                DateTimeFormatter.ISO_DATE_TIME.format(
                        dateTime.truncatedTo(ChronoUnit.SECONDS));
        return Paths.get(PREFIX + "-" + timestamp + ".jfr");
    }

    /**
     * Determines if the path represents a file that we would have recorded to.
     */
    public boolean matches(Path path) {
        return outputDir.equals(path.getParent()) && filenameMatches(path);
    }

    private boolean filenameMatches(Path path) {
        String filename = path.getFileName().toFile().getName();
        // ISO_DATE_TIME format is like 2021-12-03T10:15:30
        return filename.startsWith(PREFIX) && filename.matches("^" + PREFIX + "-\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.jfr$");
    }
}
