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

package com.splunk.opentelemetry.helper.windows;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.splunk.opentelemetry.helper.AbstractTestContainerManager;
import com.splunk.opentelemetry.helper.ResourceMapping;
import com.splunk.opentelemetry.helper.TargetContainerBuilder;
import com.splunk.opentelemetry.helper.TargetWaitStrategy;
import com.splunk.opentelemetry.helper.TestContainerManager;
import com.splunk.opentelemetry.helper.TestImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.ratelimits.RateLimiter;
import org.rnorth.ducttape.ratelimits.RateLimiterBuilder;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

public class WindowsTestContainerManager extends AbstractTestContainerManager {
  private static final Logger logger = LoggerFactory.getLogger(WindowsTestContainerManager.class);

  private static final String NPIPE_URI = "npipe:////./pipe/docker_engine";
  private static final String COLLECTOR_CONFIG_FILE_PATH = "/collector-config.yml";

  private final DockerClient client =
      DockerClientImpl.getInstance(
          new DefaultDockerClientConfig.Builder().withDockerHost(NPIPE_URI).build(),
          new ApacheDockerHttpClient.Builder().dockerHost(URI.create(NPIPE_URI)).build());

  private String natNetworkId = null;
  private Container backend;
  private Container collector;
  private Container target;

  @Override
  public void startEnvironment() {
    natNetworkId =
        client
            .createNetworkCmd()
            .withDriver("nat")
            .withName(UUID.randomUUID().toString())
            .exec()
            .getId();

    String backendImageName =
        "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-fake-backend-windows:20250530.15353244158";

    if (!imageExists(backendImageName)) {
      pullImage(backendImageName);
    }

    backend =
        createAndStartContainer(
            backendImageName,
            command ->
                command
                    .withAliases(BACKEND_ALIAS)
                    .withExposedPorts(ExposedPort.tcp(BACKEND_PORT))
                    .withEnv("JAVA_TOOL_OPTIONS=-Xmx128m")
                    .withHostConfig(
                        HostConfig.newHostConfig()
                            .withAutoRemove(true)
                            .withNetworkMode(natNetworkId)
                            .withPortBindings(
                                new PortBinding(
                                    new Ports.Binding(null, null), ExposedPort.tcp(BACKEND_PORT)))),
            containerId -> {},
            new HttpWaiter(BACKEND_PORT, "/health", Duration.ofSeconds(60)),
            true);

    String collectorImageName = "quay.io/signalfx/splunk-otel-collector-windows:latest";
    if (!imageExists(collectorImageName)) {
      pullImage(collectorImageName);
    }

    collector =
        createAndStartContainer(
            collectorImageName,
            command ->
                command
                    .withAliases(COLLECTOR_ALIAS)
                    .withExposedPorts(ExposedPort.tcp(COLLECTOR_PORT))
                    .withHostConfig(
                        HostConfig.newHostConfig()
                            .withAutoRemove(true)
                            .withNetworkMode(natNetworkId)
                            .withPortBindings(
                                new PortBinding(
                                    new Ports.Binding(null, null),
                                    ExposedPort.tcp(COLLECTOR_PORT))))
                    .withCmd("--config", COLLECTOR_CONFIG_FILE_PATH),
            containerId -> {
              try (InputStream configFileStream =
                  this.getClass().getResourceAsStream(COLLECTOR_CONFIG_RESOURCE)) {
                copyFileToContainer(
                    containerId, IOUtils.toByteArray(configFileStream), COLLECTOR_CONFIG_FILE_PATH);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            },
            new PortWaiter(COLLECTOR_PORT, Duration.ofMinutes(1)),
            true);
  }

  @Override
  public void stopEnvironment() {
    stopTarget();

    killContainer(collector);
    collector = null;

    killContainer(backend);
    backend = null;

    if (natNetworkId != null) {
      client.removeNetworkCmd(natNetworkId);
      natNetworkId = null;
    }
  }

  @Override
  public void startCollector() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void stopCollector() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isImageCompatible(TestImage image) {
    return image.platform == TestImage.Platform.WINDOWS_X86_64;
  }

  @Override
  public boolean isImagePresent(TestImage image) {
    if (!isImageCompatible(image)) {
      return false;
    }

    return imageExists(image.imageName);
  }

  @Override
  public int getBackendMappedPort() {
    return extractMappedPort(backend, BACKEND_PORT);
  }

  @Override
  public int getHecBackendMappedPort() {
    return 0;
  }

  @Override
  public int getTargetMappedPort(int originalPort) {
    return extractMappedPort(target, originalPort);
  }

  @Override
  public void startTarget(TargetContainerBuilder builder) {
    stopTarget();

    if (!imageExists(builder.targetImageName)) {
      pullImage(builder.targetImageName);
    }

    List<String> environment = new ArrayList<>();
    // hec backend is not started on windows
    environment.add("OTEL_LOGS_EXPORTER=none");

    if (builder.useDefaultAgentConfiguration) {
      getAgentEnvironment(builder.jvmArgsEnvVarName, !builder.autodetectServiceName)
          .forEach((key, value) -> environment.add(key + "=" + value));
    }

    builder.extraEnv.forEach((key, value) -> environment.add(key + "=" + value));

    target =
        createAndStartContainer(
            builder.targetImageName,
            command -> {
              HostConfig hostConfig =
                  HostConfig.newHostConfig()
                      .withAutoRemove(false)
                      .withNetworkMode(natNetworkId)
                      .withPortBindings(
                          new PortBinding(
                              new Ports.Binding(null, null), ExposedPort.tcp(builder.targetPort)));

              if (!builder.fileSystemBinds.isEmpty()) {
                List<Bind> binds =
                    builder.fileSystemBinds.stream()
                        .map(
                            bind -> {
                              MountableFile file = MountableFile.forHostPath(bind.hostPath);
                              String pathWithDisk =
                                  bind.containerPath.startsWith("/")
                                      ? "C:" + bind.containerPath
                                      : bind.containerPath;
                              String pathWithBackSlashes = pathWithDisk.replace("/", "\\");
                              return new Bind(
                                  file.getResolvedPath(),
                                  new Volume(pathWithBackSlashes),
                                  bind.isReadOnly ? AccessMode.ro : AccessMode.rw);
                            })
                        .collect(Collectors.toList());

                hostConfig.withBinds(binds);
              }

              command
                  .withExposedPorts(ExposedPort.tcp(builder.targetPort))
                  .withHostConfig(hostConfig)
                  .withEnv(environment);

              if (!builder.networkAliases.isEmpty()) {
                command.withAliases(builder.networkAliases);
              }

              if (builder.entrypoint != null) {
                command.withEntrypoint(builder.entrypoint);
              }

              if (builder.command != null) {
                command.withCmd(builder.command);
              }
            },
            (containerId) -> {
              try {
                if (builder.agentPath != null) {
                  try (InputStream agentFileStream = new FileInputStream(builder.agentPath)) {
                    copyFileToContainer(
                        containerId,
                        IOUtils.toByteArray(agentFileStream),
                        "/" + TARGET_AGENT_FILENAME);
                  }
                }

                for (ResourceMapping resource : builder.extraResources) {
                  copyResourceToContainer(
                      containerId, resource.resourcePath(), resource.containerPath());
                }
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            },
            createTargetWaiter(builder.waitStrategy, builder.targetPort),
            true);
  }

  @Override
  public void stopTarget() {
    killContainer(target);
    target = null;
  }

  @Override
  public GenericContainer<?> getTargetContainer() {
    throw new UnsupportedOperationException(
        "Windows container manager does not support testcontainers");
  }

  @Override
  public GenericContainer<?> newContainer(TestImage image) {
    throw new UnsupportedOperationException(
        "Windows container manager does not support testcontainers");
  }

  private void pullImage(String imageName) {
    logger.info("Pulling {}", imageName);

    try {
      client.pullImageCmd(imageName).exec(new PullImageResultCallback()).awaitCompletion();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean imageExists(String imageName) {
    try {
      client.inspectImageCmd(imageName).exec();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private void copyResourceToContainer(
      String containerId, String resourcePath, String containerPath) throws IOException {
    try (InputStream is =
        TestContainerManager.class.getClassLoader().getResourceAsStream(resourcePath)) {
      copyFileToContainer(containerId, IOUtils.toByteArray(is), containerPath);
    }
  }

  private void copyFileToContainer(String containerId, byte[] content, String containerPath)
      throws IOException {
    try (ByteArrayOutputStream output = new ByteArrayOutputStream();
        TarArchiveOutputStream archiveStream = new TarArchiveOutputStream(output)) {
      archiveStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

      TarArchiveEntry entry = new TarArchiveEntry(containerPath);
      entry.setSize(content.length);
      entry.setMode(0100644);

      archiveStream.putArchiveEntry(entry);
      IOUtils.write(content, archiveStream);
      archiveStream.closeArchiveEntry();
      archiveStream.finish();

      client
          .copyArchiveToContainerCmd(containerId)
          .withTarInputStream(new ByteArrayInputStream(output.toByteArray()))
          .withRemotePath("/")
          .exec();
    }
  }

  private ContainerLogHandler consumeLogs(String containerId, Waiter waiter) {
    ContainerLogFrameConsumer consumer = new ContainerLogFrameConsumer();
    waiter.configureLogger(consumer);

    client
        .logContainerCmd(containerId)
        .withFollowStream(true)
        .withSince(0)
        .withStdOut(true)
        .withStdErr(true)
        .exec(consumer);

    consumer.addListener(new Slf4jDockerLogLineListener(logger));
    return consumer;
  }

  private static int extractMappedPort(Container container, int internalPort) {
    Ports.Binding[] binding =
        container
            .inspectResponse
            .getNetworkSettings()
            .getPorts()
            .getBindings()
            .get(ExposedPort.tcp(internalPort));
    if (binding != null && binding.length > 0 && binding[0] != null) {
      return Integer.parseInt(binding[0].getHostPortSpec());
    } else {
      throw new RuntimeException("Port " + internalPort + " not mapped to host.");
    }
  }

  private Container createAndStartContainer(
      String imageName,
      Consumer<CreateContainerCmd> createAction,
      Consumer<String> prepareAction,
      Waiter waiter,
      boolean inspect) {

    String containerId = createContainer(imageName, createAction, prepareAction);
    return startContainer(imageName, containerId, waiter, inspect);
  }

  private String createContainer(
      String imageName, Consumer<CreateContainerCmd> createAction, Consumer<String> prepareAction) {
    CreateContainerCmd createCommand = client.createContainerCmd(imageName);
    createAction.accept(createCommand);

    String containerId = createCommand.exec().getId();

    prepareAction.accept(containerId);
    return containerId;
  }

  private Container startContainer(
      String imageName, String containerId, Waiter waiter, boolean inspect) {
    if (waiter == null) {
      waiter = new NoOpWaiter();
    }

    client.startContainerCmd(containerId).exec();
    ContainerLogHandler logHandler = consumeLogs(containerId, waiter);

    InspectContainerResponse inspectResponse =
        inspect ? client.inspectContainerCmd(containerId).exec() : null;
    Container container = new Container(imageName, containerId, logHandler, inspectResponse);

    waiter.waitFor(container);
    return container;
  }

  private void killContainer(Container container) {
    if (container != null) {
      try {
        client.killContainerCmd(container.containerId).exec();
      } catch (NotFoundException e) {
        // The containers are flagged as remove-on-exit, so not finding them can be expected
      }
    }
  }

  private static class Container {
    public final String imageName;
    public final String containerId;
    public final ContainerLogHandler logConsumer;
    public final InspectContainerResponse inspectResponse;

    private Container(
        String imageName,
        String containerId,
        ContainerLogHandler logConsumer,
        InspectContainerResponse inspectResponse) {
      this.imageName = imageName;
      this.containerId = containerId;
      this.logConsumer = logConsumer;
      this.inspectResponse = inspectResponse;
    }
  }

  private Waiter createTargetWaiter(TargetWaitStrategy strategy, int targetPort) {
    if (strategy instanceof TargetWaitStrategy.Log) {
      TargetWaitStrategy.Log details = (TargetWaitStrategy.Log) strategy;
      return new LogWaiter(Pattern.compile(details.regex), details.timeout);
    } else if (strategy instanceof TargetWaitStrategy.Http) {
      TargetWaitStrategy.Http details = (TargetWaitStrategy.Http) strategy;
      return new HttpWaiter(targetPort, details.path, details.timeout);
    } else {
      return new PortWaiter(targetPort, Duration.ofSeconds(60));
    }
  }

  private interface Waiter {
    default void configureLogger(ContainerLogHandler logHandler) {}

    void waitFor(Container container);
  }

  private static class NoOpWaiter implements Waiter {
    @Override
    public void waitFor(Container container) {
      // No waiting
    }
  }

  private static class LogWaiter implements Waiter {
    private final Pattern regex;
    private final Duration timeout;
    private final CountDownLatch lineHit = new CountDownLatch(1);

    private LogWaiter(Pattern regex, Duration timeout) {
      this.regex = regex;
      this.timeout = timeout;
    }

    @Override
    public void configureLogger(ContainerLogHandler logHandler) {
      logHandler.addListener(
          (type, text) -> {
            if (lineHit.getCount() > 0) {
              if (regex.matcher(text).find()) {
                lineHit.countDown();
              }
            }
          });
    }

    @Override
    public void waitFor(Container container) {
      logger.info(
          "Waiting for container {}/{} to hit log line {}",
          container.imageName,
          container.containerId,
          regex.toString());

      try {
        lineHit.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      logger.info("Done waiting for container {}/{}", container.imageName, container.containerId);
    }
  }

  private static class HttpWaiter implements Waiter {
    private static final OkHttpClient CLIENT =
        new OkHttpClient.Builder().callTimeout(1, TimeUnit.SECONDS).build();

    private final int internalPort;
    private final String path;
    private final Duration timeout;
    private final RateLimiter rateLimiter =
        RateLimiterBuilder.newBuilder()
            .withRate(1, TimeUnit.SECONDS)
            .withConstantThroughput()
            .build();

    private HttpWaiter(int internalPort, String path, Duration timeout) {
      this.internalPort = internalPort;
      this.path = path;
      this.timeout = timeout;
    }

    @Override
    public void waitFor(Container container) {
      Request request =
          new Request.Builder()
              .url("http://localhost:" + extractMappedPort(container, internalPort) + path)
              .build();

      logger.info(
          "Waiting for container {}/{} on url {}",
          container.imageName,
          container.containerId,
          request.url());

      try {
        Unreliables.retryUntilSuccess(
            (int) timeout.toMillis(),
            TimeUnit.MILLISECONDS,
            () -> {
              rateLimiter.doWhenReady(
                  () -> {
                    try {
                      Response response = CLIENT.newCall(request).execute();

                      if (response.code() != 200) {
                        throw new RuntimeException(
                            "Received status code " + response.code() + " from " + request.url());
                      }
                    } catch (IOException e) {
                      throw new RuntimeException(e);
                    }
                  });

              return true;
            });
      } catch (TimeoutException e) {
        throw new RuntimeException("Timed out waiting for container " + container.imageName, e);
      }

      logger.info("Done waiting for container {}/{}", container.imageName, container.containerId);
    }
  }

  private static class PortWaiter implements Waiter {
    private final int internalPort;
    private final Duration timeout;
    private final RateLimiter rateLimiter =
        RateLimiterBuilder.newBuilder()
            .withRate(1, TimeUnit.SECONDS)
            .withConstantThroughput()
            .build();

    private PortWaiter(int internalPort, Duration timeout) {
      this.internalPort = internalPort;
      this.timeout = timeout;
    }

    @Override
    public void waitFor(Container container) {
      logger.info(
          "Waiting for container {}/{} on port {}",
          container.imageName,
          container.containerId,
          internalPort);

      try {
        Unreliables.retryUntilSuccess(
            (int) timeout.toMillis(),
            TimeUnit.MILLISECONDS,
            () -> {
              rateLimiter.doWhenReady(
                  () -> {
                    int externalPort = extractMappedPort(container, internalPort);

                    try {
                      (new Socket("localhost", externalPort)).close();
                    } catch (IOException e) {
                      throw new IllegalStateException(
                          "Socket not listening yet: " + externalPort, e);
                    }
                  });

              return true;
            });
      } catch (TimeoutException e) {
        throw new RuntimeException("Timed out waiting for container " + container.imageName, e);
      }

      logger.info("Done waiting for container {}/{}", container.imageName, container.containerId);
    }
  }
}
