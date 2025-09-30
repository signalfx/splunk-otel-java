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
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_ENABLE_PROFILER;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_INCLUDE_INTERNAL_STACKS;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_INGEST_URL;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_KEEP_FILES;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_MEMORY_ENABLED;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_OTEL_OTLP_URL;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_PROFILER_DIRECTORY;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_RECORDING_DURATION;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_TRACING_STACKS_ONLY;
import static com.splunk.opentelemetry.profiler.Configuration.DEFAULT_CALL_STACK_INTERVAL;
import static com.splunk.opentelemetry.profiler.Configuration.DEFAULT_INCLUDE_INTERNAL_STACKS;
import static com.splunk.opentelemetry.profiler.Configuration.DEFAULT_MEMORY_ENABLED;
import static com.splunk.opentelemetry.profiler.Configuration.DEFAULT_TRACING_STACKS_ONLY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.netmikey.logunit.api.LogCapturer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ConfigurationLoggerTest {

  @RegisterExtension
  LogCapturer log = LogCapturer.create().captureForType(ConfigurationLogger.class);

  @Test
  void testLog() {
    ConfigProperties config = mock(ConfigProperties.class);

    when(config.getBoolean(CONFIG_KEY_ENABLE_PROFILER, false)).thenReturn(true);
    when(config.getString(CONFIG_KEY_PROFILER_DIRECTORY)).thenReturn("somedir");
    when(config.getDuration(CONFIG_KEY_RECORDING_DURATION)).thenReturn(Duration.ofMinutes(33));
    when(config.getBoolean(CONFIG_KEY_KEEP_FILES, false)).thenReturn(true);
    when(config.getString(CONFIG_KEY_OTEL_OTLP_URL, null)).thenReturn("http://otel.example.com");
    when(config.getString(CONFIG_KEY_INGEST_URL, "http://otel.example.com"))
        .thenReturn("http://example.com");
    when(config.getBoolean(CONFIG_KEY_MEMORY_ENABLED, DEFAULT_MEMORY_ENABLED)).thenReturn(false);
    when(config.getDuration(CONFIG_KEY_CALL_STACK_INTERVAL, DEFAULT_CALL_STACK_INTERVAL))
        .thenReturn(Duration.ofSeconds(21));
    when(config.getBoolean(CONFIG_KEY_INCLUDE_INTERNAL_STACKS, DEFAULT_INCLUDE_INTERNAL_STACKS))
        .thenReturn(true);
    when(config.getBoolean(CONFIG_KEY_TRACING_STACKS_ONLY, DEFAULT_TRACING_STACKS_ONLY))
        .thenReturn(true);

    ConfigurationLogger configurationLogger = new ConfigurationLogger();

    configurationLogger.log(config);

    log.assertContains("-----------------------");
    log.assertContains("Profiler configuration:");
    log.assertContains("                splunk.profiler.enabled : true");
    log.assertContains("              splunk.profiler.directory : somedir");
    log.assertContains("     splunk.profiler.recording.duration : PT33M");
    log.assertContains("             splunk.profiler.keep-files : true");
    log.assertContains("          splunk.profiler.logs-endpoint : http://example.com");
    log.assertContains("            otel.exporter.otlp.endpoint : http://otel.example.com");
    log.assertContains("         splunk.profiler.memory.enabled : false");
    log.assertContains("    splunk.profiler.call.stack.interval : PT21S");
    log.assertContains("splunk.profiler.include.internal.stacks : true");
    log.assertContains("    splunk.profiler.tracing.stacks.only : true");
  }

  @Test
  void testLogInheritDefaultValues() {
    ConfigProperties config = mock(ConfigProperties.class);

    String inheritedUrl = "http://otel.example.com";
    when(config.getString(CONFIG_KEY_OTEL_OTLP_URL, null)).thenReturn(inheritedUrl);
    when(config.getString(CONFIG_KEY_INGEST_URL, inheritedUrl)).thenReturn(inheritedUrl);
    when(config.getBoolean(CONFIG_KEY_MEMORY_ENABLED, DEFAULT_MEMORY_ENABLED)).thenReturn(true);

    ConfigurationLogger configurationLogger = new ConfigurationLogger();

    configurationLogger.log(config);

    log.assertContains("          splunk.profiler.logs-endpoint : http://otel.example.com");
    log.assertContains("            otel.exporter.otlp.endpoint : http://otel.example.com");
    log.assertContains("         splunk.profiler.memory.enabled : true");
  }
}
