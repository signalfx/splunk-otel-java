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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class OtelLoggerFactoryTest {
  private final InMemoryLogRecordExporter exporter = InMemoryLogRecordExporter.create();
  private final OtelLoggerFactory factory = new OtelLoggerFactory(properties -> exporter);

  @Test
  void configureLoggerWithProfilingInstrumentationScopeName() {
    var properties = DefaultConfigProperties.create(Collections.emptyMap());
    var resource = Resource.getDefault();

    var logger = factory.build(properties, resource);
    logger.logRecordBuilder().setBody("test").emit();

    var logRecord = exporter.getFinishedLogRecordItems().get(0);
    assertEquals("otel.profiling", logRecord.getInstrumentationScopeInfo().getName());
  }

  @Test
  void configureLoggerWithProfilingInstrumentationVersion() {
    var properties = DefaultConfigProperties.create(Collections.emptyMap());
    var resource = Resource.getDefault();

    var logger = factory.build(properties, resource);
    logger.logRecordBuilder().setBody("test").emit();

    var logRecord = exporter.getFinishedLogRecordItems().get(0);
    assertEquals("0.1.0", logRecord.getInstrumentationScopeInfo().getVersion());
  }

  @Test
  void configureLoggerWithOpenTelemetryResource() {
    var properties = DefaultConfigProperties.create(Collections.emptyMap());
    var resource = Resource.create(Attributes.of(AttributeKey.stringKey("test"), "value"));

    var logger = factory.build(properties, resource);
    logger.logRecordBuilder().setBody("test").emit();

    var logRecord = exporter.getFinishedLogRecordItems().get(0);
    assertEquals(resource, logRecord.getResource());
  }
}
