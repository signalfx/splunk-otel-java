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
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_MEMORY_ENABLED;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_OTEL_OTLP_URL;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_PERIOD_PREFIX;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_PROFILER_DIRECTORY;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_RECORDING_DURATION;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_TLAB_ENABLED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.netmikey.logunit.api.LogCapturer;
import io.opentelemetry.instrumentation.api.config.Config;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ConfigurationLoggerTest {

  @RegisterExtension
  LogCapturer log = LogCapturer.create().captureForType(ConfigurationLogger.class);

  @Test
  void testLog() {
    Config config = mock(Config.class);

    when(config.getBoolean(CONFIG_KEY_ENABLE_PROFILER)).thenReturn(true);
    when(config.getString(CONFIG_KEY_PROFILER_DIRECTORY)).thenReturn("somedir");
    when(config.getString(CONFIG_KEY_RECORDING_DURATION)).thenReturn("33m");
    when(config.getBoolean(CONFIG_KEY_KEEP_FILES)).thenReturn(true);
    when(config.getString(CONFIG_KEY_INGEST_URL)).thenReturn("http://example.com");
    when(config.getString(CONFIG_KEY_OTEL_OTLP_URL)).thenReturn("http://otel.example.com");
    when(config.getBoolean(CONFIG_KEY_MEMORY_ENABLED)).thenReturn(false);
    when(config.getBoolean(CONFIG_KEY_TLAB_ENABLED)).thenReturn(false);
    when(config.getDuration(CONFIG_KEY_PERIOD_PREFIX + ".threaddump"))
        .thenReturn(Duration.ofMillis(500));

    ConfigurationLogger configurationLogger = new ConfigurationLogger();

    configurationLogger.log(config);

    log.assertContains("-----------------------");
    log.assertContains("Profiler configuration:");
    log.assertContains("                splunk.profiler.enabled : true");
    log.assertContains("              splunk.profiler.directory : somedir");
    log.assertContains("     splunk.profiler.recording.duration : 33m");
    log.assertContains("             splunk.profiler.keep-files : true");
    log.assertContains("          splunk.profiler.logs-endpoint : http://example.com");
    log.assertContains("            otel.exporter.otlp.endpoint : http://otel.example.com");
    log.assertContains("         splunk.profiler.memory.enabled : false");
    log.assertContains("           splunk.profiler.tlab.enabled : false");
    log.assertContains("      splunk.profiler.period.threaddump : PT0.5S");
  }

  @Test
  void testLogInheritDefaultValues() {
    Config config = mock(Config.class);

    when(config.getBoolean(CONFIG_KEY_ENABLE_PROFILER)).thenReturn(true);
    when(config.getString(CONFIG_KEY_PROFILER_DIRECTORY)).thenReturn("somedir");
    when(config.getString(CONFIG_KEY_RECORDING_DURATION)).thenReturn("33m");
    when(config.getBoolean(CONFIG_KEY_KEEP_FILES)).thenReturn(true);
    when(config.getString(CONFIG_KEY_INGEST_URL)).thenReturn(null);
    when(config.getString(CONFIG_KEY_OTEL_OTLP_URL)).thenReturn("http://otel.example.com");
    when(config.getBoolean(CONFIG_KEY_MEMORY_ENABLED)).thenReturn(true);
    when(config.getBoolean(CONFIG_KEY_MEMORY_ENABLED, false)).thenReturn(true);
    when(config.getBoolean(CONFIG_KEY_TLAB_ENABLED)).thenReturn(null);
    when(config.getDuration(CONFIG_KEY_PERIOD_PREFIX + ".threaddump"))
        .thenReturn(Duration.ofMillis(500));

    ConfigurationLogger configurationLogger = new ConfigurationLogger();

    configurationLogger.log(config);

    log.assertContains("-----------------------");
    log.assertContains("Profiler configuration:");
    log.assertContains("                splunk.profiler.enabled : true");
    log.assertContains("              splunk.profiler.directory : somedir");
    log.assertContains("     splunk.profiler.recording.duration : 33m");
    log.assertContains("             splunk.profiler.keep-files : true");
    log.assertContains("          splunk.profiler.logs-endpoint : http://otel.example.com");
    log.assertContains("            otel.exporter.otlp.endpoint : http://otel.example.com");
    log.assertContains("         splunk.profiler.memory.enabled : true");
    log.assertContains("           splunk.profiler.tlab.enabled : true");
    log.assertContains("      splunk.profiler.period.threaddump : PT0.5S");
  }
}
