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

import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_ENABLE_PROFILER;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_INGEST_URL;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_KEEP_FILES;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_OTEL_OTLP_URL;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_PERIOD_PREFIX;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_PROFILER_DIRECTORY;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_RECORDING_DURATION;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_TLAB_ENABLED;

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
    log(CONFIG_KEY_ENABLE_PROFILER, config::getBoolean);
    log(CONFIG_KEY_PROFILER_DIRECTORY, config::getString);
    log(CONFIG_KEY_RECORDING_DURATION, config::getString);
    log(CONFIG_KEY_KEEP_FILES, config::getBoolean);
    log(CONFIG_KEY_INGEST_URL, config::getString);
    log(CONFIG_KEY_OTEL_OTLP_URL, config::getString);
    log(CONFIG_KEY_TLAB_ENABLED, config::getBoolean);
    log(CONFIG_KEY_PERIOD_PREFIX + "." + "threaddump", config::getDuration);
    logger.info("-----------------------");
  }

  private void log(String key, Function<String, Object> getter) {
    logger.info(" {} : {}", pad(key), getter.apply(key));
  }

  private String pad(String str) {
    return String.format("%38s", str);
  }
}
