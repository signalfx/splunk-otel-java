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
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RecordingEscapeHatch {

  private static final long MIN_FREE_SPACE_BYTES = 100 * 1024 * 1024; // 100MiB
  private static final Logger logger = LoggerFactory.getLogger(RecordingEscapeHatch.class);
  private static final Duration MAX_PENDING_DURATION = Duration.ofMinutes(5);

  private final RecordingFileNamingConvention namingConvention;
  private final boolean configKeepsFilesOnDisk;
  private final long maxFileCount;
  private final FilesShim filesShim;

  RecordingEscapeHatch(Builder builder) {
    this.namingConvention = builder.namingConvention;
    this.configKeepsFilesOnDisk = builder.configKeepsFilesOnDisk;
    this.filesShim = builder.filesShim;
    this.maxFileCount = MAX_PENDING_DURATION.toMillis() / builder.recordingDuration.toMillis();
  }

  public boolean jfrCanContinue() {
    boolean result = freeSpaceIsOk() && notFileBacklogged();
    if (!result) {
      logger.warn("** THIS WILL RESULT IN LOSS OF PROFILING DATA **");
    }
    return result;
  }

  private boolean freeSpaceIsOk() {
    try {
      Path outputPath = namingConvention.getOutputPath();
      FileStore store = filesShim.getFileStore(outputPath);
      long usableSpace = store.getUsableSpace();
      boolean result = usableSpace > MIN_FREE_SPACE_BYTES;
      if (!result) {
        logger.warn(
            "** NOT STARTING RECORDING, only "
                + usableSpace
                + " bytes free, require "
                + MIN_FREE_SPACE_BYTES);
      }
      return result;
    } catch (IOException e) {
      logger.error("Could not check free space, assuming everything is bad", e);
      return false;
    }
  }

  /**
   * Backlogged means that there are too many recent jfr files sitting on disk, and we think we are
   * falling behind. We will never be backlogged if the user has specified
   * -Dsplunk.profiler.keep-files=true
   */
  private boolean notFileBacklogged() {
    if (configKeepsFilesOnDisk) {
      return true;
    }
    try {
      return pendingFileCount() < maxFileCount;
    } catch (IOException e) {
      logger.warn("Error listing files in output directory, assuming everything is bad");
      return false;
    }
  }

  /** Returns the number of jfr files in the output directory that match our pattern */
  private long pendingFileCount() throws IOException {
    Path outputPath = namingConvention.getOutputPath();
    return filesShim
        .list(outputPath)
        .filter(filesShim::isRegularFile)
        .filter(namingConvention::matches)
        .count();
  }

  static Builder builder() {
    return new Builder();
  }

  // To help with testing
  interface FilesShim {
    FilesShim DEFAULT =
        new FilesShim() {
          @Override
          public FileStore getFileStore(Path path) throws IOException {
            return Files.getFileStore(path);
          }

          @Override
          public Stream<Path> list(Path path) throws IOException {
            return Files.list(path);
          }

          @Override
          public boolean isRegularFile(Path path) {
            return Files.isRegularFile(path);
          }
        };

    FileStore getFileStore(Path path) throws IOException;

    Stream<Path> list(Path path) throws IOException;

    boolean isRegularFile(Path path);
  }

  static class Builder {

    RecordingFileNamingConvention namingConvention;
    boolean configKeepsFilesOnDisk = false;
    Duration recordingDuration;
    FilesShim filesShim = FilesShim.DEFAULT;

    Builder namingConvention(RecordingFileNamingConvention namingConvention) {
      this.namingConvention = namingConvention;
      return this;
    }

    Builder configKeepsFilesOnDisk(boolean configKeepsFilesOnDisk) {
      this.configKeepsFilesOnDisk = configKeepsFilesOnDisk;
      return this;
    }

    Builder recordingDuration(Duration recordingDuration) {
      this.recordingDuration = recordingDuration;
      return this;
    }

    Builder filesShim(FilesShim shim) {
      this.filesShim = shim;
      return this;
    }

    RecordingEscapeHatch build() {
      return new RecordingEscapeHatch(this);
    }
  }
}
