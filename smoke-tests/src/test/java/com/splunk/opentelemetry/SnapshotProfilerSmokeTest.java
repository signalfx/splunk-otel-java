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

import static com.splunk.opentelemetry.LogsInspector.getStringAttr;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.DATA_FORMAT;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.FRAME_COUNT;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.INSTRUMENTATION_SOURCE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.PPROF_GZIP_BASE64;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.splunk.opentelemetry.helper.TargetContainerBuilder;
import com.splunk.opentelemetry.helper.TargetWaitStrategy;
import com.splunk.opentelemetry.helper.TestContainerManager;
import com.splunk.opentelemetry.helper.TestImage;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class SnapshotProfilerSmokeTest {

  private static final Logger logger = LoggerFactory.getLogger(SnapshotProfilerSmokeTest.class);
  private static final okhttp3.OkHttpClient client = OkHttpUtils.client();
  private static final int PETCLINIC_PORT = 9966;

  private TestContainerManager containerManager;
  private TelemetryRetriever telemetryRetriever;
  private final String jdkVersion;

  SnapshotProfilerSmokeTest(String jdkVersion) {
    this.jdkVersion = jdkVersion;
  }

  public static class TestJdk8 extends SnapshotProfilerSmokeTest {
    TestJdk8() {
      super("8");
    }
  }

  public static class TestJdk11 extends SnapshotProfilerSmokeTest {
    TestJdk11() {
      super("11");
    }
  }

  public static class TestJdk17 extends SnapshotProfilerSmokeTest {
    TestJdk17() {
      super("17");
    }
  }

  @BeforeAll
  void setupEnvironment() {
    containerManager = SmokeTest.createContainerManager();
    containerManager.startEnvironment();

    telemetryRetriever = new TelemetryRetriever(client, containerManager.getBackendMappedPort());

    startPetclinic();
  }

  @AfterAll
  void teardown() {
    containerManager.stopEnvironment();
  }

  String getPetclinicImageName() {
    String prefix = "ghcr.io/signalfx/splunk-otel-java/profiling-petclinic-base-";
    String suffix = "-jdk" + jdkVersion + ":latest";

    TestImage linuxImage = TestImage.linuxImage(prefix + "linux" + suffix);
    // Allows testing with Linux containers on Windows if that option is enabled
    if (containerManager.isImageCompatible(linuxImage)) {
      return linuxImage.imageName;
    }
    return prefix + "windows" + suffix;
  }

  @Test
  void verifyIngestedLogContent() throws Exception {
    await()
        .atMost(2, TimeUnit.MINUTES)
        .pollInterval(1, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(countThreadDumpsInFakeBackend()).isGreaterThan(0));

    LogsInspector logs = telemetryRetriever.waitForLogs();

    assertThat(logs.getLogStream())
        .allMatch(log -> "otel.profiling".equals(getStringAttr(log, SOURCE_TYPE)))
        .allMatch(log -> "snapshot".equals(getStringAttr(log, INSTRUMENTATION_SOURCE)))
        .allMatch(log -> PPROF_GZIP_BASE64.equals(getStringAttr(log, DATA_FORMAT)));

    assertThat(logs.getLogStream())
        .allMatch(log -> LogsInspector.getLongAttr(log, FRAME_COUNT) > 0);
  }

  private long countThreadDumpsInFakeBackend() throws IOException, InterruptedException {
    return telemetryRetriever.waitForLogs().getCpuSamples().size();
  }

  private void generateSomeSpans() throws Exception {
    logger.info("Generating some spans...");
    int port = containerManager.getTargetMappedPort(PETCLINIC_PORT);
    for (int i = 0; i < 1000; i++) {
      doGetRequest("http://localhost:" + port + "/petclinic/api/vets");
      doGetRequest("http://localhost:" + port + "/petclinic/api/visits");
    }
  }

  private static void doGetRequest(String url) throws Exception {
    Request request = new Request.Builder().url(url).build();
    try (Response response = client.newCall(request).execute()) {
      assertEquals(200, response.code());
    }
  }

  private void startPetclinic() {
    containerManager.startTarget(
        new TargetContainerBuilder(getPetclinicImageName())
            .withTargetPort(PETCLINIC_PORT)
            .withNetworkAliases("petclinic")
            .withAgentPath(SmokeTest.agentPath)
            .withEntrypoint("java")
            .withWaitStrategy(
                new TargetWaitStrategy.Http(Duration.ofMinutes(5), "/petclinic/api/vets"))
            .withUseDefaultAgentConfiguration(false)
            .withCommand(
                "-javaagent:/" + TestContainerManager.TARGET_AGENT_FILENAME,
                "-Dotel.resource.attributes=service.name=smoketest,deployment.environment=smokeytown",
                "-Dotel.javaagent.debug=true",
                "-Dotel.logs.exporter=none",
                "-Dsplunk.snapshot.profiler.enabled=true",
                "-Dsplunk.snapshot.selection.rate=0.1",
                "-Dsplunk.profiler.logs-endpoint=http://collector:4318/v1/logs",
                // uncomment to enable exporting traces
                // "-Dotel.exporter.otlp.endpoint=http://collector:4318",
                "-jar",
                "/app/spring-petclinic-rest.jar"));

    logger.info("Petclinic has been started.");

    try {
      generateSomeSpans();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
