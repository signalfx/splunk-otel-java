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

package com.splunk.opentelemetry.jmx;

import static com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil.createAutoConfiguredSdk;
import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getConfigProvider;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.instrumentation.config.bridge.DeclarativeConfigBridge;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.javaagent.extension.instrumentation.internal.AgentDistributionConfig;
import io.opentelemetry.javaagent.instrumentation.jmx.JmxMetricInsightInstaller;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JmxMetricInsightSupervisorInitializationTest {
  private static final String YAML =
      """
      file_format: "1.2"
      instrumentation/development:
        java:
          jmx:
            discovery:
              delay: 1000
      distribution:
        javaagent:
          instrumentation:
      %s
      """;

  @RegisterExtension static final AutoCleanupExtension cleanup = AutoCleanupExtension.create();

  @TempDir Path tempDir;

  @BeforeEach
  void resetAgentConfig() {
    AgentDistributionConfig.resetForTest();
    cleanup.deferCleanup(AgentDistributionConfig::resetForTest);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"disabled: [jmx]", "default_enabled: false", "enabled: [jmx]\ndisabled: [jmx]"})
  void disabledDeclarativeConfigurationRejectsRemoteUpdates(String instrumentation)
      throws Exception {
    var sdk = createAutoConfiguredSdk(YAML.formatted(instrumentation.indent(6)), tempDir, cleanup);
    var startup = getConfigProvider(sdk).getInstrumentationConfig("jmx");
    assertThat(startup.get("discovery").getLong("delay")).isEqualTo(1000);

    assertDisabledSupervisor(sdk.getOpenTelemetrySdk(), startup);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"otel.instrumentation.jmx.enabled", "otel.instrumentation.common.default-enabled"})
  void disabledPropertyConfigurationRejectsRemoteUpdates(String property) throws Exception {
    var properties =
        DefaultConfigProperties.createFromMap(
            Map.of(property, "false", "otel.jmx.discovery.delay", "2s"));
    AgentDistributionConfig.set(AgentDistributionConfig.fromConfigProperties(properties));
    assertDisabledSupervisor(
        OpenTelemetry.noop(),
        DeclarativeConfigBridge.createInstrumentationConfig(properties)
            .getInstrumentationConfig("jmx"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"default_enabled: true", "default_enabled: false\nenabled: [jmx]"})
  void enabledConfigurationPublishesCollectingSupervisor(String instrumentation) throws Exception {
    var sdk = createAutoConfiguredSdk(YAML.formatted(instrumentation.indent(6)), tempDir, cleanup);
    var supervisor =
        JmxMetricInsightSupervisor.initialize(
            sdk.getOpenTelemetrySdk(), getConfigProvider(sdk).getInstrumentationConfig("jmx"));
    cleanup.deferCleanup(supervisor);

    assertThat(JmxMetricInsightSupervisor.getInstance()).isSameAs(supervisor);
    assertThat(supervisor.isEnabled()).isTrue();
    supervisor.start();
    supervisor.applyConfiguration(remoteConfig());
    assertThat(supervisor.isEnabled()).isTrue();
  }

  @Test
  void closingDisabledSupervisorAllowsReinitialization() throws Exception {
    AgentDistributionConfig.set(
        AgentDistributionConfig.fromConfigProperties(
            DefaultConfigProperties.createFromMap(
                Map.of("otel.instrumentation.jmx.enabled", "false"))));
    var disabled =
        JmxMetricInsightSupervisor.initialize(
            OpenTelemetry.noop(), DeclarativeConfigProperties.empty());
    cleanup.deferCleanup(disabled);
    assertThatThrownBy(() ->
            JmxMetricInsightSupervisor.initialize(
                OpenTelemetry.noop(), DeclarativeConfigProperties.empty()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already initialized");

    disabled.close();
    disabled.close();
    assertThatThrownBy(JmxMetricInsightSupervisor::getInstance)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("not initialized");

    AgentDistributionConfig.resetForTest();
    var enabled =
        JmxMetricInsightSupervisor.initialize(
            OpenTelemetry.noop(), DeclarativeConfigProperties.empty());
    cleanup.deferCleanup(enabled);
    disabled.close();

    assertThat(JmxMetricInsightSupervisor.getInstance()).isSameAs(enabled);
    assertThat(enabled.isEnabled()).isTrue();
  }

  @Test
  void failedCollectionCleanupStillRemovesRegistration() throws Exception {
    try (var services =
        mockConstruction(
            JmxMetricsService.class,
            (service, context) ->
                doThrow(new IllegalStateException("Cannot close collection"))
                    .when(service)
                    .close())) {
      var supervisor =
          JmxMetricInsightSupervisor.initialize(
              OpenTelemetry.noop(), DeclarativeConfigProperties.empty());
      cleanup.deferCleanup(supervisor);

      assertThatThrownBy(() -> supervisor.close())
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Cannot close collection");
      assertThatThrownBy(JmxMetricInsightSupervisor::getInstance)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("not initialized");
      assertThatThrownBy(supervisor::start)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("closed");

      supervisor.close();
      verify(services.constructed().get(0)).close();
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void installerPublishesAndRemovesSupervisor(boolean enabled) throws Exception {
    var sdk =
        createAutoConfiguredSdk(
            YAML.formatted(("default_enabled: " + enabled).indent(6)), tempDir, cleanup);
    try (var global = mockStatic(GlobalOpenTelemetry.class);
        var installer = new JmxMetricInsightInstaller()) {
      global.when(GlobalOpenTelemetry::get).thenReturn(sdk.getOpenTelemetrySdk());
      installer.afterAgent(sdk);
      var supervisor = JmxMetricInsightSupervisor.getInstance();
      assertThat(supervisor.isEnabled()).isEqualTo(enabled);

      installer.afterAgent(sdk);
      assertThat(JmxMetricInsightSupervisor.getInstance()).isSameAs(supervisor);
    }

    assertThatThrownBy(JmxMetricInsightSupervisor::getInstance)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("not initialized");
  }

  private static void assertDisabledSupervisor(
      OpenTelemetry openTelemetry, DeclarativeConfigProperties startup) throws Exception {
    try (var services = mockConstruction(JmxMetricsService.class)) {
      var supervisor = JmxMetricInsightSupervisor.initialize(openTelemetry, startup);
      cleanup.deferCleanup(supervisor);
      assertThat(JmxMetricInsightSupervisor.getInstance()).isSameAs(supervisor);
      assertThat(supervisor.isEnabled()).isFalse();

      // Later changes cannot replace the decision made at agent startup.
      AgentDistributionConfig.resetForTest();
      supervisor.start();
      for (var update : List.of(startup, remoteConfig(), DeclarativeConfigProperties.empty())) {
        assertThatThrownBy(() -> supervisor.applyConfiguration(update))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("JMX instrumentation was disabled at agent startup");
      }
      supervisor.start();

      assertThat(JmxMetricInsightSupervisor.getInstance()).isSameAs(supervisor);
      assertThat(supervisor.isEnabled()).isFalse();
      assertThat(services.constructed())
          .singleElement()
          .satisfies(service -> verifyNoInteractions(service));
    }
  }

  private static DeclarativeConfigProperties remoteConfig() {
    return DeclarativeConfiguration.toConfigProperties(Map.of("discovery", Map.of("delay", 2000L)));
  }
}
