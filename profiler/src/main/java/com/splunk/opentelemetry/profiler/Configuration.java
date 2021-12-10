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

import com.google.auto.service.AutoService;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.extension.config.ConfigPropertySource;
import java.util.HashMap;
import java.util.Map;

@AutoService(ConfigPropertySource.class)
public class Configuration implements ConfigPropertySource {

  private static final String DEFAULT_RECORDING_DURATION = "20s";

  public static final String CONFIG_KEY_ENABLE_PROFILER = "splunk.profiler.enabled";
  public static final String CONFIG_KEY_PROFILER_DIRECTORY = "splunk.profiler.directory";
  public static final String CONFIG_KEY_RECORDING_DURATION = "splunk.profiler.recording.duration";
  public static final String CONFIG_KEY_KEEP_FILES = "splunk.profiler.keep-files";
  public static final String CONFIG_KEY_INGEST_URL = "splunk.profiler.logs-endpoint";
  public static final String CONFIG_KEY_OTEL_OTLP_URL = "otel.exporter.otlp.endpoint";
  public static final String CONFIG_KEY_PERIOD_PREFIX = "splunk.profiler.period";
  public static final String CONFIG_KEY_MEMORY_ENABLED = "splunk.profiler.memory.enabled";
  public static final String CONFIG_KEY_TLAB_ENABLED = "splunk.profiler.tlab.enabled";
  public static final String CONFIG_KEY_INCLUDE_AGENT_INTERNALS =
      "splunk.profiler.include.agent.internals";
  // Include stacks where every frame starts with jvm/sun/jdk
  public static final String CONFIG_KEY_INCLUDE_JVM_INTERNALS =
      "splunk.profiler.include.jvm.internals";

  @Override
  public Map<String, String> getProperties() {
    HashMap<String, String> config = new HashMap<>();
    config.put(CONFIG_KEY_ENABLE_PROFILER, "false");
    config.put(CONFIG_KEY_PROFILER_DIRECTORY, ".");
    config.put(CONFIG_KEY_RECORDING_DURATION, DEFAULT_RECORDING_DURATION);
    config.put(CONFIG_KEY_KEEP_FILES, "false");
    config.put(CONFIG_KEY_MEMORY_ENABLED, "false");
    return config;
  }

  public static String getConfigUrl(Config config) {
    String ingestUrl = config.getString(CONFIG_KEY_INGEST_URL);
    if (ingestUrl == null) {
      return config.getString(CONFIG_KEY_OTEL_OTLP_URL);
    }
    return ingestUrl;
  }

  public static boolean getTLABEnabled(Config config) {
    Boolean enabled = config.getBoolean(CONFIG_KEY_TLAB_ENABLED);
    if (enabled == null) {
      return config.getBoolean(CONFIG_KEY_MEMORY_ENABLED, false);
    }
    return enabled;
  }
}
