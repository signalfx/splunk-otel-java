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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.instrumentation.config.bridge.DeclarativeConfigBridge;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.javaagent.extension.instrumentation.internal.AgentDistributionConfig;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import java.io.ByteArrayInputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class JmxMetricInsightSupervisorTest {
  private static final String RULES =
      """
      rules:
        - bean: splunk.test:type=SupervisedMonitor
          mapping:
            CallCount:
              metric: %s
              type: counter
              unit: "{call}"
      """;

  @RegisterExtension static final AutoCleanupExtension cleanup = AutoCleanupExtension.create();

  @TempDir Path tempDir;

  private final InMemoryMetricReader reader = InMemoryMetricReader.create();
  private OpenTelemetrySdk sdk;

  @BeforeEach
  void setUp() throws Exception {
    AgentDistributionConfig.resetForTest();
    cleanup.deferCleanup(AgentDistributionConfig::resetForTest);
    sdk =
        OpenTelemetrySdk.builder()
            .setMeterProvider(SdkMeterProvider.builder().registerMetricReader(reader).build())
            .build();
    cleanup.deferCleanup(sdk);
    var server = ManagementFactory.getPlatformMBeanServer();
    var name = new ObjectName("splunk.test:type=SupervisedMonitor");
    server.registerMBean(new StandardMBean(() -> 7, CallMonitorMBean.class), name);
    cleanup.deferCleanup(() -> server.unregisterMBean(name));
  }

  @Test
  void replacesEntireConfigurationAndClosesCollection() throws Exception {
    var supervisor =
        newSupervisor(
            RULES.formatted("test.supervisor.original")
                + "metrics:\n  included: [test.supervisor.*]\n");
    supervisor.start();
    awaitMetric("test.supervisor.original");

    supervisor.applyConfiguration(config(RULES.formatted("test.supervisor.updated")));
    awaitMetric("test.supervisor.updated");

    // The complete replacement drops the startup inclusion filter.
    supervisor.applyConfiguration(config(RULES.formatted("other.allowed")));
    awaitMetric("other.allowed");
    supervisor.applyConfiguration(config(RULES.formatted("test.supervisor.restored")));
    awaitMetric("test.supervisor.restored");

    supervisor.applyConfiguration(DeclarativeConfigProperties.empty());
    assertThat(metrics()).isEmpty();

    supervisor.close();
    assertThat(metrics()).isEmpty();
    assertThatThrownBy(
            () -> supervisor.applyConfiguration(config(RULES.formatted("test.supervisor.closed"))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("closed");
  }

  @Test
  void invalidRulesLeavePreviousCollectionActiveAndCanBeCorrected() throws Exception {
    var supervisor = newSupervisor(RULES.formatted("test.supervisor.original"));
    supervisor.start();
    awaitMetric("test.supervisor.original");

    assertThatThrownBy(() ->
            supervisor.applyConfiguration(
                config(
                    RULES
                        .formatted("test.supervisor.invalid")
                        .replace("type: counter", "type: invalid"))))
        .isInstanceOf(IllegalArgumentException.class);
    awaitMetric("test.supervisor.original");

    supervisor.applyConfiguration(config(RULES.formatted("test.supervisor.corrected")));
    awaitMetric("test.supervisor.corrected");
  }

  @Test
  void remoteUpdateCanRepairInvalidStartupRules() throws Exception {
    var supervisor =
        newSupervisor(
            RULES.formatted("test.supervisor.invalid").replace("type: counter", "type: invalid"));
    assertThatThrownBy(supervisor::start).isInstanceOf(IllegalArgumentException.class);

    supervisor.applyConfiguration(config(RULES.formatted("test.supervisor.corrected")));

    awaitMetric("test.supervisor.corrected");
  }

  @Test
  void identicalUpdatesDoNotRestartCollection() throws Exception {
    JmxMetricsService service = mock(JmxMetricsService.class);
    var startup = config("discovery:\n  delay: 1000\nmetrics:\n  excluded: [other.*]\n");
    var supervisor = new JmxMetricInsightSupervisor(service, startup);
    var update = config("discovery:\n  delay: 2000\n");

    supervisor.applyConfiguration(update);
    supervisor.applyConfiguration(update);
    supervisor.applyConfiguration(DeclarativeConfigProperties.empty());
    supervisor.applyConfiguration(DeclarativeConfigProperties.empty());

    ArgumentCaptor<DeclarativeConfigProperties> configuration =
        ArgumentCaptor.forClass(DeclarativeConfigProperties.class);
    verify(service, times(2)).reconfigure(configuration.capture());
    verifyNoMoreInteractions(service);
    assertThat(DeclarativeConfigProperties.toMap(configuration.getAllValues().get(0)))
        .isEqualTo(Map.of("discovery", Map.of("delay", 2000L)));
    assertThat(configuration.getAllValues().get(1).getPropertyKeys()).isEmpty();
  }

  @Test
  void canRetryPreviousSettingsAfterReplacementFailsToStart() throws Exception {
    JmxMetricsService service = mock(JmxMetricsService.class);
    when(service.start(any())).thenReturn(true);
    doThrow(new IllegalStateException("Cannot start collection"))
        .doNothing()
        .when(service)
        .reconfigure(any());
    var startup = config("discovery:\n  delay: 1000\n");
    var supervisor = new JmxMetricInsightSupervisor(service, startup);
    supervisor.start();

    assertThatThrownBy(() -> supervisor.applyConfiguration(config("discovery:\n  delay: 2000\n")))
        .isInstanceOf(IllegalStateException.class);

    supervisor.applyConfiguration(startup);

    verify(service, times(2)).reconfigure(any());
  }

  @Test
  void unmanagedStartupRejectsRemoteUpdates() {
    JmxMetricsService service = mock(JmxMetricsService.class);
    var supervisor = new JmxMetricInsightSupervisor(service, null);

    supervisor.start();

    assertThatThrownBy(() -> supervisor.applyConfiguration(DeclarativeConfigProperties.empty()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("enabled at agent startup");
    verifyNoInteractions(service);
  }

  @Test
  void preservesBridgedStartupSettings() {
    var startup =
        bridgedConfig(
            Map.of(
                "otel.jmx.config", "first.yaml,second.yaml",
                "otel.jmx.discovery.delay", "1234ms",
                "otel.jmx.metrics.included", "test.*",
                "otel.jmx.metrics.excluded", "test.excluded",
                "otel.jmx.metrics.experimental.included", "tomcat.*"));
    assertThat(startup.getPropertyKeys()).isEmpty();
    JmxMetricsService service = mock(JmxMetricsService.class);
    var supervisor = new JmxMetricInsightSupervisor(service, startup);
    cleanup.deferCleanup(supervisor);

    supervisor.start();

    var configuration = ArgumentCaptor.forClass(DeclarativeConfigProperties.class);
    verify(service).start(configuration.capture());
    var collected = configuration.getValue();
    assertThat(collected.getScalarList("config", String.class))
        .containsExactly("first.yaml", "second.yaml");
    assertThat(collected.get("discovery").getLong("delay")).isEqualTo(1234);
    assertThat(collected.get("metrics").getScalarList("included", String.class))
        .containsExactly("test.*");
    assertThat(collected.get("metrics").getScalarList("excluded", String.class))
        .containsExactly("test.excluded");
    assertThat(collected.get("metrics").get("experimental").getScalarList("included", String.class))
        .containsExactly("tomcat.*");
  }

  @Test
  void preservesBridgedDiscoveryDelayFallbackToExportInterval() {
    JmxMetricsService service = mock(JmxMetricsService.class);
    var supervisor =
        new JmxMetricInsightSupervisor(
            service, bridgedConfig(Map.of("otel.metric.export.interval", "7s")));
    cleanup.deferCleanup(supervisor);

    supervisor.start();

    var configuration = ArgumentCaptor.forClass(DeclarativeConfigProperties.class);
    verify(service).start(configuration.capture());
    assertThat(configuration.getValue().get("discovery").getLong("delay")).isEqualTo(7000);
  }

  @Test
  void emptyRemoteUpdateResetsBridgedStartupAndRepeatedUpdatesDoNotRestart() throws Exception {
    JmxMetricsService service = mock(JmxMetricsService.class);
    when(service.start(any())).thenReturn(true);
    var supervisor =
        new JmxMetricInsightSupervisor(
            service, bridgedConfig(Map.of("otel.jmx.config", "startup.yaml")));
    cleanup.deferCleanup(supervisor);
    supervisor.start();

    supervisor.applyConfiguration(DeclarativeConfigProperties.empty());
    supervisor.applyConfiguration(DeclarativeConfigProperties.empty());

    var configuration = ArgumentCaptor.forClass(DeclarativeConfigProperties.class);
    verify(service).reconfigure(configuration.capture());
    assertThat(configuration.getValue().getPropertyKeys()).isEmpty();
  }

  @Test
  void collectsFileRulesAndAppliesFiltersFromBridgedStartup() throws Exception {
    Path rules = tempDir.resolve("jmx.yaml");
    Files.writeString(
        rules,
        RULES.formatted("test.environment.keep")
            + RULES.formatted("test.environment.excluded").replace("rules:\n", "")
            + RULES.formatted("other.filtered").replace("rules:\n", ""));
    var supervisor =
        new JmxMetricInsightSupervisor(
            new JmxMetricsService(sdk),
            bridgedConfig(
                Map.of(
                    "otel.jmx.config", rules.toString(),
                    "otel.jmx.discovery.delay", "50ms",
                    "otel.jmx.metrics.included", "test.environment.*",
                    "otel.jmx.metrics.excluded", "test.environment.excluded")));
    cleanup.deferCleanup(supervisor);

    supervisor.start();
    awaitMetric("test.environment.keep");

    supervisor.applyConfiguration(config(RULES.formatted("test.remote.replacement")));
    awaitMetric("test.remote.replacement");

    supervisor.close();
    assertThat(metrics()).isEmpty();
  }

  private static DeclarativeConfigProperties bridgedConfig(Map<String, String> properties) {
    return DeclarativeConfigBridge.createInstrumentationConfig(
            DefaultConfigProperties.createFromMap(properties))
        .getInstrumentationConfig("jmx");
  }

  private JmxMetricInsightSupervisor newSupervisor(String configuration) {
    var supervisor =
        new JmxMetricInsightSupervisor(new JmxMetricsService(sdk), config(configuration));
    cleanup.deferCleanup(supervisor);
    return supervisor;
  }

  private void awaitMetric(String name) {
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(() ->
            assertThat(metrics())
                .singleElement()
                .satisfies(metric -> {
                  assertThat(metric.getName()).isEqualTo(name);
                  assertThat(metric.getLongSumData().getPoints())
                      .singleElement()
                      .satisfies(point -> assertThat(point.getValue()).isEqualTo(7));
                }));
  }

  private List<MetricData> metrics() {
    return reader.collectAllMetrics().stream()
        .filter(
            metric -> metric.getInstrumentationScopeInfo().getName().equals("io.opentelemetry.jmx"))
        .toList();
  }

  private static DeclarativeConfigProperties config(String yaml) {
    return DeclarativeConfiguration.toConfigProperties(
        new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
  }

  public interface CallMonitorMBean {
    long getCallCount();
  }
}
