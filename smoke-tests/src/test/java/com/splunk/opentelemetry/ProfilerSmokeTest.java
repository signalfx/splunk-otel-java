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
import static com.splunk.opentelemetry.LogsInspector.hasThreadName;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.DATA_FORMAT;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.FRAME_COUNT;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.PPROF_GZIP_BASE64;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import com.splunk.opentelemetry.helper.TargetContainerBuilder;
import com.splunk.opentelemetry.helper.TargetWaitStrategy;
import com.splunk.opentelemetry.helper.TestContainerManager;
import com.splunk.opentelemetry.helper.TestImage;
import com.splunk.opentelemetry.helper.windows.WindowsTestContainerManager;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ProfilerSmokeTest {

  private static final Logger logger = LoggerFactory.getLogger(ProfilerSmokeTest.class);
  private static final okhttp3.OkHttpClient client = OkHttpUtils.client();
  private static final int PETCLINIC_PORT = 9966;
  @TempDir private static Path tempDir;

  private TestContainerManager containerManager;
  private TelemetryRetriever telemetryRetriever;
  private final String jdkVersion;

  ProfilerSmokeTest(String jdkVersion) {
    this.jdkVersion = jdkVersion;
  }

  public static class TestJdk8 extends ProfilerSmokeTest {
    TestJdk8() {
      super("8");
    }
  }

  public static class TestJdk11 extends ProfilerSmokeTest {
    TestJdk11() {
      super("11");
    }
  }

  public static class TestJdk17 extends ProfilerSmokeTest {
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

    assertThat(logs.getLogStream())
        .allMatch(log -> "otel.profiling".equals(getStringAttr(log, SOURCE_TYPE)))
        .allMatch(log -> PPROF_GZIP_BASE64.equals(getStringAttr(log, DATA_FORMAT)));

    assertThat(logs.getLogStream())
        .allMatch(log -> LogsInspector.getLongAttr(log, FRAME_COUNT) > 0);

    assertThat(logs.getCpuSamples())
        .isNotEmpty()
        .allMatch(sample -> 1001 == sample.getSourceEventPeriod());

    assertThat(logs.getCpuSamples())
        .describedAs("Contains JFR thread")
        .anyMatch(hasThreadName("Catalina-utility-1"));

    assertThat(logs.getCpuSamples()).anyMatch(hasThreadName("main"));

    assertThat(logs.getMemorySamples())
        .isNotEmpty()
        .allMatch(sample -> sample.getAllocated() > 0)
        .allMatch(sample -> sample.getThreadName() != null);
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
    return telemetryRetriever.waitForLogs().getCpuSamples().size();
  }

  private boolean contextEventsHaveStackTraces() throws Exception {
    List<Path> files = findJfrFilesInOutputDir();
    return files.stream().anyMatch(this::contextEventHasStackTrace);
  }

  private boolean contextEventHasStackTrace(Path path) {
    if (!Files.isReadable(path)) {
      makeReadable(path);
    }
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
    if (!Files.isReadable(path)) {
      makeReadable(path);
    }
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

  private void generateSomeSpans() throws Exception {
    logger.info("Generating some spans...");
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

  private void startPetclinic() {
    containerManager.startTarget(
        new TargetContainerBuilder(getPetclinicImageName())
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
                "-Dotel.logs.exporter=none",
                "-Dsplunk.profiler.enabled=true",
                "-Dsplunk.profiler.memory.enabled=true",
                "-Dsplunk.profiler.directory=/app/jfr",
                "-Dsplunk.profiler.keep-files=true",
                "-Dsplunk.profiler.call.stack.interval=1001",
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

  private void makeReadable(Path path) {
    if (containerManager instanceof WindowsTestContainerManager) {
      return;
    }

    try {
      // on linux host jfr files created inside the container are not readable from the host
      Container.ExecResult result =
          containerManager
              .getTargetContainer()
              .execInContainer("chmod", "a+r", "/app/jfr/" + path.getFileName().toString());
      if (result.getExitCode() != 0) {
        logger.error(
            "Failed to make file readable, chmod exited with {} stdout: {} stderr: {}",
            result.getExitCode(),
            result.getStdout(),
            result.getStderr());
      }
    } catch (IOException | InterruptedException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
