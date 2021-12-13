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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.logs.OtlpLogsExporter;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.Test;

class LogsExporterBuilderTest {

  @Test
  void testBuildSimple() {
    Config config = mock(Config.class);
    OtlpLogsExporter exporter =
        (OtlpLogsExporter) LogsExporterBuilder.fromConfig(config, Resource.empty());
    String endpoint = exporter.getEndpoint();
    assertEquals("localhost:4317", endpoint);
    assertNotNull(exporter.getAdapter());
  }

  @Test
  void testCustomEndpoint() {
    Config config = mock(Config.class);
    when(config.getString(Configuration.CONFIG_KEY_INGEST_URL))
        .thenReturn("http://example.com:9122/");
    OtlpLogsExporter exporter =
        (OtlpLogsExporter) LogsExporterBuilder.fromConfig(config, Resource.empty());
    String endpoint = exporter.getEndpoint();
    assertEquals("example.com:9122", endpoint);
    assertNotNull(exporter.getAdapter());
  }

  @Test
  void defaultEndpoint() {
    Config config = mock(Config.class);
    when(config.getString(Configuration.CONFIG_KEY_OTEL_OTLP_URL))
        .thenReturn("http://mycollector.com:4312/");
    OtlpLogsExporter exporter =
        (OtlpLogsExporter) LogsExporterBuilder.fromConfig(config, Resource.empty());
    String endpoint = exporter.getEndpoint();
    assertEquals("mycollector.com:4312", endpoint);
    assertNotNull(exporter.getAdapter());
  }
}
