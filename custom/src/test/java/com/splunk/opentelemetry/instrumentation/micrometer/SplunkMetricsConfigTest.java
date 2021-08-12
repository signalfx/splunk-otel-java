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

package com.splunk.opentelemetry.instrumentation.micrometer;

import static com.splunk.opentelemetry.SplunkConfiguration.SPLUNK_ACCESS_TOKEN;
import static com.splunk.opentelemetry.instrumentation.micrometer.SplunkMetricsConfig.DEFAULT_METRICS_ENDPOINT;
import static com.splunk.opentelemetry.instrumentation.micrometer.SplunkMetricsConfig.METRICS_ENABLED_PROPERTY;
import static com.splunk.opentelemetry.instrumentation.micrometer.SplunkMetricsConfig.METRICS_ENDPOINT_PROPERTY;
import static com.splunk.opentelemetry.instrumentation.micrometer.SplunkMetricsConfig.METRICS_EXPORT_INTERVAL_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    var javaagentConfig = Config.newBuilder().build();
    var resource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "test-service"));
    var splunkMetricsConfig = new SplunkMetricsConfig(javaagentConfig, resource);

    // when & then
    assertFalse(splunkMetricsConfig.enabled());
    assertFalse(splunkMetricsConfig.accessToken().isBlank());
    assertEquals(DEFAULT_METRICS_ENDPOINT, splunkMetricsConfig.uri());
    assertEquals("test-service", splunkMetricsConfig.source());
    assertEquals(Duration.ofSeconds(30), splunkMetricsConfig.step());
  }

  @Test
  void testCustomValues() {
    var javaagentConfig =
        Config.newBuilder()
            .readProperties(
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
  }
}
