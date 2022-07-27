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

import static com.splunk.opentelemetry.SplunkConfiguration.METRICS_ENABLED_PROPERTY;
import static com.splunk.opentelemetry.SplunkConfiguration.METRICS_ENDPOINT_PROPERTY;
import static com.splunk.opentelemetry.SplunkConfiguration.METRICS_EXPORT_INTERVAL_PROPERTY;
import static com.splunk.opentelemetry.SplunkConfiguration.METRICS_IMPLEMENTATION;
import static com.splunk.opentelemetry.SplunkConfiguration.SPLUNK_ACCESS_TOKEN;
import static com.splunk.opentelemetry.SplunkConfiguration.SPLUNK_REALM_NONE;
import static com.splunk.opentelemetry.SplunkConfiguration.SPLUNK_REALM_PROPERTY;
import static com.splunk.opentelemetry.micrometer.SplunkMetricsConfig.DEFAULT_METRICS_ENDPOINT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.core.instrument.config.validate.Validated;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SplunkMetricsConfigTest {
  @Test
  void testDefaultValues() {
    // given
    var autoConfiguredSdk =
        AutoConfiguredOpenTelemetrySdk.builder().setResultAsGlobal(false).build();
    var resource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "test-service"));
    var splunkMetricsConfig = new SplunkMetricsConfig(autoConfiguredSdk.getConfig(), resource);

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
    var autoConfiguredSdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .setResultAsGlobal(false)
            .addPropertiesSupplier(
                () ->
                    Map.of(
                        METRICS_ENABLED_PROPERTY,
                        "true",
                        METRICS_IMPLEMENTATION,
                        "micrometer",
                        SPLUNK_ACCESS_TOKEN,
                        "token",
                        METRICS_ENDPOINT_PROPERTY,
                        "http://my-endpoint:42",
                        METRICS_EXPORT_INTERVAL_PROPERTY,
                        "60000"))
            .build();

    var resource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "test-service"));
    var splunkMetricsConfig = new SplunkMetricsConfig(autoConfiguredSdk.getConfig(), resource);

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
    var autoConfiguredSdk =
        AutoConfiguredOpenTelemetrySdk.builder().setResultAsGlobal(false).build();
    var resource = Resource.empty();
    var splunkMetricsConfig = new SplunkMetricsConfig(autoConfiguredSdk.getConfig(), resource);

    // when
    Validated<?> validated = splunkMetricsConfig.validate();

    // then
    assertFalse(validated.isValid());
  }

  @Test
  void emptyEndpointIsNotValid() {
    // given
    var autoConfiguredSdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .setResultAsGlobal(false)
            .addPropertiesSupplier(() -> Map.of(METRICS_ENDPOINT_PROPERTY, ""))
            .build();
    var resource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "test-service"));
    var splunkMetricsConfig = new SplunkMetricsConfig(autoConfiguredSdk.getConfig(), resource);

    // when
    Validated<?> validated = splunkMetricsConfig.validate();

    // then
    assertFalse(validated.isValid());
  }

  @Test
  void usesRealmUrlDefaultIfRealmDefined() {
    var autoConfiguredSdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .setResultAsGlobal(false)
            .addPropertiesSupplier(() -> Map.of(SPLUNK_REALM_PROPERTY, "test0"))
            .build();
    var config = new SplunkMetricsConfig(autoConfiguredSdk.getConfig(), Resource.getDefault());

    assertEquals(config.uri(), "https://ingest.test0.signalfx.com");
  }

  @Test
  void usesLocalUrlDefaultIfRealmIsNone() {
    var autoConfiguredSdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .setResultAsGlobal(false)
            .addPropertiesSupplier(() -> Map.of(SPLUNK_REALM_PROPERTY, SPLUNK_REALM_NONE))
            .build();
    var config = new SplunkMetricsConfig(autoConfiguredSdk.getConfig(), Resource.getDefault());

    assertEquals(config.uri(), DEFAULT_METRICS_ENDPOINT);
  }
}
