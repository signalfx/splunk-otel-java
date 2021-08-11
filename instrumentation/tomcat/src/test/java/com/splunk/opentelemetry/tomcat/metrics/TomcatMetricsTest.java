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

package com.splunk.opentelemetry.tomcat.metrics;

import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ThreadPoolSemanticConventions.TASKS_COMPLETED;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ThreadPoolSemanticConventions.TASKS_SUBMITTED;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ThreadPoolSemanticConventions.THREADS_ACTIVE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ThreadPoolSemanticConventions.THREADS_CORE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ThreadPoolSemanticConventions.THREADS_CURRENT;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ThreadPoolSemanticConventions.THREADS_IDLE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ThreadPoolSemanticConventions.THREADS_MAX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.splunk.opentelemetry.testing.MeterId;
import com.splunk.opentelemetry.testing.TestMetricsAccess;
import io.opentelemetry.instrumentation.test.utils.PortUtils;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(AgentInstrumentationExtension.class)
public class TomcatMetricsTest {

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

    // then
    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThreadPoolMetrics("http-nio-" + port));

    // when
    tomcatServer.stop();
    tomcatServer.destroy();

    // then
    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(TomcatMetricsTest::assertNoThreadPoolMetrics);
  }

  private static void assertThreadPoolMetrics(String name) {
    var tags = Map.of("executor.name", name, "executor.type", "tomcat");

    assertThat(TestMetricsAccess.getMeters())
        .containsExactlyInAnyOrder(
            MeterId.from(THREADS_CURRENT, tags),
            MeterId.from(THREADS_ACTIVE, tags),
            MeterId.from(THREADS_IDLE, tags),
            MeterId.from(THREADS_CORE, tags),
            MeterId.from(THREADS_MAX, tags),
            MeterId.from(TASKS_SUBMITTED, tags),
            MeterId.from(TASKS_COMPLETED, tags));
  }

  private static void assertNoThreadPoolMetrics() {
    assertThat(TestMetricsAccess.getMeterNames())
        .doesNotContain(
            THREADS_CURRENT.name(),
            THREADS_ACTIVE.name(),
            THREADS_IDLE.name(),
            THREADS_CORE.name(),
            THREADS_MAX.name(),
            TASKS_SUBMITTED.name(),
            TASKS_COMPLETED.name());
  }
}
