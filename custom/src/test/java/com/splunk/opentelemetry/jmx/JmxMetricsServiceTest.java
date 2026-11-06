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
import java.time.Duration;
import java.util.List;
import java.util.Map;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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
    assertThat(service.start(empty(), empty())).isFalse();

    var firstRules = config(RULES.formatted("petclinic.first"));
    assertThat(service.start(firstRules, empty())).isTrue();
    assertThat(service.start(firstRules, empty())).isTrue();
    awaitMetric("petclinic.first");

    service.close();
    service.close();
    assertThat(jmxMetrics()).isEmpty();

    assertThatThrownBy(() ->
            service.start(
                config(
                    RULES.formatted("petclinic.invalid").replace("type: counter", "type: invalid")),
                empty()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(jmxMetrics()).isEmpty();

    assertThat(service.start(config(RULES.formatted("petclinic.second")), empty())).isTrue();
    awaitMetric("petclinic.second");
    assertThat(jmxMetrics()).extracting(MetricData::getName).containsExactly("petclinic.second");
  }

  @Test
  void instancesHaveIndependentRulesSettingsAndLifecycles() throws Exception {
    JmxMetricsService first = newService();
    JmxMetricsService second = newService();
    var settings =
        config(
            """
            discovery:
              delay: 1000
            metrics:
              included: [petclinic.second]
            """);

    assertThat(first.start(config(RULES.formatted("petclinic.first")), empty())).isTrue();
    assertThat(
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
                        """),
                settings))
        .isTrue();

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

    assertThat(newService().start(rules, empty())).isTrue();

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
}
