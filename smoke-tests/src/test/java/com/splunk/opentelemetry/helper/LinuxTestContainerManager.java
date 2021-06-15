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

package com.splunk.opentelemetry.helper;

import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class LinuxTestContainerManager extends AbstractTestContainerManager {
  private static final Logger logger = LoggerFactory.getLogger(LinuxTestContainerManager.class);

  private final Network network = Network.newNetwork();
  private GenericContainer<?> backend = null;
  private GenericContainer<?> collector = null;
  private GenericContainer<?> target = null;

  @Override
  public void startEnvironment() {
    backend =
        new GenericContainer<>(
                DockerImageName.parse(
                    "ghcr.io/open-telemetry/java-test-containers:smoke-fake-backend-20210614.934907903"))
            .withExposedPorts(BACKEND_PORT)
            .waitingFor(Wait.forHttp("/health").forPort(BACKEND_PORT))
            .withNetwork(network)
            .withNetworkAliases(BACKEND_ALIAS)
            .withLogConsumer(new Slf4jLogConsumer(logger));
    backend.start();

    collector =
        new GenericContainer<>(DockerImageName.parse("otel/opentelemetry-collector-contrib:latest"))
            .dependsOn(backend)
            .withNetwork(network)
            .withNetworkAliases(COLLECTOR_ALIAS)
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .withCopyFileToContainer(
                MountableFile.forClasspathResource(COLLECTOR_CONFIG_RESOURCE), "/etc/otel.yaml")
            .withCommand("--config /etc/otel.yaml");
    collector.start();
  }

  @Override
  public void stopEnvironment() {
    if (backend != null) {
      backend.stop();
      backend = null;
    }

    if (collector != null) {
      collector.stop();
      collector = null;
    }

    network.close();
  }

  @Override
  public boolean isImageCompatible(TestImage image) {
    return image.platform == TestImage.Platform.LINUX_X86_64;
  }

  @Override
  public boolean isImagePresent(TestImage image) {
    if (!isImageCompatible(image)) {
      return false;
    }

    try {
      DockerClientFactory.lazyClient().inspectImageCmd(image.imageName).exec();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public int getBackendMappedPort() {
    return backend.getMappedPort(BACKEND_PORT);
  }

  @Override
  public int getTargetMappedPort(int originalPort) {
    return target.getMappedPort(originalPort);
  }

  @Override
  public void startTarget(
      String targetImageName,
      String agentPath,
      Map<String, String> extraEnv,
      TargetWaitStrategy waitStrategy) {
    target =
        new GenericContainer<>(DockerImageName.parse(targetImageName))
            .withStartupTimeout(Duration.ofMinutes(5))
            .withExposedPorts(TARGET_PORT)
            .withNetwork(network)
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .withCopyFileToContainer(
                MountableFile.forHostPath(agentPath), "/" + TARGET_AGENT_FILENAME)
            .withEnv(getAgentEnvironment())
            .withEnv(extraEnv);
    if (waitStrategy != null) {
      if (waitStrategy instanceof TargetWaitStrategy.Log) {
        target =
            target.waitingFor(
                Wait.forLogMessage(((TargetWaitStrategy.Log) waitStrategy).regex, 1)
                    .withStartupTimeout(waitStrategy.timeout));
      }
    }
    target.start();
  }

  @Override
  public void stopTarget() {
    if (target != null) {
      target.stop();
      target = null;
    }
  }
}
