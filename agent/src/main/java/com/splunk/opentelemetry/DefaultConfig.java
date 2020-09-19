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

package com.splunk.opentelemetry;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.regex.Pattern;

final class DefaultConfig {
  private static final Pattern ENV_REPLACEMENT = Pattern.compile("[^a-zA-Z0-9_]");
  private static final String CONFIGURATION_FILE_PROPERTY = "otel.trace.config";
  private static final Properties OTEL_CONFIGURATION_FILE = loadConfigurationFile();

  static void setDefaultConfig(String property, String value) {
    if (!isConfigured(property)) {
      System.setProperty(property, value);
    }
  }

  private static boolean isConfigured(String propertyName) {
    return System.getProperty(propertyName) != null
        || System.getenv(toEnvVarName(propertyName)) != null
        || OTEL_CONFIGURATION_FILE.containsKey(propertyName);
  }

  private static String toEnvVarName(String propertyName) {
    return ENV_REPLACEMENT.matcher(propertyName.toUpperCase()).replaceAll("_");
  }

  // this code is copied from otel-java-instrumentation Config class -- we can't call Config here
  // because that would initialize the whole configuration
  private static Properties loadConfigurationFile() {
    Properties properties = new Properties();

    // Reading from system property first and from env after
    String configurationFilePath = System.getProperty(CONFIGURATION_FILE_PROPERTY);
    if (configurationFilePath == null) {
      configurationFilePath = System.getenv(toEnvVarName(CONFIGURATION_FILE_PROPERTY));
    }
    if (configurationFilePath == null) {
      return properties;
    }

    // Normalizing tilde (~) paths for unix systems
    configurationFilePath =
        configurationFilePath.replaceFirst("^~", System.getProperty("user.home"));

    File configurationFile = new File(configurationFilePath);
    if (!configurationFile.exists()) {
      return properties;
    }

    try (FileReader fileReader = new FileReader(configurationFile)) {
      properties.load(fileReader);
    } catch (IOException ignored) {
      // OTel agent will log this error anyway
    }

    return properties;
  }

  private DefaultConfig() {}
}
