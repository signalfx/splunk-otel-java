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

import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getConfigProvider;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.instrumentation.jmx.JmxTelemetry;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.javaagent.extension.instrumentation.internal.AgentDistributionConfig;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class JmxMetricsInitializerTest {
  private static final String YAML =
      """
      file_format: "1.2"
      meter_provider:
        readers:
          - periodic:
              interval: 50
              exporter:
                console:
      instrumentation/development:
        java:
          jmx:
            discovery:
              delay: 1000
      distribution:
        splunk:
          java:
            jmx:
              rules:
                - bean: splunk.test:type=CallMonitor
                  metricAttribute:
                    source: const(inline)
                  mapping:
                    CallCount:
                      metric: petclinic.repository.call.count
                      type: counter
                      unit: "{call}"
                      desc: Completed repository calls.
                    CallTime:
                      metric: petclinic.repository.call.duration.average
                      type: gauge
                      sourceUnit: ms
                      unit: s
                      desc: Average repository call duration.
      """;

  @RegisterExtension static final AutoCleanupExtension autoCleanup = AutoCleanupExtension.create();

  @TempDir Path tempDir;

  private final InMemoryMetricExporter exporter = InMemoryMetricExporter.create();
  private SplunkJmxMetricsInitializer initializer;

  @BeforeEach
  void setUp() {
    AgentDistributionConfig.resetForTest();
    autoCleanup.deferCleanup(AgentDistributionConfig::resetForTest);
    // Exercise the generated agent-listener SPI registration as well as the SDK customizer SPI.
    initializer =
        (SplunkJmxMetricsInitializer)
            ComponentLoader.loadList(
                    ComponentLoader.forClassLoader(getClass().getClassLoader()),
                    AgentListener.class)
                .stream()
                .filter(SplunkJmxMetricsInitializer.class::isInstance)
                .findFirst()
                .orElseThrow();
    autoCleanup.deferCleanup(initializer);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void exportsInlineRulesUsingTheConfiguredSdk(boolean registerAfterStartup) throws Exception {
    CallMonitor monitor = new CallMonitor();
    if (!registerAfterStartup) {
      registerMBean(monitor);
    }
    AutoConfiguredOpenTelemetrySdk sdk = createSdk(YAML);

    assertThat(AgentDistributionConfig.get().isInstrumentationEnabled("jmx")).isFalse();
    assertThat(
            getConfigProvider(sdk)
                .getInstrumentationConfig("jmx")
                .get("discovery")
                .getLong("delay"))
        .isEqualTo(1000);

    initializer.afterAgent(sdk);
    autoCleanup.deferCleanup(initializer);
    // Repeated startup must not register duplicate callbacks.
    initializer.afterAgent(sdk);
    if (registerAfterStartup) {
      registerMBean(monitor);
    }

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(() -> {
          MetricData count = lastMetric("petclinic.repository.call.count");
          assertThat(count.getInstrumentationScopeInfo().getName())
              .isEqualTo("io.opentelemetry.jmx");
          assertThat(count.getUnit()).isEqualTo("{call}");
          assertThat(count.getDescription()).isEqualTo("Completed repository calls.");
          assertThat(count.getLongSumData().isMonotonic()).isTrue();
          assertThat(count.getLongSumData().getPoints())
              .singleElement()
              .satisfies(point -> {
                assertThat(point.getValue()).isEqualTo(7);
                assertThat(point.getAttributes())
                    .isEqualTo(Attributes.builder().put("source", "inline").build());
              });
          MetricData time = lastMetric("petclinic.repository.call.duration.average");
          assertThat(time.getUnit()).isEqualTo("s");
          assertThat(time.getDoubleGaugeData().getPoints())
              .singleElement()
              .satisfies(point -> assertThat(point.getValue()).isEqualTo(0.25));
        });

    monitor.callCount = 11;
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() ->
            assertThat(lastMetric("petclinic.repository.call.count").getLongSumData().getPoints())
                .singleElement()
                .satisfies(point -> assertThat(point.getValue()).isEqualTo(11)));

    initializer.close();
    exporter.reset();
    sdk.getOpenTelemetrySdk()
        .getSdkMeterProvider()
        .forceFlush()
        .join(5, java.util.concurrent.TimeUnit.SECONDS);
    assertThat(jmxMetrics()).isEmpty();
  }

  @Test
  void remoteUpdatesUseTheStartupCollectionSupervisor() throws Exception {
    registerMBean(new CallMonitor());
    AutoConfiguredOpenTelemetrySdk sdk = createSdk(YAML);
    initializer.afterAgent(sdk);

    var supervisor = JmxMetricInsightSupervisor.getInstance();
    supervisor.applyConfiguration(
        DeclarativeConfiguration.toConfigProperties(
            Map.of(
                "rules",
                List.of(
                    Map.of(
                        "bean",
                        "splunk.test:type=CallMonitor",
                        "mapping",
                        Map.of(
                            "CallCount",
                            Map.of(
                                "metric", "petclinic.remote.call.count",
                                "type", "counter",
                                "unit", "{call}")))))));

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(() -> {
          exporter.reset();
          sdk.getOpenTelemetrySdk()
              .getSdkMeterProvider()
              .forceFlush()
              .join(5, java.util.concurrent.TimeUnit.SECONDS);
          assertThat(jmxMetrics())
              .extracting(MetricData::getName)
              .contains("petclinic.remote.call.count")
              .doesNotContain(
                  "petclinic.repository.call.count", "petclinic.repository.call.duration.average");
        });

    initializer.close();
    assertThatThrownBy(JmxMetricInsightSupervisor::getInstance)
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void appliesDistributionMetricFilters() throws Exception {
    registerMBean(new CallMonitor());
    AutoConfiguredOpenTelemetrySdk sdk =
        createSdk(
            YAML.replace(
                "\n      jmx:\n",
                "\n      jmx:\n        metrics:\n          included: [petclinic.*]\n          excluded: [petclinic.repository.call.duration.*]\n"));

    initializer.afterAgent(sdk);
    autoCleanup.deferCleanup(initializer);

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(() -> assertThat(jmxMetrics()).isNotEmpty());
    assertThat(jmxMetrics())
        .extracting(MetricData::getName)
        .containsOnly("petclinic.repository.call.count");
  }

  @Test
  void supportsSharedRulePropertiesAndEmptyMetricMappings() throws Exception {
    registerMBean(new CallMonitor());
    AutoConfiguredOpenTelemetrySdk sdk =
        createSdk(
            """
            file_format: "1.2"
            meter_provider:
              readers:
                - periodic:
                    interval: 50
                    exporter:
                      console:
            distribution:
              splunk:
                java:
                  jmx:
                    rules:
                      - beans: ["splunk.test:type=CallMonitor"]
                        prefix: petclinic.
                        type: gauge
                        unit: "{call}"
                        dropNegativeValues: true
                        mapping:
                          CallCount:
            """);

    initializer.afterAgent(sdk);
    autoCleanup.deferCleanup(initializer);

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(() ->
            assertThat(lastMetric("petclinic.CallCount").getLongGaugeData().getPoints())
                .singleElement()
                .satisfies(point -> assertThat(point.getValue()).isEqualTo(7)));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "file_format: '1.2'",
        "file_format: '1.2'\ndistribution:\n  splunk:\n    java:\n      other: {}"
      })
  void doesNotStartWithoutDistributionJmxNode(String yaml) throws Exception {
    AutoConfiguredOpenTelemetrySdk sdk = createSdk(yaml);
    try (var jmx = mockStatic(JmxTelemetry.class)) {
      initializer.afterAgent(sdk);
      jmx.verifyNoInteractions();
    }
  }

  @Test
  void respectsDisabledJmxInstrumentation() throws Exception {
    AutoConfiguredOpenTelemetrySdk sdk =
        createSdk(
            YAML.replace(
                "distribution:\n",
                """
                distribution:
                  javaagent:
                    instrumentation:
                      disabled: [jmx]
                """));
    try (var jmx = mockStatic(JmxTelemetry.class)) {
      initializer.afterAgent(sdk);
      jmx.verifyNoInteractions();
    }
  }

  @Test
  void startsBuiltinsForAnEmptyDistributionNode() throws Exception {
    var server = ManagementFactory.getPlatformMBeanServer();
    var name = new ObjectName("splunk.test:type=BuiltIn");
    server.registerMBean(new StandardMBean(new CallMonitor(), CallMonitorMBean.class), name);
    autoCleanup.deferCleanup(() -> server.unregisterMBean(name));
    AutoConfiguredOpenTelemetrySdk sdk =
        createSdk(
            """
            file_format: "1.2"
            meter_provider:
              readers:
                - periodic:
                    interval: 50
                    exporter:
                      console:
            distribution:
              splunk:
                java:
                  jmx:
            """);

    assertThat(AgentDistributionConfig.get().isInstrumentationEnabled("jmx")).isFalse();
    initializer.afterAgent(sdk);
    autoCleanup.deferCleanup(initializer);

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(() ->
            assertThat(lastMetric("test.jmx.builtin.stable").getLongSumData().getPoints())
                .singleElement()
                .satisfies(point -> assertThat(point.getValue()).isEqualTo(7)));
  }

  @Test
  void doesNotStartForEnvironmentConfiguration() {
    AutoConfiguredOpenTelemetrySdk sdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .addPropertiesSupplier(() ->
                Map.of(
                    "otel.traces.exporter",
                    "none",
                    "otel.metrics.exporter",
                    "none",
                    "otel.logs.exporter",
                    "none"))
            .disableShutdownHook()
            .build();
    autoCleanup.deferCleanup(sdk.getOpenTelemetrySdk());
    try (var jmx = mockStatic(JmxTelemetry.class)) {
      initializer.afterAgent(sdk);
      jmx.verifyNoInteractions();
    }
  }

  @Test
  void reportsInvalidRulesWithoutFailingAgentStartup() throws Exception {
    AutoConfiguredOpenTelemetrySdk sdk = createSdk(YAML.replace("type: counter", "type: invalid"));
    Logger logger = Logger.getLogger(SplunkJmxMetricsInitializer.class.getName());
    Handler handler = mock(Handler.class);
    logger.addHandler(handler);
    autoCleanup.deferCleanup(() -> logger.removeHandler(handler));

    initializer.afterAgent(sdk);

    ArgumentCaptor<LogRecord> record = ArgumentCaptor.forClass(LogRecord.class);
    verify(handler).publish(record.capture());
    assertThat(record.getValue().getMessage()).contains("distribution.splunk.java.jmx");
    assertThat(record.getValue().getThrown()).isInstanceOf(IllegalArgumentException.class);
    assertThat(jmxMetrics()).isEmpty();
  }

  private AutoConfiguredOpenTelemetrySdk createSdk(String yaml) throws Exception {
    Path configFile = tempDir.resolve("otel.yaml");
    Files.writeString(configFile, yaml);
    ComponentLoader delegate = ComponentLoader.forClassLoader(getClass().getClassLoader());
    ComponentLoader loader =
        new ComponentLoader() {
          @Override
          public <T> Iterable<T> load(Class<T> spiClass) {
            List<T> providers = new ArrayList<>(ComponentLoader.loadList(delegate, spiClass));
            if (spiClass == DeclarativeConfigurationCustomizerProvider.class) {
              DeclarativeConfigurationCustomizerProvider customizer =
                  builder ->
                      builder.addMetricExporterCustomizer(
                          MetricExporter.class, (ignored, config) -> exporter);
              providers.add(spiClass.cast(customizer));
            }
            return providers;
          }
        };
    System.setProperty("otel.config.file", configFile.toString());
    try {
      AutoConfiguredOpenTelemetrySdk sdk =
          AutoConfiguredOpenTelemetrySdk.builder()
              .setComponentLoader(loader)
              .addPropertiesSupplier(() -> Map.of("otel.config.file", configFile.toString()))
              .disableShutdownHook()
              .build();
      autoCleanup.deferCleanup(sdk.getOpenTelemetrySdk());
      return sdk;
    } finally {
      System.clearProperty("otel.config.file");
    }
  }

  private void registerMBean(CallMonitor monitor) throws Exception {
    var server = ManagementFactory.getPlatformMBeanServer();
    var name = new ObjectName("splunk.test:type=CallMonitor");
    server.registerMBean(new StandardMBean(monitor, CallMonitorMBean.class), name);
    autoCleanup.deferCleanup(() -> server.unregisterMBean(name));
  }

  private MetricData lastMetric(String name) {
    List<MetricData> metrics =
        exporter.getFinishedMetricItems().stream()
            .filter(metric -> metric.getName().equals(name))
            .toList();
    assertThat(metrics).isNotEmpty();
    return metrics.get(metrics.size() - 1);
  }

  private List<MetricData> jmxMetrics() {
    return exporter.getFinishedMetricItems().stream()
        .filter(
            metric -> metric.getInstrumentationScopeInfo().getName().equals("io.opentelemetry.jmx"))
        .toList();
  }

  public interface CallMonitorMBean {
    long getCallCount();

    long getCallTime();
  }

  public static class CallMonitor implements CallMonitorMBean {
    volatile long callCount = 7;

    @Override
    public long getCallCount() {
      return callCount;
    }

    @Override
    public long getCallTime() {
      return 250;
    }
  }
}
