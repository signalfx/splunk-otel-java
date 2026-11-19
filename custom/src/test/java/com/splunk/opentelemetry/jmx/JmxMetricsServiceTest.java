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

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.api.incubator.config.DeclarativeConfigProperties.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
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

class JmxMetricsServiceTest {
  private static final String RULES =
      """
      rules:
        - bean: splunk.test:type=DynamicCallMonitor
          mapping:
            CallCount:
              metric: %s
              type: counter
              unit: "{call}"
      """;

  @RegisterExtension static final AutoCleanupExtension autoCleanup = AutoCleanupExtension.create();

  private final InMemoryMetricReader reader = InMemoryMetricReader.create();
  private OpenTelemetrySdk sdk;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() throws Exception {
    // No agent lifecycle, declarative SDK startup, or global configuration is required.
    sdk =
        OpenTelemetrySdk.builder()
            .setMeterProvider(SdkMeterProvider.builder().registerMetricReader(reader).build())
            .build();
    autoCleanup.deferCleanup(sdk);

    var server = ManagementFactory.getPlatformMBeanServer();
    var name = new ObjectName("splunk.test:type=DynamicCallMonitor");
    server.registerMBean(new StandardMBean(() -> 7, CallMonitorMBean.class), name);
    autoCleanup.deferCleanup(() -> server.unregisterMBean(name));
  }

  @Test
  void startsAfterSdkCreationAndCanRestartWithDifferentRules() throws Exception {
    JmxMetricsService service = newService();
    service.start(empty());
    service.close();

    var firstRules = config(RULES.formatted("petclinic.first"));
    service.start(firstRules);
    service.start(firstRules);
    awaitMetric("petclinic.first");

    service.close();
    service.close();
    assertThat(jmxMetrics()).isEmpty();

    assertThatThrownBy(() ->
            service.start(
                config(
                    RULES
                        .formatted("petclinic.invalid")
                        .replace("type: counter", "type: invalid"))))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(jmxMetrics()).isEmpty();

    service.start(config(RULES.formatted("petclinic.second")));
    awaitMetric("petclinic.second");
    assertThat(jmxMetrics()).extracting(MetricData::getName).containsExactly("petclinic.second");
  }

  @Test
  void instancesHaveIndependentRulesSettingsAndLifecycles() throws Exception {
    JmxMetricsService first = newService();
    JmxMetricsService second = newService();
    first.start(config(RULES.formatted("petclinic.first")));
    second.start(
        config(
            RULES.formatted("petclinic.second")
                + """
                  - bean: splunk.test:type=DynamicCallMonitor
                    mapping:
                      CallCount:
                        metric: petclinic.filtered
                        type: counter
                        unit: "{call}"
                """
                + """
                discovery:
                  delay: 1000
                metrics:
                  included: [petclinic.second]
                """));

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(() ->
            assertThat(jmxMetrics())
                .extracting(MetricData::getName)
                .containsExactlyInAnyOrder("petclinic.first", "petclinic.second"));

    first.close();
    assertThat(jmxMetrics())
        .singleElement()
        .satisfies(metric -> {
          assertThat(metric.getName()).isEqualTo("petclinic.second");
          assertThat(metric.getLongSumData().getPoints())
              .singleElement()
              .satisfies(point -> assertThat(point.getValue()).isEqualTo(7));
        });
  }

  @Test
  void preservesEscapedStringsAndUnicodeInRuleDescriptionsAndAttributes() {
    String description = "Calls \"per repository\"\\path\n\tżółw";
    String label = "Repository \"calls\"\\path żółw";
    var rules =
        DeclarativeConfiguration.toConfigProperties(
            Map.of(
                "rules",
                List.of(
                    Map.of(
                        "bean", "splunk.test:type=DynamicCallMonitor",
                        "metricAttribute", Map.of("label", "const(" + label + ")"),
                        "mapping",
                            Map.of(
                                "CallCount",
                                Map.of(
                                    "metric", "petclinic.escaped",
                                    "type", "counter",
                                    "unit", "{call}",
                                    "desc", description))))));

    newService().start(rules);

    awaitMetric("petclinic.escaped");
    assertThat(jmxMetrics())
        .singleElement()
        .satisfies(metric -> {
          assertThat(metric.getDescription()).isEqualTo(description);
          assertThat(metric.getLongSumData().getPoints())
              .singleElement()
              .satisfies(point ->
                  assertThat(point.getAttributes().get(stringKey("label"))).isEqualTo(label));
        });
  }

  @Test
  void loadsExternalAndInlineRulesWhileSkippingBadFiles() throws Exception {
    Path valid = tempDir.resolve("valid.yaml");
    Path invalid = tempDir.resolve("invalid.yaml");
    Files.writeString(valid, RULES.formatted("petclinic.file"));
    Files.writeString(invalid, "rules: [not-a-rule]");
    var rules =
        config(
            RULES.formatted("petclinic.inline")
                + """
                config:
                  - "%s"
                  - "%s"
                  - "%s"
                """
                    .formatted(tempDir.resolve("missing.yaml"), invalid, valid));

    newService().start(rules);

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(() ->
            assertThat(jmxMetrics())
                .extracting(MetricData::getName)
                .containsExactlyInAnyOrder("petclinic.file", "petclinic.inline"));
  }

  @Test
  void matchesUpstreamStableDefaultsAndExperimentalOptIn() throws Exception {
    var server = ManagementFactory.getPlatformMBeanServer();
    var stable = new ObjectName("splunk.test:type=BuiltIn");
    var unstable = new ObjectName("Catalina:type=GlobalRequestProcessor,name=custom-test");
    server.registerMBean(new StandardMBean(() -> 7, CallMonitorMBean.class), stable);
    server.registerMBean(new StandardMBean(() -> 7, TomcatMBean.class), unstable);
    autoCleanup.deferCleanup(() -> server.unregisterMBean(stable));
    autoCleanup.deferCleanup(() -> server.unregisterMBean(unstable));
    JmxMetricsService service = newService();

    service.start(empty());
    awaitMetric("test.jmx.builtin.stable");
    // JVM rules are excluded and experimental metrics are not collected by default.
    assertThat(jmxMetrics())
        .extracting(MetricData::getName)
        .containsExactly("test.jmx.builtin.stable");

    service.close();
    service.start(
        config(
            """
            metrics:
              experimental:
                included: [tomcat.request.count]
            """));
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(() ->
            assertThat(jmxMetrics())
                .extracting(MetricData::getName)
                .containsExactlyInAnyOrder("test.jmx.builtin.stable", "tomcat.request.count"));

    service.close();
    service.start(
        config(
            """
            metrics:
              experimental:
                included: [tomcat.*]
              excluded: [tomcat.*]
            """));
    awaitMetric("test.jmx.builtin.stable");
  }

  private JmxMetricsService newService() {
    JmxMetricsService service = new JmxMetricsService(sdk);
    autoCleanup.deferCleanup(service);
    return service;
  }

  private void awaitMetric(String name) {
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(() ->
            assertThat(jmxMetrics())
                .singleElement()
                .satisfies(metric -> {
                  assertThat(metric.getName()).isEqualTo(name);
                  assertThat(metric.getLongSumData().getPoints())
                      .singleElement()
                      .satisfies(point -> assertThat(point.getValue()).isEqualTo(7));
                }));
  }

  private List<MetricData> jmxMetrics() {
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

  public interface TomcatMBean {
    long getrequestCount();
  }
}
