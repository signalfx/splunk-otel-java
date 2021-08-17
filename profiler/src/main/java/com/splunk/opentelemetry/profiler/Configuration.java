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
import io.opentelemetry.javaagent.extension.config.ConfigPropertySource;
import java.util.HashMap;
import java.util.Map;

@AutoService(ConfigPropertySource.class)
public class Configuration implements ConfigPropertySource {

  public static final String CONFIG_KEY_ENABLE_PROFILER = "splunk.profiler.enabled";
  public static final String CONFIG_KEY_PROFILER_DIRECTORY = "splunk.profiler.directory";
  public static final String CONFIG_KEY_RECORDING_DURATION_MS =
      "splunk.profiler.recording.duration";
  public static final String CONFIG_KEY_KEEP_FILES = "splunk.profiler.keep-files";
  public static final String CONFIG_KEY_INGEST_URL = "splunk.profiler.logs-endpoint";
  public static final String CONFIG_KEY_OTEL_OTLP_URL = "otel.exporter.otlp.endpoint";
  public static final String CONFIG_KEY_PERIOD_PREFIX = "splunk.profiler.period";

  @Override
  public Map<String, String> getProperties() {
    HashMap<String, String> config = new HashMap<>();
    config.put(CONFIG_KEY_ENABLE_PROFILER, "false");
    config.put(CONFIG_KEY_KEEP_FILES, "false");
    return config;
  }
}
