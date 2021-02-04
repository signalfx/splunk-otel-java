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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.splunk.opentelemetry.helper.LinuxTestContainerManager;
import com.splunk.opentelemetry.helper.TargetWaitStrategy;
import com.splunk.opentelemetry.helper.TestContainerManager;
import com.splunk.opentelemetry.helper.TestImage;
import com.splunk.opentelemetry.helper.windows.WindowsTestContainerManager;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

public abstract class SmokeTest {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  protected static OkHttpClient client = OkHttpUtils.client();
  protected static TestContainerManager containerManager = createContainerManager();

  public static final String agentPath =
      System.getProperty("io.opentelemetry.smoketest.agent.shadowJar.path");

  /** Subclasses can override this method to customise target application's environment */
  protected Map<String, String> getExtraEnv() {
    return Collections.emptyMap();
  }

  @BeforeAll
  static void setupSpec() {
    containerManager.startEnvironment();
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

    containerManager.startTarget(image.imageName, agentPath, getExtraEnv(), getWaitStrategy());
  }

  protected TargetWaitStrategy getWaitStrategy() {
    return null;
  }

  @AfterEach
  void cleanup() throws IOException {
    resetBackend();
  }

  protected void resetBackend() throws IOException {
    client
        .newCall(
            new Request.Builder()
                .url(
                    String.format(
                        "http://localhost:%d/clear-requests",
                        containerManager.getBackendMappedPort()))
                .build())
        .execute()
        .close();
  }

  void stopTarget() {
    containerManager.stopTarget();
  }

  @AfterAll
  static void cleanupSpec() {
    containerManager.stopEnvironment();
  }

  protected static Stream<AnyValue> findResourceAttribute(
      Collection<ExportTraceServiceRequest> traces, String attributeKey) {
    return traces.stream()
        .flatMap(it -> it.getResourceSpansList().stream())
        .flatMap(it -> it.getResource().getAttributesList().stream())
        .filter(it -> it.getKey().equals(attributeKey))
        .map(KeyValue::getValue);
  }

  protected TraceInspector waitForTraces() throws IOException, InterruptedException {
    String content = waitForContent();

    return new TraceInspector(
        StreamSupport.stream(OBJECT_MAPPER.readTree(content).spliterator(), false)
            .map(
                it -> {
                  ExportTraceServiceRequest.Builder builder =
                      ExportTraceServiceRequest.newBuilder();
                  // TODO(anuraaga): Register parser into object mapper to avoid de -> re ->
                  // deserialize.
                  try {
                    JsonFormat.parser().merge(OBJECT_MAPPER.writeValueAsString(it), builder);
                  } catch (InvalidProtocolBufferException | JsonProcessingException e) {
                    e.printStackTrace();
                  }
                  return builder.build();
                })
            .collect(Collectors.toList()));
  }

  private String waitForContent() throws IOException, InterruptedException {
    long previousSize = 0;
    long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
    String content = "[]";
    while (System.currentTimeMillis() < deadline) {

      Request request =
          new Request.Builder()
              .url(
                  String.format(
                      "http://localhost:%d/get-requests", containerManager.getBackendMappedPort()))
              .build();

      try (ResponseBody body = client.newCall(request).execute().body()) {
        content = body.string();
      }

      if (content.length() > 2 && content.length() == previousSize) {
        break;
      }
      previousSize = content.length();
      System.out.printf("Current content size %d%n", previousSize);
      TimeUnit.MILLISECONDS.sleep(500);
    }

    return content;
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
