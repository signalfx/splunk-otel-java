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

import static com.splunk.opentelemetry.helper.TestImage.linuxImage;
import static com.splunk.opentelemetry.helper.TestImage.proprietaryLinuxImage;
import static com.splunk.opentelemetry.helper.TestImage.windowsImage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.splunk.opentelemetry.helper.TestImage;
import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.params.provider.Arguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AppServerTest extends SmokeTest {

  private static final Logger log = LoggerFactory.getLogger(AppServerTest.class);

  /**
   * The test case is expected to create and verify the following trace: <code>
   * 1. Server span for the initial request to http://localhost:%d/greeting?url=http://localhost:8080/headers
   * 2. Client http span to http://localhost:8080/headers
   * 3. Server http span for http://localhost:8080/headers
   * </code>
   */
  protected void assertWebAppTrace(ExpectedServerAttributes serverAttributes)
      throws IOException, InterruptedException {
    String url = getUrl("/app/greeting", false);

    Request request = new Request.Builder().get().url(url).build();
    String responseHeadersAndBody = tryGetResponse(request);

    TraceInspector traces = waitForTraces();

    assertEquals(1, traces.countTraceIds(), "There is one trace");

    Set<String> traceIds = traces.getTraceIds();
    String theOneTraceId = traceIds.iterator().next();

    assertTrue(
        responseHeadersAndBody.contains(theOneTraceId),
        "trace id is present in the HTTP headers as reported by the called endpoint");

    assertEquals(
        2,
        traces.countSpansByKind(Span.SpanKind.SPAN_KIND_SERVER),
        "Server spans in the distributed trace");
    assertWebengineAttributesInWebAppTrace(serverAttributes, traces);

    assertEquals(
        1,
        traces.countFilteredAttributes("url.path", "/app/greeting"),
        "The span for the initial web request");
    assertEquals(
        1,
        traces.countFilteredAttributes("url.full", getUrl("/app/headers", true)),
        "Client span for the remote call");
    assertEquals(
        1,
        traces.countFilteredAttributes("url.path", "/app/headers"),
        "Server span for the remote call");

    assertThat(traces.getInstrumentationLibraryVersions())
        .as("All spans are tagged with current otel library version")
        .containsExactly(getOtelInstrumentationVersion());

    // verify that correct service name is set in the resource
    assertTrue(
        traces.resourceExists(
            "service.name", shouldAutodetectServiceName() ? "smoke-test-app" : "smoke-test"));
  }

  /*
   * Some app servers, e.g. WebLogic 12.1.3 open their HTTP listen port before
   * an application gets deployed. Which results in all kinds of response codes
   * ranging from 404 to 503 to be returned instead of expected response.
   * This method retries for 30 seconds to get a 200 response after the container's
   * port is open and docker thinks the container is good for use.
   */
  private String tryGetResponse(Request request) throws IOException {
    long startTime = System.currentTimeMillis();
    Response response;
    String responseBody;
    do {
      response = client.newCall(request).execute();
      responseBody = response.body().string();
      System.out.println("Got response code " + response.code());
    } while (response.code() != 200
        && System.currentTimeMillis() - startTime < TimeUnit.SECONDS.toMillis(30));

    assertEquals(
        200, response.code(), "Unexpected response code. Got this response: " + responseBody);
    return responseBody;
  }

  private int totalNumberOfSpansInWebappTrace() {
    // 1) Incoming /greeting
    // 2) Outgoing /headers
    // 3) Incoming /headers
    return 3;
  }

  protected void assertWebengineAttributesInWebAppTrace(
      ExpectedServerAttributes serverAttributes, TraceInspector traces) {
    assertEquals(
        2,
        traces.countFilteredAttributes("webengine.name", serverAttributes.webengineName),
        "Webengine name is present on all server spans");
    assertEquals(
        2,
        traces.countFilteredAttributes("webengine.version", serverAttributes.webengineVersion),
        "Webengine version is present on all server spans");
  }

  protected void assertServerHandler(ExpectedServerAttributes serverAttributes)
      throws IOException, InterruptedException {
    String url =
        getUrl("/this-is-definitely-not-there-but-there-should-be-a-trace-nevertheless", false);

    Request request = new Request.Builder().get().url(url).build();
    Response response = client.newCall(request).execute();
    log.debug("Response for non-existing page: {}", response.body().string());
    assertEquals(
        404,
        response.code(),
        "404 response code is expected from the app-server for a request to a non-existing page.");
    var traces = waitForTraces();

    var numberOfTraceIds = traces.countTraceIds();
    assertEquals(1, numberOfTraceIds, "There is one trace from server handler");

    assertEquals(
        1,
        traces.countSpansByName(serverAttributes.handlerSpanName),
        "Server span has expected name");

    assertEquals(
        serverAttributes.webengineName,
        traces.getServerSpanAttribute("webengine.name"),
        "Webengine name tag on server span");
    assertEquals(
        serverAttributes.webengineVersion,
        traces.getServerSpanAttribute("webengine.version"),
        "Webengine version tag on server span");

    clearTelemetry();
  }

  protected static class ExpectedServerAttributes {
    final String handlerSpanName;
    final String webengineName;
    final String webengineVersion;

    public ExpectedServerAttributes(
        String handlerSpanName, String webengineName, String webengineVersion) {
      this.handlerSpanName = handlerSpanName;
      this.webengineName = webengineName;
      this.webengineVersion = webengineVersion;
    }
  }

  protected static final String OTEL_IMAGE_VERSION = "20241014.11321808438";
  protected static final String OTEL_REPO_PREFIX =
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-";
  protected static final String SPLUNK_REPO_PREFIX = "ghcr.io/signalfx/splunk-otel-";

  protected static final List<String> VMS_HOTSPOT = Collections.singletonList("hotspot");
  protected static final List<String> VMS_OPENJ9 = Collections.singletonList("openj9");
  protected static final List<String> VMS_ALL =
      Arrays.asList(VMS_HOTSPOT.get(0), VMS_OPENJ9.get(0));

  protected static class Configurations {
    private final String serverName;
    private final List<Arguments> items;

    private Configurations(String serverName) {
      this.serverName = serverName;
      this.items = new ArrayList<>();
    }

    public Configurations otelLinux(
        String version,
        String tag,
        ExpectedServerAttributes serverAttributes,
        List<String> vms,
        String... jdks) {
      ImageFactory imageFactory =
          (jdk) -> {
            String name = OTEL_REPO_PREFIX + serverName + ":" + version + "-jdk" + jdk + "-" + tag;
            return linuxImage(name);
          };

      addItems(serverAttributes, vms, jdks, imageFactory);
      return this;
    }

    public Configurations otelLinux(
        String version,
        ExpectedServerAttributes serverAttributes,
        List<String> vms,
        String... jdks) {
      return otelLinux(version, OTEL_IMAGE_VERSION, serverAttributes, vms, jdks);
    }

    public Configurations splunkLinux(
        String version,
        ExpectedServerAttributes serverAttributes,
        List<String> vms,
        String... jdks) {

      ImageFactory imageFactory =
          (jdk) -> {
            String name = SPLUNK_REPO_PREFIX + serverName + ":" + version + "-jdk" + jdk;
            return proprietaryLinuxImage(name);
          };

      addItems(serverAttributes, vms, jdks, imageFactory);
      return this;
    }

    public Configurations otelWindows(
        String version,
        ExpectedServerAttributes serverAttributes,
        List<String> vms,
        String... jdks) {

      ImageFactory imageFactory =
          (jdk) -> {
            String name =
                OTEL_REPO_PREFIX
                    + serverName
                    + ":"
                    + version
                    + "-jdk"
                    + jdk
                    + "-windows-"
                    + OTEL_IMAGE_VERSION;
            return windowsImage(name);
          };

      addItems(serverAttributes, vms, jdks, imageFactory);
      return this;
    }

    public Stream<Arguments> stream() {
      return items.stream();
    }

    private void addItems(
        ExpectedServerAttributes serverAttributes,
        List<String> vms,
        String[] jdks,
        ImageFactory imageFactory) {
      for (String vm : vms) {
        for (String jdk : jdks) {
          String jdkFull = jdk + ("hotspot".equals(vm) ? "" : "-" + vm);
          items.add(arguments(imageFactory.create(jdkFull), serverAttributes));
        }
      }
    }

    private interface ImageFactory {
      TestImage create(String jdk);
    }
  }

  protected static Configurations configurations(String serverName) {
    return new Configurations(serverName);
  }
}
