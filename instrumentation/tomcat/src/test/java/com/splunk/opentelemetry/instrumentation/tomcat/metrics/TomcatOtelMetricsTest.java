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

package com.splunk.opentelemetry.instrumentation.tomcat.metrics;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.assertj.core.api.Assertions.assertThat;

import com.splunk.opentelemetry.testing.TestMetricsAccess;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.instrumentation.test.utils.PortUtils;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import java.nio.file.Files;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

@ExtendWith(AgentInstrumentationExtension.class)
public class TomcatOtelMetricsTest {
  private static final String INSTRUMENTATION_NAME =
      "com.splunk.javaagent.tomcat-thread-pool-metrics";

  @RegisterExtension
  static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  @AfterEach
  void clearMetrics() {
    TestMetricsAccess.clearMetrics();
  }

  @Test
  void shouldReportMetrics() throws Exception {
    // given
    var port = PortUtils.findOpenPort();
    var tomcatServer = new Tomcat();
    var baseDir = Files.createTempDirectory("tomcat").toFile();
    baseDir.deleteOnExit();
    tomcatServer.setBaseDir(baseDir.getAbsolutePath());
    tomcatServer.setPort(port);

    // when
    // explicitly trigger connector & protocol & endpoint creation
    tomcatServer.getConnector();
    tomcatServer.start();

    Attributes attributes =
        Attributes.of(
            stringKey("executor.type"), "tomcat", stringKey("executor.name"), "http-nio-" + port);

    assertThreadPoolGauge(
        "executor.threads", "The current number of threads in the pool.", attributes);
    assertThreadPoolGauge(
        "executor.threads.active", "The number of threads that are currently busy.", attributes);
    assertThreadPoolGauge(
        "executor.threads.idle", "The number of threads that are currently idle.", attributes);
    assertThreadPoolGauge(
        "executor.threads.core",
        "Core thread pool size - the number of threads that are always kept in the pool.",
        attributes);
    assertThreadPoolGauge(
        "executor.threads.max", "The maximum number of threads in the pool.", attributes);

    assertThreadPoolCounter(
        "executor.tasks.submitted",
        "The total number of tasks that were submitted to this executor.",
        attributes);
    assertThreadPoolCounter(
        "executor.tasks.completed",
        "The total number of tasks completed by this executor.",
        attributes);

    // when
    tomcatServer.stop();
    tomcatServer.destroy();

    // sleep exporter interval
    Thread.sleep(100);
    testing.clearData();
    Thread.sleep(100);

    // then
    assertNotMetric("executor.threads");
    assertNotMetric("executor.threads.active");
    assertNotMetric("executor.threads.idle");
    assertNotMetric("executor.threads.core");
    assertNotMetric("executor.threads.max");
    assertNotMetric("executor.tasks.submitted");
    assertNotMetric("executor.tasks.completed");
  }

  private static void assertThreadPoolGauge(
      String name, String description, Attributes attributes) {
    testing.waitAndAssertMetrics(
        "com.splunk.javaagent.tomcat-thread-pool-metrics",
        name,
        metrics ->
            metrics.anySatisfy(
                metric ->
                    OpenTelemetryAssertions.assertThat(metric)
                        .hasUnit("threads")
                        .hasDescription(description)
                        .hasDoubleGaugeSatisfying(
                            gauge ->
                                gauge.hasPointsSatisfying(
                                    point -> point.hasAttributes(attributes)))));
  }

  private static void assertThreadPoolCounter(
      String name, String description, Attributes attributes) {
    testing.waitAndAssertMetrics(
        "com.splunk.javaagent.tomcat-thread-pool-metrics",
        name,
        metrics ->
            metrics.anySatisfy(
                metric ->
                    OpenTelemetryAssertions.assertThat(metric)
                        .hasUnit("tasks")
                        .hasDescription(description)
                        .hasDoubleSumSatisfying(
                            sum ->
                                sum.hasPointsSatisfying(
                                    point -> point.hasAttributes(attributes)))));
  }

  private static void assertNotMetric(String name) {
    assertThat(testing.metrics())
        .filteredOn(
            metricData ->
                metricData.getInstrumentationScopeInfo().getName().equals(INSTRUMENTATION_NAME)
                    && metricData.getName().equals(name))
        .isEmpty();
  }
}
