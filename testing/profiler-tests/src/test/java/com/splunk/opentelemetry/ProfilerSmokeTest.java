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

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

public class ProfilerSmokeTest {

  public static final Path agentPath =
      Paths.get(System.getProperty("io.opentelemetry.smoketest.agent.shadowJar.path"));

  static GenericContainer<?> petclinic;

  @TempDir static Path tempDir;

  @BeforeAll
  static void setup() {
    MountableFile agentJar = MountableFile.forHostPath(agentPath);
    petclinic =
        new GenericContainer<>(new ImageFromDockerfile().withDockerfile(Path.of("./Dockerfile")))
            .withExposedPorts(9966)
            .withCopyFileToContainer(agentJar, "/app/javaagent.jar")
            .withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint("java"))
            .withCommand(
                "-javaagent:/app/javaagent.jar",
                "-Dotel.javaagent.debug=true",
                "-Dsplunk.profiler.enabled=true",
                "-Dsplunk.profiler.directory=/app/jfr",
                "-Dsplunk.profiler.keepfiles=true",
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
  void ensureJfrFilesCreated() {
    System.out.println("Petclinic has been started.");

    await()
        .atMost(60, TimeUnit.SECONDS)
        .pollInterval(1, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(findJfrFilesInOutputDir()).isNotEmpty());
  }

  private List<Path> findJfrFilesInOutputDir() throws Exception {
    System.out.println("Opening dir to look for jfr files...");
    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(tempDir)) {

      return StreamSupport.stream(dirStream.spliterator(), false)
          .filter(Files::isRegularFile)
          .filter(item -> item.getFileName().toString().endsWith(".jfr"))
          .collect(Collectors.toList());
    }
  }
}
