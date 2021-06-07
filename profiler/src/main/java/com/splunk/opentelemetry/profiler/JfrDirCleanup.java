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

import static com.splunk.opentelemetry.profiler.util.HelpfulExecutors.logUncaught;

import com.google.common.annotations.VisibleForTesting;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for cleaning up jfr files that might not have otherwise been cleaned
 * up. It can register a VM shutdown hook to remove its list of pending files at shutdown.
 */
public class JfrDirCleanup {

  private static final Logger logger = LoggerFactory.getLogger(JfrDirCleanup.class);
  private final Consumer<Path> fileDeleter;
  private final Map<Path, Integer> pending = new ConcurrentHashMap<>();

  public JfrDirCleanup(Consumer<Path> fileDeleter) {
    this.fileDeleter = fileDeleter;
  }

  public void recordingCreated(Path path) {
    pending.put(path, 0);
  }

  public void recordingDeleted(Path path) {
    pending.remove(path);
  }

  public void cleanUp() {
    pending
        .keySet()
        .forEach(
            path -> {
              logger.warn("Shutdown :: JfrDirCleanup deleting {}", path);
              fileDeleter.accept(path);
            });
  }

  public void registerShutdownHook() {
    getRuntime().addShutdownHook(new Thread(logUncaught(this::cleanUp)));
  }

  @VisibleForTesting
  protected Runtime getRuntime() {
    return Runtime.getRuntime();
  }
}
