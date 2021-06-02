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

import static java.util.Collections.emptyMap;

import com.google.common.annotations.VisibleForTesting;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JfrSettingsReader {

  private static final Logger logger = LoggerFactory.getLogger(JfrSettingsReader.class.getName());
  private static final String DEFAULT_JFR_SETTINGS = "jfr.settings";

  public Map<String, String> read() {
    return read(DEFAULT_JFR_SETTINGS);
  }

  public Map<String, String> read(String resourceName) {
    Map<String, String> result = new HashMap<>();
    try (BufferedReader reader = openResource(resourceName)) {
      if (reader == null) {
        return emptyMap();
      }
      reader
          .lines()
          .filter(line -> !line.trim().startsWith("#")) // ignore commented lines
          .forEach(
              line -> {
                String[] kv = line.split("=");
                result.put(kv[0], kv[1]);
              });
      logger.debug("Read {} JFR settings entries.", result.size());
      return result;
    } catch (IOException e) {
      logger.warn("Error handling settings", e);
      return emptyMap();
    }
  }

  @VisibleForTesting
  BufferedReader openResource(String resourceName) {
    InputStream in =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
    if (in == null) {
      logger.error("Error reading jfr settings, resource {} not found!", resourceName);
      return null;
    }
    return new BufferedReader(new InputStreamReader(in));
  }
}
