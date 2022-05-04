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

package com.splunk.opentelemetry.micrometer;

import static com.splunk.opentelemetry.SplunkConfiguration.PROFILER_MEMORY_ENABLED_PROPERTY;
import static com.splunk.opentelemetry.SplunkConfiguration.SPLUNK_ACCESS_TOKEN;
import static com.splunk.opentelemetry.SplunkConfiguration.SPLUNK_REALM_NONE;
import static com.splunk.opentelemetry.SplunkConfiguration.SPLUNK_REALM_PROPERTY;
import static com.splunk.opentelemetry.micrometer.SplunkMetricsConfig.DEFAULT_METRICS_ENDPOINT;
import static com.splunk.opentelemetry.micrometer.SplunkMetricsConfig.METRICS_ENABLED_PROPERTY;
import static com.splunk.opentelemetry.micrometer.SplunkMetricsConfig.METRICS_ENDPOINT_PROPERTY;
import static com.splunk.opentelemetry.micrometer.SplunkMetricsConfig.METRICS_EXPORT_INTERVAL_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.core.instrument.config.validate.Validated;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SplunkMetricsConfigTest {
  @Test
  void testDefaultValues() {
    // given
    var javaagentConfig = Config.builder().build();
    var resource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "test-service"));
    var splunkMetricsConfig = new SplunkMetricsConfig(javaagentConfig, resource);

    // when & then
    assertFalse(splunkMetricsConfig.enabled());
    assertNull(splunkMetricsConfig.accessToken());
    assertEquals(DEFAULT_METRICS_ENDPOINT, splunkMetricsConfig.uri());
    assertEquals("test-service", splunkMetricsConfig.source());
    assertEquals(Duration.ofSeconds(30), splunkMetricsConfig.step());
    assertTrue(splunkMetricsConfig.validate().isValid());
  }

  @Test
  void testCustomValues() {
    var javaagentConfig =
        Config.builder()
            .addProperties(
                Map.of(
                    METRICS_ENABLED_PROPERTY,
                    "true",
                    SPLUNK_ACCESS_TOKEN,
                    "token",
                    METRICS_ENDPOINT_PROPERTY,
                    "http://my-endpoint:42",
                    METRICS_EXPORT_INTERVAL_PROPERTY,
                    "60000"))
            .build();
    var resource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "test-service"));
    var splunkMetricsConfig = new SplunkMetricsConfig(javaagentConfig, resource);

    // when & then
    assertTrue(splunkMetricsConfig.enabled());
    assertEquals("token", splunkMetricsConfig.accessToken());
    assertEquals("http://my-endpoint:42", splunkMetricsConfig.uri());
    assertEquals("test-service", splunkMetricsConfig.source());
    assertEquals(Duration.ofSeconds(60), splunkMetricsConfig.step());
    assertTrue(splunkMetricsConfig.validate().isValid());
  }

  @Test
  void emptyServiceNameIsNotValid() {
    // given
    var javaagentConfig = Config.builder().build();
    var resource = Resource.empty();
    var splunkMetricsConfig = new SplunkMetricsConfig(javaagentConfig, resource);

    // when
    Validated<?> validated = splunkMetricsConfig.validate();

    // then
    assertFalse(validated.isValid());
  }

  @Test
  void emptyEndpointIsNotValid() {
    // given
    var javaagentConfig = Config.builder().addProperty(METRICS_ENDPOINT_PROPERTY, "").build();
    var resource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "test-service"));
    var splunkMetricsConfig = new SplunkMetricsConfig(javaagentConfig, resource);

    // when
    Validated<?> validated = splunkMetricsConfig.validate();

    // then
    assertFalse(validated.isValid());
  }

  @Test
  void profilerEnablesMetrics() {
    var javaagentConfig =
        Config.builder()
            .addProperties(
                Map.of(METRICS_ENABLED_PROPERTY, "false", PROFILER_MEMORY_ENABLED_PROPERTY, "true"))
            .build();
    var config = new SplunkMetricsConfig(javaagentConfig, Resource.getDefault());
    assertTrue(config.enabled());
  }

  @Test
  void usesRealmUrlDefaultIfRealmDefined() {
    var javaagentConfig =
        Config.builder().addProperties(Map.of(SPLUNK_REALM_PROPERTY, "test0")).build();
    var config = new SplunkMetricsConfig(javaagentConfig, Resource.getDefault());

    assertEquals(config.uri(), "https://ingest.test0.signalfx.com");
  }

  @Test
  void usesLocalUrlDefaultIfRealmIsNone() {
    var javaagentConfig =
        Config.builder().addProperties(Map.of(SPLUNK_REALM_PROPERTY, SPLUNK_REALM_NONE)).build();
    var config = new SplunkMetricsConfig(javaagentConfig, Resource.getDefault());

    assertEquals(config.uri(), DEFAULT_METRICS_ENDPOINT);
  }
}
