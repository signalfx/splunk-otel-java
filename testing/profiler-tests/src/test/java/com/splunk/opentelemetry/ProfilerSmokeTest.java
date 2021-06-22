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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.okhttp3.OkHttpClient;
import org.testcontainers.shaded.okhttp3.Request;
import org.testcontainers.shaded.okhttp3.Response;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class ProfilerSmokeTest {

  private static final Logger logger = LoggerFactory.getLogger(ProfilerSmokeTest.class);
  public static final Path agentPath =
      Paths.get(System.getProperty("io.opentelemetry.smoketest.agent.shadowJar.path"));
  public static final int PETCLINIC_PORT = 9966;

  static GenericContainer<?> petclinic;

  @TempDir static Path tempDir;

  @BeforeAll
  static void setup() {
    MountableFile agentJar = MountableFile.forHostPath(agentPath);
    petclinic =
        new GenericContainer<>(
                DockerImageName.parse(
                    "ghcr.io/signalfx/splunk-otel-java/profiling-petclinic-base:latest"))
            .withExposedPorts(PETCLINIC_PORT)
            .withCopyFileToContainer(agentJar, "/app/javaagent.jar")
            .withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint("java"))
            .withCommand(
                "-javaagent:/app/javaagent.jar",
                "-Dotel.javaagent.debug=true",
                "-Dsplunk.profiler.enabled=true",
                "-Dsplunk.profiler.directory=/app/jfr",
                "-Dsplunk.profiler.keep-files=true",
                "-jar",
                "/app/spring-petclinic-rest.jar")
            .withFileSystemBind(
                tempDir.toAbsolutePath().toString(), "/app/jfr", BindMode.READ_WRITE)
            .waitingFor(Wait.forHttp("/petclinic/api/vets"));
    petclinic.start();
  }

  @AfterAll
  static void teardown() {
    petclinic.stop();
  }

  @Test
  void ensureJfrFilesContainContextChangeEvents() throws Exception {
    logger.info("Petclinic has been started.");

    generateSomeSpans();

    await()
        .atMost(60, TimeUnit.SECONDS)
        .pollInterval(1, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(spanThreadContextEventsFound()).isTrue());
  }

  private boolean spanThreadContextEventsFound() throws Exception {
    List<Path> files = findJfrFilesInOutputDir();
    return files.stream().anyMatch(this::containsContextAttached);
  }

  private boolean containsContextAttached(Path path) {
    try (RecordingFile file = new RecordingFile(path)) {
      while (file.hasMoreEvents()) {
        RecordedEvent event = file.readEvent();
        if ("otel.ContextAttached".equals(event.getEventType().getName())) {
          return true;
        }
      }
    } catch (IOException e) {
      return false;
    }
    return false;
  }

  private void generateSomeSpans() throws Exception {
    logger.info("Generating some spans...");
    OkHttpClient client = buildClient();
    int port = petclinic.getMappedPort(PETCLINIC_PORT);
    doGetRequest(client, "http://localhost:" + port + "/petclinic/api/vets");
    doGetRequest(client, "http://localhost:" + port + "/petclinic/api/visits");
  }

  private void doGetRequest(OkHttpClient client, String url) throws Exception {
    Request request = new Request.Builder().url(url).build();
    try (Response response = client.newCall(request).execute()) {
      assertEquals(200, response.code());
    }
  }

  private OkHttpClient buildClient() {
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
