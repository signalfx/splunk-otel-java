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

import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_CALL_STACK_INTERVAL;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_DEPRECATED_THREADDUMP_PERIOD;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_ENABLE_PROFILER;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_INGEST_URL;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_KEEP_FILES;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_MEMORY_ENABLED;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_MEMORY_SAMPLER_INTERVAL;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_OTEL_OTLP_URL;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_PROFILER_DIRECTORY;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_RECORDING_DURATION;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_TLAB_ENABLED;
import static com.splunk.opentelemetry.profiler.Configuration.DEFAULT_MEMORY_ENABLED;

import io.opentelemetry.instrumentation.api.config.Config;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class logs the active profiler configuration for debug/troubleshooting purposes. */
public class ConfigurationLogger {

  private static final Logger logger = LoggerFactory.getLogger(ConfigurationLogger.class);

  public void log(Config config) {
    logger.info("-----------------------");
    logger.info("Profiler configuration:");
    log(CONFIG_KEY_ENABLE_PROFILER, (it) -> config.getBoolean(it, false));
    log(CONFIG_KEY_PROFILER_DIRECTORY, config::getString);
    log(CONFIG_KEY_RECORDING_DURATION, config::getString);
    log(CONFIG_KEY_KEEP_FILES, (it) -> config.getBoolean(it, false));
    log(CONFIG_KEY_INGEST_URL, (it) -> Configuration.getConfigUrl(config));
    log(CONFIG_KEY_OTEL_OTLP_URL, (it) -> config.getString(it, null));
    log(CONFIG_KEY_MEMORY_ENABLED, (it) -> config.getBoolean(it, DEFAULT_MEMORY_ENABLED));
    log(CONFIG_KEY_TLAB_ENABLED, (it) -> Configuration.getTLABEnabled(config));
    log(CONFIG_KEY_MEMORY_SAMPLER_INTERVAL, (it) -> Configuration.getMemorySamplerInterval(config));
    log(CONFIG_KEY_CALL_STACK_INTERVAL, (it) -> Configuration.getCallStackInterval(config));
    log(CONFIG_KEY_DEPRECATED_THREADDUMP_PERIOD, (it) -> config.getDuration(it, null));
    logger.info("-----------------------");
  }

  private void log(String key, Function<String, Object> getter) {
    logger.info(" {} : {}", pad(key), getter.apply(key));
  }

  private String pad(String str) {
    return String.format("%39s", str);
  }
}
