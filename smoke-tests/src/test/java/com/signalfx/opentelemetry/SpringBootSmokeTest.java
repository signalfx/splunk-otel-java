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

package com.signalfx.opentelemetry;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import java.io.IOException;
import java.util.Collection;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SpringBootSmokeTest extends SmokeTest {

  protected String getTargetImage(int jdk) {
    return "open-telemetry-docker-dev.bintray.io/java/smoke-springboot-jdk" + jdk + ":latest";
  }

  @Test
  public void springBootSmokeTestOnJDK() throws IOException, InterruptedException {
    startTarget(8);
    String url = String.format("http://localhost:%d/greeting", target.getMappedPort(8080));
    Request request = new Request.Builder().url(url).get().build();

    Object currentAgentVersion =
        new JarFile(agentPath)
            .getManifest()
            .getMainAttributes()
            .get(Attributes.Name.IMPLEMENTATION_VERSION);

    Response response = client.newCall(request).execute();
    Collection<ExportTraceServiceRequest> traces = waitForTraces();

    Assertions.assertEquals(response.body().string(), "Hi!");
    Assertions.assertEquals(1, countSpansByName(traces, "/greeting"));
    Assertions.assertEquals(1, countSpansByName(traces, "webcontroller.greeting"));
    Assertions.assertEquals(1, countSpansByName(traces, "webcontroller.withspan"));
    Assertions.assertEquals(
        3,
        getSpanStream(traces)
            .flatMap(s -> s.getAttributesList().stream())
            .filter(a -> a.getKey().equals("otel.instrumentation_library.version"))
            .map(a -> a.getValue().getStringValue())
            .filter(s -> s.equals(currentAgentVersion))
            .count());
    Assertions.assertEquals(
        3,
        getSpanStream(traces)
            .flatMap(s -> s.getAttributesList().stream())
            .filter(a -> a.getKey().equals("signalfx.instrumentation_library.version"))
            .map(a -> a.getValue().getStringValue())
            .filter(s -> s.equals(currentAgentVersion))
            .count());

    stopTarget();

    //    where:
    //    jdk << [8, 11, 14]
  }
}
