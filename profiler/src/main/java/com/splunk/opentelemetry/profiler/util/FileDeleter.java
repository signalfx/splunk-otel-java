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

import static java.util.logging.Level.WARNING;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.logging.Logger;

public interface FileDeleter extends Consumer<Path> {

  Logger logger = Logger.getLogger(FileDeleter.class.getName());

  static FileDeleter newDeleter() {
    return path -> {
      try {
        Files.delete(path);
      } catch (IOException e) {
        logger.log(WARNING, "Could not delete: " + path, e);
      }
    };
  }

  static FileDeleter noopFileDeleter() {
    return path -> {
      logger.log(WARNING, "Leaving {0} on disk.", path);
    };
  }
}
