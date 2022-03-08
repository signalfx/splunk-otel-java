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

import static com.splunk.opentelemetry.LogsInspector.getLongAttr;
import static com.splunk.opentelemetry.LogsInspector.getStringAttr;
import static com.splunk.opentelemetry.LogsInspector.hasThreadName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import com.splunk.opentelemetry.helper.TargetContainerBuilder;
import com.splunk.opentelemetry.helper.TargetWaitStrategy;
import com.splunk.opentelemetry.helper.TestContainerManager;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ProfilerSmokeTest {

  private static final Logger logger = LoggerFactory.getLogger(ProfilerSmokeTest.class);
  private static final okhttp3.OkHttpClient client = OkHttpUtils.client();
  private static final int PETCLINIC_PORT = 9966;

  private static TestContainerManager containerManager;
  private static TelemetryRetriever telemetryRetriever;
  private static Path tempDir;

  public static class TestJdk8 extends ProfilerSmokeTest {
    @BeforeAll
    static void setup() {
      startPetclinic("8");
    }
  }

  // FIXME: remove ignore when images for other versions are pushed
  @Ignore
  public static class TestJdk11 extends ProfilerSmokeTest {
    @BeforeAll
    static void setup() {
      startPetclinic("11");
    }
  }

  // FIXME: remove ignore when images for other versions are pushed
  @Ignore
  public static class TestJdk17 extends ProfilerSmokeTest {
    @BeforeAll
    static void setup() {
      startPetclinic("17");
    }
  }

  @BeforeAll
  static void setupEnvironment(@TempDir Path tempDir) {
    ProfilerSmokeTest.tempDir = tempDir;

    containerManager = SmokeTest.createContainerManager();
    containerManager.startEnvironment();

    telemetryRetriever = new TelemetryRetriever(client, containerManager.getBackendMappedPort());
  }

  @AfterAll
  static void teardown() {
    containerManager.stopEnvironment();
  }

  static String getPetclinicImageName(String jdkVersion) {
    // FIXME: add JDK and OS parts to the image name once images are pushed
    return "ghcr.io/signalfx/splunk-otel-java/profiling-petclinic-base:latest";
  }

  @Test
  void ensureJfrFilesContainContextChangeEvents() throws Exception {
    await()
        .atMost(1, TimeUnit.MINUTES)
        .pollInterval(1, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(spanThreadContextEventsFound()).isTrue());

    assertThat(contextEventsHaveStackTraces()).isFalse();
  }

  @Test
  void verifyIngestedLogContent() throws Exception {
    await()
        .atMost(2, TimeUnit.MINUTES)
        .pollInterval(1, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(countThreadDumpsInFakeBackend()).isGreaterThan(0));

    LogsInspector logs = telemetryRetriever.waitForLogs();

    assertThat(logs.getThreadDumpEvents())
        .isNotEmpty()
        .allMatch(log -> 1001 == getLongAttr(log, "source.event.period"))
        .allMatch(log -> "otel.profiling".equals(getStringAttr(log, "com.splunk.sourcetype")));

    assertThat(logs.getLogStream())
        .describedAs("Contains JFR thread")
        .anyMatch(hasThreadName("Catalina-utility-1"));

    assertThat(logs.getLogStream()).anyMatch(hasThreadName("main"));

    assertThat(logs.getTlabEvents())
        .isNotEmpty()
        .allMatch(log -> getLongAttr(log, "memory.allocated") > 0)
        .allMatch(log -> log.getBody().getStringValue().startsWith("\""))
        .allMatch(log -> log.getBody().getStringValue().split("\n").length >= 2);
  }

  @Test
  void ensureEventsResumeAfterRestartingCollector() throws Exception {
    // Petclinic can no longer access collector on Windows after restarting the container
    boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
    assumeFalse(isWindows);

    await()
        .atMost(2, TimeUnit.MINUTES)
        .pollInterval(1, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(countThreadDumpsInFakeBackend()).isGreaterThan(0));

    containerManager.stopCollector();

    long threadDumpsAfterCollectorStop = countThreadDumpsInFakeBackend();
    logger.info("Thread dump events after collector stop {}", threadDumpsAfterCollectorStop);
    Thread.sleep(30_000);
    long threadDumpsBeforeCollectorStart = countThreadDumpsInFakeBackend();
    logger.info("Thread dump events before collector start {}", threadDumpsBeforeCollectorStart);
    assertEquals(threadDumpsAfterCollectorStop, threadDumpsBeforeCollectorStart);

    containerManager.startCollector();

    await()
        .atMost(2, TimeUnit.MINUTES)
        .pollInterval(1, TimeUnit.SECONDS)
        .untilAsserted(
            () ->
                assertThat(countThreadDumpsInFakeBackend())
                    .isGreaterThan(threadDumpsAfterCollectorStop));
  }

  private long countThreadDumpsInFakeBackend() throws IOException, InterruptedException {
    return telemetryRetriever.waitForLogs().getThreadDumpEvents().count();
  }

  private boolean contextEventsHaveStackTraces() throws Exception {
    List<Path> files = findJfrFilesInOutputDir();
    return files.stream().anyMatch(this::contextEventHasStackTrace);
  }

  private boolean contextEventHasStackTrace(Path path) {
    try {
      return RecordingFile.readAllEvents(path).stream()
          .filter(ProfilerSmokeTest::isContextAttachedEvent)
          .anyMatch(event -> event.getStackTrace() != null);
    } catch (IOException e) {
      throw new AssertionError("Failed to open JFR file " + path, e);
    }
  }

  private boolean spanThreadContextEventsFound() throws Exception {
    List<Path> files = findJfrFilesInOutputDir();
    return files.stream().anyMatch(this::containsContextAttached);
  }

  private boolean containsContextAttached(Path path) {
    try {
      return RecordingFile.readAllEvents(path).stream()
          .anyMatch(ProfilerSmokeTest::isContextAttachedEvent);
    } catch (IOException e) {
      throw new AssertionError("Failed to open JFR file " + path, e);
    }
  }

  private static boolean isContextAttachedEvent(RecordedEvent event) {
    return "otel.ContextAttached".equals(event.getEventType().getName());
  }

  private static void generateSomeSpans() throws Exception {
    logger.info("Generating some spans...");
    // int port = petclinic.getMappedPort(PETCLINIC_PORT);
    int port = containerManager.getTargetMappedPort(PETCLINIC_PORT);
    doGetRequest("http://localhost:" + port + "/petclinic/api/vets");
    doGetRequest("http://localhost:" + port + "/petclinic/api/visits");
  }

  private static void doGetRequest(String url) throws Exception {
    Request request = new Request.Builder().url(url).build();
    try (Response response = client.newCall(request).execute()) {
      assertEquals(200, response.code());
    }
  }

  private List<Path> findJfrFilesInOutputDir() throws Exception {
    logger.info("Opening dir to look for jfr files...");
    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(tempDir)) {

      return StreamSupport.stream(dirStream.spliterator(), false)
          .filter(Files::isRegularFile)
          .filter(item -> item.getFileName().toString().endsWith(".jfr"))
          .collect(Collectors.toList());
    }
  }

  private static void startPetclinic(String jdkVersion) {
    containerManager.startTarget(
        new TargetContainerBuilder(getPetclinicImageName(jdkVersion))
            .withTargetPort(PETCLINIC_PORT)
            .withNetworkAliases("petclinic")
            .withAgentPath(SmokeTest.agentPath)
            .withEntrypoint("java")
            .withFileSystemBinds(
                new TargetContainerBuilder.FileSystemBind(
                    tempDir.toAbsolutePath().toString(), "/app/jfr", false))
            .withWaitStrategy(
                new TargetWaitStrategy.Http(Duration.ofMinutes(5), "/petclinic/api/vets"))
            .withUseDefaultAgentConfiguration(false)
            .withCommand(
                "-javaagent:/" + TestContainerManager.TARGET_AGENT_FILENAME,
                "-Dotel.resource.attributes=service.name=smoketest,deployment.environment=smokeytown",
                "-Dotel.javaagent.debug=true",
                "-Dsplunk.profiler.enabled=true",
                "-Dsplunk.profiler.tlab.enabled=true",
                "-Dsplunk.profiler.memory.data.format=text",
                "-Dsplunk.profiler.directory=/app/jfr",
                "-Dsplunk.profiler.keep-files=true",
                "-Dsplunk.profiler.call.stack.interval=1001",
                "-Dsplunk.profiler.logs-endpoint=http://collector:4317",
                // uncomment to enable exporting traces
                // "-Dotel.exporter.otlp.endpoint=http://collector:4317",
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
