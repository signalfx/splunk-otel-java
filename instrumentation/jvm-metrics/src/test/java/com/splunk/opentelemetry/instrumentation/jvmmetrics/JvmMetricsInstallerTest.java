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

package com.splunk.opentelemetry.instrumentation.jvmmetrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import com.splunk.opentelemetry.instrumentation.jvmmetrics.otel.OtelAllocatedMemoryMetrics;
import com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil;
import io.opentelemetry.instrumentation.config.bridge.DeclarativeConfigPropertiesBridgeBuilder;
import io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

class JvmMetricsInstallerTest {

  @Test
  void shouldInstallJvmMetrics_declarativeConfig() {
    // given
    JvmMetricsInstaller agentListener = new JvmMetricsInstaller();
    AutoConfiguredOpenTelemetrySdk sdk = mock(AutoConfiguredOpenTelemetrySdk.class);
    String yaml =
        """
            file_format: "1.0"
            instrumentation/development:
              java:
                jvm_metrics_splunk:
                  enabled: true
            """;
    ConfigProperties config =
        new DeclarativeConfigPropertiesBridgeBuilder()
            .build(
                AutoConfigureUtil.getInstrumentationConfig(DeclarativeConfigTestUtil.parse(yaml)));

    try (MockedStatic<AutoConfigureUtil> autoConfigureUtil = mockStatic(AutoConfigureUtil.class);
        MockedConstruction<OtelAllocatedMemoryMetrics> allocatedMetrics =
            mockConstruction(OtelAllocatedMemoryMetrics.class)) {
      autoConfigureUtil.when(() -> AutoConfigureUtil.getConfig(sdk)).thenReturn(config);

      // when
      agentListener.afterAgent(sdk);

      // then
      // Verification if `new OtelAllocatedMemoryMetrics().install();` was called from
      // JvmMetricsInstaller.afterAgent
      assertThat(allocatedMetrics.constructed()).hasSize(1);
      verify(allocatedMetrics.constructed().get(0)).install();
    }
  }

  @Test
  void shouldInstallJvmMetrics_envVarsConfig() {
    // given
    JvmMetricsInstaller agentListener = new JvmMetricsInstaller();
    AutoConfiguredOpenTelemetrySdk sdk = mock(AutoConfiguredOpenTelemetrySdk.class);
    ConfigProperties config =
        DefaultConfigProperties.createFromMap(
            Map.of("otel.instrumentation.jvm-metrics-splunk.enabled", "true"));

    try (MockedStatic<AutoConfigureUtil> autoConfigureUtil = mockStatic(AutoConfigureUtil.class);
        MockedConstruction<OtelAllocatedMemoryMetrics> allocatedMetrics =
            mockConstruction(OtelAllocatedMemoryMetrics.class)) {
      autoConfigureUtil.when(() -> AutoConfigureUtil.getConfig(sdk)).thenReturn(config);

      // when
      agentListener.afterAgent(sdk);

      // then
      // Verification if `new OtelAllocatedMemoryMetrics().install();` was called from
      // JvmMetricsInstaller.afterAgent
      assertThat(allocatedMetrics.constructed()).hasSize(1);
      verify(allocatedMetrics.constructed().get(0)).install();
    }
  }
}
