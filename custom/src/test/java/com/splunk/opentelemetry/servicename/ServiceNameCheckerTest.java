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

package com.splunk.opentelemetry.servicename;

import static com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil.createAutoConfiguredSdk;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ServiceNameCheckerTest {
  @RegisterExtension static final AutoCleanupExtension autoCleanup = AutoCleanupExtension.create();

  @Test
  void shouldLogWarnWhenEnvVarConfigDoesNotDefineServiceName() {
    AutoConfiguredOpenTelemetrySdk autoConfiguredSdk =
        AutoConfiguredOpenTelemetrySdk.builder().build();
    List<String> warnings = new ArrayList<>();

    new ServiceNameChecker(warnings::add).beforeAgent(autoConfiguredSdk);

    assertThat(warnings)
        .singleElement()
        .satisfies(message -> assertThat(message).contains("OTEL_SERVICE_NAME"));
  }

  @Test
  void shouldLogWarnWhenDeclarativeConfigDoesNotDefineServiceName(@TempDir Path tempDir)
      throws IOException {
    AutoConfiguredOpenTelemetrySdk autoConfiguredSdk =
        createAutoConfiguredSdk("file_format: \"1.0\"", tempDir, autoCleanup);
    List<String> warnings = new ArrayList<>();

    new ServiceNameChecker(warnings::add).beforeAgent(autoConfiguredSdk);

    assertThat(warnings)
        .singleElement()
        .satisfies(message -> assertThat(message).contains("configuration YAML file"));
  }

  @ParameterizedTest
  @MethodSource("resourceServiceNameCases")
  void shouldEvaluateResourceServiceNameConfiguration(String serviceName, boolean expected) {
    assertThat(ServiceNameChecker.isServiceNameConfigured(resourceWithServiceName(serviceName)))
        .isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("configAndResourceServiceNameCases")
  void shouldEvaluateConfigAndResourceServiceNameConfiguration(
      String otelServiceName,
      String resourceAttributesServiceName,
      String resourceServiceName,
      boolean expected) {
    ConfigProperties config = configProperties(otelServiceName, resourceAttributesServiceName);

    assertThat(
            ServiceNameChecker.isServiceNameConfigured(
                config, resourceWithServiceName(resourceServiceName)))
        .isEqualTo(expected);
  }

  private static ConfigProperties configProperties(
      String otelServiceName, String resourceAttributesServiceName) {
    Map<String, String> properties = new HashMap<>();
    if (otelServiceName != null) {
      properties.put("otel.service.name", otelServiceName);
    }
    if (resourceAttributesServiceName != null) {
      properties.put(
          "otel.resource.attributes", SERVICE_NAME.getKey() + "=" + resourceAttributesServiceName);
    }
    return DefaultConfigProperties.createFromMap(properties);
  }

  private static Resource resourceWithServiceName(String serviceName) {
    if (serviceName == null) {
      return Resource.empty();
    }
    return Resource.create(Attributes.of(SERVICE_NAME, serviceName));
  }

  private static Stream<Arguments> resourceServiceNameCases() {
    return Stream.of(
        Arguments.of("test-service", true),
        Arguments.of(" test-service ", true),
        Arguments.of(null, false),
        Arguments.of("", false),
        Arguments.of("  ", false),
        Arguments.of("unknown_service:java", false));
  }

  private static Stream<Arguments> configAndResourceServiceNameCases() {
    return Stream.of(
        Arguments.of("test-service", null, null, true),
        Arguments.of("", null, null, false),
        Arguments.of("  ", null, null, false),
        Arguments.of(null, "test-service", null, true),
        Arguments.of(null, "", null, false),
        Arguments.of(null, "  ", null, false),
        Arguments.of(null, null, "test-service", true),
        Arguments.of(null, null, "", false),
        Arguments.of(null, null, "  ", false),
        Arguments.of(null, null, "unknown_service:java", false),
        Arguments.of("  ", null, "test-service", true),
        Arguments.of(null, "", "test-service", true),
        Arguments.of(null, null, null, false),
        Arguments.of(null, "  ", "unknown_service:java", false),
        Arguments.of("  ", null, "unknown_service:java", false));
  }
}
