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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class LinuxTestContainerManager extends AbstractTestContainerManager {
  private static final Logger logger = LoggerFactory.getLogger(LinuxTestContainerManager.class);

  private Network network;
  private GenericContainer<?> backend;
  private GenericContainer<?> hecBackend;
  private GenericContainer<?> collector;
  private GenericContainer<?> target;

  @Override
  public void startEnvironment() {
    network = Network.newNetwork();

    backend =
        new GenericContainer<>(
                DockerImageName.parse(
                    "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-fake-backend:20221127.3559314891"))
            .withExposedPorts(BACKEND_PORT)
            .withEnv("JAVA_TOOL_OPTIONS", "-Xmx128m")
            .waitingFor(Wait.forHttp("/health").forPort(BACKEND_PORT))
            .withNetwork(network)
            .withNetworkAliases(BACKEND_ALIAS)
            .withLogConsumer(new Slf4jLogConsumer(logger));
    backend.start();

    hecBackend =
        new GenericContainer<>(DockerImageName.parse("mockserver/mockserver:5.15.0"))
            .withExposedPorts(HEC_BACKEND_PORT)
            .waitingFor(Wait.forLogMessage(".*started on port.*", 1))
            .withNetwork(network)
            .withNetworkAliases(HEC_BACKEND_ALIAS)
            .withLogConsumer(new Slf4jLogConsumer(logger));
    hecBackend.start();

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
    if (hecBackend != null) {
      hecBackend.stop();
      hecBackend = null;
    }
    if (collector != null) {
      collector.stop();
      collector = null;
    }
    if (network != null) {
      network.close();
      network = null;
    }
  }

  @Override
  public void startCollector() {
    if (collector != null) {
      collector.start();
    }
  }

  @Override
  public void stopCollector() {
    if (collector != null) {
      collector.stop();
    }
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
  public int getHecBackendMappedPort() {
    return hecBackend.getMappedPort(HEC_BACKEND_PORT);
  }

  @Override
  public int getTargetMappedPort(int originalPort) {
    return target.getMappedPort(originalPort);
  }

  @Override
  public void startTarget(TargetContainerBuilder builder) {
    target =
        new GenericContainer<>(DockerImageName.parse(builder.targetImageName))
            .withStartupTimeout(Duration.ofMinutes(5))
            .withExposedPorts(builder.targetPort)
            .withNetwork(network)
            .withLogConsumer(new Slf4jLogConsumer(logger));

    if (builder.agentPath != null) {
      target.withCopyFileToContainer(
          MountableFile.forHostPath(builder.agentPath), "/" + TARGET_AGENT_FILENAME);
    }

    if (builder.useDefaultAgentConfiguration) {
      target.withEnv(
          getAgentEnvironment(builder.jvmArgsEnvVarName, !builder.autodetectServiceName));
    }

    builder.fileSystemBinds.forEach(
        it ->
            target.withFileSystemBind(
                it.hostPath,
                it.containerPath,
                it.isReadOnly ? BindMode.READ_ONLY : BindMode.READ_WRITE));

    if (builder.command != null) {
      target.withCommand(builder.command.toArray(new String[0]));
    }

    if (builder.entrypoint != null) {
      target.withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint(builder.entrypoint));
    }

    if (!builder.networkAliases.isEmpty()) {
      target.withNetworkAliases(builder.networkAliases.toArray(new String[0]));
    }

    target.withEnv(builder.extraEnv);

    for (ResourceMapping resource : builder.extraResources) {
      target.withCopyFileToContainer(
          MountableFile.forClasspathResource(resource.resourcePath()), resource.containerPath());
    }

    TargetWaitStrategy waitStrategy = builder.waitStrategy;

    if (waitStrategy != null) {
      if (waitStrategy instanceof TargetWaitStrategy.Log) {
        target =
            target.waitingFor(
                Wait.forLogMessage(((TargetWaitStrategy.Log) waitStrategy).regex, 1)
                    .withStartupTimeout(waitStrategy.timeout));
      }
      if (waitStrategy instanceof TargetWaitStrategy.Http) {
        target =
            target.waitingFor(
                Wait.forHttp(((TargetWaitStrategy.Http) waitStrategy).path)
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

  @Override
  public GenericContainer<?> getTargetContainer() {
    return target;
  }

  @Override
  public GenericContainer<?> newContainer(TestImage image) {
    return new GenericContainer<>(DockerImageName.parse(image.imageName))
        .dependsOn(collector)
        .withNetwork(network)
        .withLogConsumer(new Slf4jLogConsumer(logger));
  }
}
