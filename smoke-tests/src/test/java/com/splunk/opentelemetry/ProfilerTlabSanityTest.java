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

package com.splunk.opentelemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import io.opentelemetry.proto.trace.v1.Span;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

// TODO: use SmokeTest as base class
class ProfilerTlabSanityTest {

  private static final Logger logger = LoggerFactory.getLogger(ProfilerTlabSanityTest.class);

  private static final int BACKEND_PORT = 8080;

  private Network network;
  private GenericContainer<?> backend;
  private GenericContainer<?> collector;
  private GenericContainer<?> app;

  private TelemetryRetriever telemetryRetriever;

  @AfterEach
  void stopApp() {
    if (app != null) {
      app.stop();
    }
    if (backend != null) {
      backend.stop();
    }
    if (collector != null) {
      collector.stop();
    }
    if (network != null) {
      network.close();
    }
  }

  @Test
  void verifyThatTlabLogsHaveCorrectLinkage() {
    // skip this on windows for now
    assumeFalse(System.getProperty("os.name").toLowerCase().contains("windows"));

    // given
    network = Network.newNetwork();
    backend =
        new GenericContainer<>(
                DockerImageName.parse(
                    "ghcr.io/open-telemetry/java-test-containers:smoke-fake-backend-20210624.967200357"))
            .withExposedPorts(BACKEND_PORT)
            .waitingFor(Wait.forHttp("/health").forPort(BACKEND_PORT))
            .withNetwork(network)
            .withNetworkAliases("backend")
            .withLogConsumer(new Slf4jLogConsumer(logger));
    backend.start();

    telemetryRetriever =
        new TelemetryRetriever(OkHttpUtils.client(), backend.getMappedPort(BACKEND_PORT));

    collector =
        new GenericContainer<>(DockerImageName.parse("otel/opentelemetry-collector-contrib:latest"))
            .dependsOn(backend)
            .withNetwork(network)
            .withNetworkAliases("collector")
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("otel.yaml"), "/etc/otel.yaml")
            .withCommand("--config /etc/otel.yaml");
    collector.start();

    app =
        new GenericContainer<>(DockerImageName.parse("eclipse-temurin:11-alpine"))
            .withStartupTimeout(Duration.ofMinutes(1))
            .dependsOn(collector)
            .withNetwork(network)
            .withNetworkAliases("tlab-sanity-test-app")
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .withCopyFileToContainer(
                MountableFile.forHostPath(SmokeTest.agentPath), "/opentelemetry-javaagent.jar")
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("tlab-sanity-test-app/TlabSanityTestApp.java"),
                "/TlabSanityTestApp.java")
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("tlab-sanity-test-app/start.sh", 777),
                "/start.sh")
            .withEnv("OTEL_SERVICE_NAME", "tlab-sanity-test")
            .withEnv("OTEL_JAVAAGENT_DEBUG", "true")
            // add a span around the method that does lots of allocations
            .withEnv(
                "OTEL_INSTRUMENTATION_METHODS_INCLUDE", "TlabSanityTestApp[instrumentedMethod]")
            .withEnv("SPLUNK_PROFILER_ENABLED", "true")
            .withEnv("SPLUNK_PROFILER_TLAB_ENABLED", "true")
            .withEnv("SPLUNK_PROFILER_CALL_STACK_INTERVAL", "1000")
            .withEnv("SPLUNK_PROFILER_LOGS_ENDPOINT", "http://collector:4317")
            .withEnv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://collector:4317")
            .withCommand("/bin/sh", "-c", "/start.sh");

    // when
    app.start();

    // then
    await()
        // wait a bit for the application to finish
        .pollDelay(25, TimeUnit.SECONDS)
        .atMost(60, TimeUnit.SECONDS)
        .pollInterval(1, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              var traces = telemetryRetriever.waitForTraces();
              Span span =
                  traces
                      .getSpanStream()
                      .findFirst()
                      .orElseThrow(() -> new AssertionError("Did not find any span"));

              var logs = telemetryRetriever.waitForLogs();
              assertThat(logs.getTlabEvents())
                  .filteredOn(LogsInspector.hasThreadName("tlab-test-thread"))
                  // can't verify that all events match, but most of them definitely should
                  .anyMatch(
                      record ->
                          record.getTraceId().equals(span.getTraceId())
                              && record.getSpanId().equals(span.getSpanId()));
            });
  }
}
