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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.splunk.opentelemetry.helper.TestImage;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.proto.trace.v1.Span;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

abstract class ProfilerAllocationSanityTest extends SmokeTest {

  private GenericContainer<?> app;

  public static class TestPprof extends ProfilerAllocationSanityTest {}

  @AfterEach
  void stopApp() {
    if (app != null) {
      app.stop();
    }
  }

  @Test
  void verifyThatTlabLogsHaveCorrectLinkage() {
    var image = TestImage.linuxImage("eclipse-temurin:11-alpine");
    assumeTrue(containerManager.isImageCompatible(image));

    // given
    app =
        containerManager
            .newContainer(image)
            .withStartupTimeout(Duration.ofMinutes(1))
            .withNetworkAliases("tlab-sanity-test-app")
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
            .withEnv("SPLUNK_PROFILER_MEMORY_ENABLED", "true")
            .withEnv("SPLUNK_PROFILER_CALL_STACK_INTERVAL", "1000")
            .withEnv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://collector:4318")
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
              var traces = waitForTraces();
              Span span =
                  traces
                      .getSpanStream()
                      .findFirst()
                      .orElseThrow(() -> new AssertionError("Did not find any span"));

              var logs = waitForLogs();
              assertThat(logs.getMemorySamples())
                  .filteredOn(LogsInspector.hasThreadName("tlab-test-thread"))
                  // can't verify that all events match, but most of them definitely should
                  .anyMatch(
                      sample ->
                          sample
                                  .getTraceId()
                                  .equals(TraceId.fromBytes(span.getTraceId().toByteArray()))
                              && sample
                                  .getSpanId()
                                  .equals(SpanId.fromBytes(span.getSpanId().toByteArray())));
            });
  }
}
