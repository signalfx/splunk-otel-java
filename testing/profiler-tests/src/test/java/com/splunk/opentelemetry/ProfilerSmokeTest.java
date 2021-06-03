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

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Spliterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;
import org.junit.ClassRule;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

public class ProfilerSmokeTest {

  @ClassRule public static GenericContainer<?> petclinic;

  static {
    MountableFile agentJar = MountableFile.forHostPath(findAgentJar());
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
                "-jar",
                "/app/spring-petclinic-rest.jar")
            .withFileSystemBind("build/test/output", "/app/jfr", BindMode.READ_WRITE)
            .waitingFor(Wait.forHttp("/petclinic/api/vets"));
  }

  static Path findAgentJar() {
    try {
      Spliterator<Path> spliterator = Files.newDirectoryStream(Path.of("build/")).spliterator();
      return StreamSupport.stream(spliterator, false)
          .filter(path -> path.toFile().isFile())
          .filter(path -> path.getFileName().toString().startsWith("splunk-otel-javaagent"))
          .filter(path -> path.getFileName().toString().endsWith(".jar"))
          .sorted()
          .findFirst()
          .orElseThrow();
    } catch (Exception e) {
      fail(e);
      return null;
    }
  }

  @Test
  void ensureJfrFilesCreated() throws Exception {
    Path outputDir = Path.of("build/test/output");
    Files.createDirectories(outputDir);
    petclinic.start();
    System.out.println("Petclinic has been started.");

    Instant start = Instant.now();
    AtomicBoolean done = new AtomicBoolean(false);
    while (!done.get()) {

      System.out.println("Opening dir to look for jfr files...");
      DirectoryStream<Path> dirStream = Files.newDirectoryStream(outputDir);

      dirStream.forEach(
          item -> {
            System.out.println("Found " + item);
            if (item.toFile().isFile() && item.getFileName().toString().endsWith(".jfr")) {
              done.set(true);
            }
          });
      if (Duration.between(start, Instant.now()).toSeconds() > 60) {
        petclinic.stop();
        fail("No output within time.");
      }
      TimeUnit.SECONDS.sleep(1);
    }
  }
}
