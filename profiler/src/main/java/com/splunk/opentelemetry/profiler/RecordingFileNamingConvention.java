/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.opentelemetry.profiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

class RecordingFileNamingConvention {

  private static final String PREFIX = "otel-profiler";
  // ISO_DATE_TIME format is like 2021-12-03T10:15:30
  private final Pattern filenamePattern =
      Pattern.compile("^" + PREFIX + "-\\d{4}-\\d{2}-\\d{2}T\\d{2}_\\d{2}_\\d{2}-\\d+\\.jfr$");
  private final Path outputDir;

  RecordingFileNamingConvention(Path outputDir) {
    this.outputDir = outputDir;
  }

  /** Constructs a full path for a new jfr file using the current time. */
  Path newOutputPath() throws IOException {
    return newOutputPath(LocalDateTime.now());
  }

  Path newOutputPath(LocalDateTime dateTime) throws IOException {
    String timestamp =
        DateTimeFormatter.ISO_DATE_TIME.format(dateTime.truncatedTo(ChronoUnit.SECONDS));
    Path path =
        Files.createTempFile(outputDir, PREFIX + "-" + timestamp.replace(':', '_') + "-", ".jfr");
    System.err.println("created path" + path + " " + Files.isReadable(path));
    new Exception().printStackTrace();
    return path;
  }

  /** Determines if the path represents a file that we would have recorded to. */
  boolean matches(Path path) {
    return outputDir.equals(path.getParent()) && filenameMatches(path);
  }

  private boolean filenameMatches(Path path) {
    String filename = path.getFileName().toString();
    return filenamePattern.matcher(filename).matches();
  }

  Path getOutputPath() {
    return outputDir;
  }
}
