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

package com.splunk.opentelemetry.profiler;

import static com.splunk.opentelemetry.profiler.TestHelpers.flattenToLogRecords;
import static com.splunk.opentelemetry.profiler.TestHelpers.getLongAttr;
import static com.splunk.opentelemetry.profiler.TestHelpers.getStringAttr;
import static com.splunk.opentelemetry.profiler.TestHelpers.parseToExportLogsServiceRequests;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.splunk.opentelemetry.profiler.events.ContextAttached;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.logs.v1.LogRecord;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.okhttp3.OkHttpClient;
import org.testcontainers.shaded.okhttp3.Request;
import org.testcontainers.shaded.okhttp3.Response;
import org.testcontainers.shaded.okhttp3.ResponseBody;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class ProfilerSmokeTest {

  private static final Logger logger = LoggerFactory.getLogger(ProfilerSmokeTest.class);
  private static final Path AGENT_PATH =
      Paths.get(System.getProperty("io.opentelemetry.smoketest.agent.shadowJar.path"));
  private static final Network NETWORK = Network.newNetwork();
  public static final int PETCLINIC_PORT = 9966;
  public static final int BACKEND_PORT = 8080;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final RecordedEventStream eventStream = new BasicJfrRecordingFile(JFR.instance);

  private static GenericContainer<?> backend;
  private static GenericContainer<?> collector;
  private static GenericContainer<?> petclinic;

  @TempDir static Path tempDir;

  @BeforeAll
  static void setup() throws Exception {
    MountableFile agentJar = MountableFile.forHostPath(AGENT_PATH);

    backend =
        new GenericContainer<>(
                DockerImageName.parse(
                    "ghcr.io/open-telemetry/java-test-containers:smoke-fake-backend-20210624.967200357"))
            .withExposedPorts(BACKEND_PORT)
            .waitingFor(Wait.forHttp("/health").forPort(BACKEND_PORT))
            .withNetwork(NETWORK)
            .withNetworkAliases("backend")
            .withLogConsumer(new Slf4jLogConsumer(logger));
    backend.start();

    collector =
        new GenericContainer<>(DockerImageName.parse("otel/opentelemetry-collector-contrib:latest"))
            .dependsOn(backend)
            .withNetwork(NETWORK)
            .withNetworkAliases("collector")
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("collector.yaml"), "/etc/otel.yaml")
            .withCommand("--config /etc/otel.yaml");
    collector.start();

    petclinic =
        new GenericContainer<>(
                DockerImageName.parse(
                    "ghcr.io/signalfx/splunk-otel-java/profiling-petclinic-base:latest"))
            .withExposedPorts(PETCLINIC_PORT)
            .withNetwork(NETWORK)
            .withNetworkAliases("petclinic")
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .withCopyFileToContainer(agentJar, "/app/javaagent.jar")
            .withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint("java"))
            .withCommand(
                "-javaagent:/app/javaagent.jar",
                "-Dotel.resource.attributes=service.name=smoketest,deployment.environment=smokeytown",
                "-Dotel.javaagent.debug=true",
                "-Dsplunk.profiler.enabled=true",
                "-Dsplunk.profiler.tlab.enabled=false",
                "-Dsplunk.profiler.directory=/app/jfr",
                "-Dsplunk.profiler.keep-files=true",
                "-Dsplunk.profiler.period.threaddump=1001",
                "-Dsplunk.profiler.logs-endpoint=http://collector:4317",
                // uncomment to enable exporting traces
                // "-Dotel.exporter.otlp.endpoint=http://collector:4317",
                "-jar",
                "/app/spring-petclinic-rest.jar")
            .withFileSystemBind(
                tempDir.toAbsolutePath().toString(), "/app/jfr", BindMode.READ_WRITE)
            .waitingFor(Wait.forHttp("/petclinic/api/vets"));
    petclinic.start();
    logger.info("Petclinic has been started.");
    generateSomeSpans();
  }

  @AfterAll
  static void teardown() {
    petclinic.stop();
    collector.stop();
    backend.stop();
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

    List<ExportLogsServiceRequest> resourceLogs = fetchResourceLogs();
    List<LogRecord> logs = flattenToLogRecords(resourceLogs);
    assertAllThreadDumps(logs, log -> 1001 == getLongAttr(log, "source.event.period"));
    assertAllThreadDumps(logs, log -> "otel.profiling".equals(getStringAttr(log, "sourcetype")));

    Optional<LogRecord> jfrThread =
        logs.stream()
            .filter(log -> log.getBody().getStringValue().startsWith("\"Catalina-utility-1\""))
            .findFirst();
    assertThat(jfrThread).isNotEmpty();
    Optional<LogRecord> mainThread =
        logs.stream()
            .filter(log -> log.getBody().getStringValue().startsWith("\"main\""))
            .findFirst();
    assertThat(mainThread).isNotEmpty();
    assertThat(countTLABs(logs)).isGreaterThan(0);
  }

  @Test
  void ensureEventsResumeAfterRestartingCollector() throws Exception {
    await()
        .atMost(2, TimeUnit.MINUTES)
        .pollInterval(1, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(countThreadDumpsInFakeBackend()).isGreaterThan(0));
    collector.stop();

    long threadDumpsAfterCollectorStop = countThreadDumpsInFakeBackend();
    logger.info("Thread dump events after collector stop {}", threadDumpsAfterCollectorStop);
    Thread.sleep(30_000);
    long threadDumpsBeforeCollectorStart = countThreadDumpsInFakeBackend();
    logger.info("Thread dump events before collector start {}", threadDumpsBeforeCollectorStart);
    assertEquals(threadDumpsAfterCollectorStop, threadDumpsBeforeCollectorStart);

    collector.start();
    await()
        .atMost(2, TimeUnit.MINUTES)
        .pollInterval(1, TimeUnit.SECONDS)
        .untilAsserted(
            () ->
                assertThat(countThreadDumpsInFakeBackend())
                    .isGreaterThan(threadDumpsAfterCollectorStop));
  }

  private long countThreadDumpsInFakeBackend() throws IOException {
    List<ExportLogsServiceRequest> resourceLogs = fetchResourceLogs();
    return flattenToLogRecords(resourceLogs).stream()
        .flatMap(log -> log.getAttributesList().stream())
        .filter(attr -> attr.getKey().equals("source.event.name"))
        .filter(attr -> attr.getValue().getStringValue().equals("jdk.ThreadDump"))
        .count();
  }

  private long countTLABs(List<LogRecord> logs) {
    return logs.stream().filter(TestHelpers::isTLABEvent).count();
  }

  private boolean assertAllThreadDumps(List<LogRecord> logs, Predicate<LogRecord> predicate) {
    return logs.stream().filter(TestHelpers::isThreadDumpEvent).allMatch(predicate);
  }

  private List<ExportLogsServiceRequest> fetchResourceLogs() throws IOException {
    OkHttpClient client = buildClient();
    int port = backend.getMappedPort(BACKEND_PORT);
    Request request = new Request.Builder().url("http://localhost:" + port + "/get-logs").build();
    try (Response response = client.newCall(request).execute()) {
      assertEquals(200, response.code());
      try (ResponseBody body = response.body()) {
        String bodyContent = new String(body.byteStream().readAllBytes());
        return parseToExportLogsServiceRequests(bodyContent);
      }
    }
  }

  private boolean contextEventsHaveStackTraces() throws Exception {
    List<Path> files = findJfrFilesInOutputDir();
    return files.stream().anyMatch(this::contextEventHasStackTrace);
  }

  private boolean contextEventHasStackTrace(Path path) {
    try (Stream<RecordedEvent> events = eventStream.open(path)) {
      return events
          .filter(event1 -> isEventType(event1, ContextAttached.EVENT_NAME))
          .anyMatch(event -> event.getStackTrace() != null);
    }
  }

  private boolean spanThreadContextEventsFound() throws Exception {
    List<Path> files = findJfrFilesInOutputDir();
    return files.stream().anyMatch(this::containsContextAttached);
  }

  private boolean threadDumpEventsFound() throws Exception {
    List<Path> files = findJfrFilesInOutputDir();
    return files.stream().anyMatch(this::containsThreadDump);
  }

  private boolean containsContextAttached(Path path) {
    return anyEventMatches(path, event -> isEventType(event, ContextAttached.EVENT_NAME));
  }

  private boolean containsThreadDump(Path path) {
    return anyEventMatches(path, event -> isEventType(event, ThreadDumpProcessor.EVENT_NAME));
  }

  private boolean anyEventMatches(Path path, Predicate<RecordedEvent> check) {
    try (Stream<RecordedEvent> open = eventStream.open(path)) {
      return open.anyMatch(check);
    }
  }

  private boolean isEventType(RecordedEvent event, String name) {
    return name.equals(event.getEventType().getName());
  }

  private static void generateSomeSpans() throws Exception {
    logger.info("Generating some spans...");
    OkHttpClient client = buildClient();
    int port = petclinic.getMappedPort(PETCLINIC_PORT);
    doGetRequest(client, "http://localhost:" + port + "/petclinic/api/vets");
    doGetRequest(client, "http://localhost:" + port + "/petclinic/api/visits");
  }

  private static void doGetRequest(OkHttpClient client, String url) throws Exception {
    Request request = new Request.Builder().url(url).build();
    try (Response response = client.newCall(request).execute()) {
      assertEquals(200, response.code());
    }
  }

  private static OkHttpClient buildClient() {
    return new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build();
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
}
