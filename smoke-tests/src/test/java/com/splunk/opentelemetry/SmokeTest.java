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

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.splunk.opentelemetry.helper.LinuxTestContainerManager;
import com.splunk.opentelemetry.helper.ResourceMapping;
import com.splunk.opentelemetry.helper.TargetWaitStrategy;
import com.splunk.opentelemetry.helper.TestContainerManager;
import com.splunk.opentelemetry.helper.TestImage;
import com.splunk.opentelemetry.helper.windows.WindowsTestContainerManager;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public abstract class SmokeTest {
  protected static final OkHttpClient client = OkHttpUtils.client();
  protected static final TestContainerManager containerManager = createContainerManager();
  private static boolean containerManagerStarted = false;

  public static final String agentPath =
      System.getProperty("io.opentelemetry.smoketest.agent.shadowJar.path");

  private TelemetryRetriever telemetryRetriever;

  /** Subclasses can override this method to pass jvm arguments in another environment variable */
  protected String getJvmArgsEnvVarName() {
    return "JAVA_TOOL_OPTIONS";
  }

  /** Subclasses can override this method to customise target application's environment. */
  protected Map<String, String> getExtraEnv() {
    return Collections.emptyMap();
  }

  /**
   * Subclasses can override this method to copy some extra resource files to the target container.
   */
  protected List<ResourceMapping> getExtraResources() {
    return List.of();
  }

  @BeforeAll
  static void setupSpec() {
    // TestContainerManager starts backend and collector, we want to start them only once
    // and reuse for all tests
    if (containerManagerStarted) {
      return;
    }

    containerManagerStarted = true;
    containerManager.startEnvironment();
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread() {
              @Override
              public void run() {
                containerManager.stopEnvironment();
              }
            });
  }

  void startTargetOrSkipTest(TestImage image) {
    // Skip the test if the current OS and image are incompatible (Windows images on Linux host or
    // vice versa).
    assumeTrue(
        containerManager.isImageCompatible(image),
        "Current Docker environment can run image " + image);

    if (image.isProprietaryImage) {
      // Proprietary images are private, therefore if they are not present, the test
      // will be skipped as not everybody can pull them from a remote repository.
      assumeTrue(containerManager.isImagePresent(image), "Proprietary image is present: " + image);
    }

    containerManager.startTarget(
        image.imageName,
        agentPath,
        getJvmArgsEnvVarName(),
        getExtraEnv(),
        getExtraResources(),
        getWaitStrategy());
  }

  protected TargetWaitStrategy getWaitStrategy() {
    return null;
  }

  @BeforeEach
  void setUpTelemetryRetriever() {
    telemetryRetriever = new TelemetryRetriever(client, containerManager.getBackendMappedPort());
  }

  @AfterEach
  void clearTelemetry() throws IOException {
    telemetryRetriever.clearTelemetry();
  }

  void stopTarget() {
    containerManager.stopTarget();
  }

  protected TraceInspector waitForTraces() throws IOException, InterruptedException {
    return telemetryRetriever.waitForTraces();
  }

  protected MetricsInspector waitForMetrics() throws IOException, InterruptedException {
    return telemetryRetriever.waitForMetrics();
  }

  protected LogsInspector waitForLogs() throws IOException, InterruptedException {
    return telemetryRetriever.waitForLogs();
  }

  protected String getCurrentAgentVersion() throws IOException {
    return new JarFile(agentPath)
        .getManifest()
        .getMainAttributes()
        .get(Attributes.Name.IMPLEMENTATION_VERSION)
        .toString();
  }

  private static TestContainerManager createContainerManager() {
    boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");

    if (isWindows && !"1".equals(System.getenv("USE_LINUX_CONTAINERS"))) {
      return new WindowsTestContainerManager();
    } else {
      return new LinuxTestContainerManager();
    }
  }
}
