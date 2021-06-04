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

package com.splunk.opentelemetry.profiler.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface FileDeleter extends Consumer<Path> {

  Logger logger = LoggerFactory.getLogger(FileDeleter.class);

  static FileDeleter newDeleter() {
    return path -> {
      try {
        Files.delete(path);
      } catch (IOException e) {
        logger.warn("Could not delete: " + path, e);
      }
    };
  }

  static FileDeleter noopFileDeleter() {
    return path -> {
      logger.warn("Leaving " + path + " on disk.");
    };
  }
}
