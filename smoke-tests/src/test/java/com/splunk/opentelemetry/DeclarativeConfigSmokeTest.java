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
import static org.assertj.core.api.Assertions.assertThat;

import com.splunk.opentelemetry.helper.ResourceMapping;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class DeclarativeConfigSmokeTest extends SmokeTest {
  @Override
  protected List<ResourceMapping> getExtraResources() {
    return List.of(
        // declarative configuration file
        ResourceMapping.of("declarative_config_file.yaml", "/declarative_config_file.yaml"));
  }

  @Override
  protected Map<String, String> getExtraEnv() {
    return Map.of("OTEL_CONFIG_FILE", "/declarative_config_file.yaml");
  }

  @ParameterizedTest(name = "{index} => DeclarativeConfig SmokeTest On JDK{0}.")
  @ValueSource(ints = {8, 17})
  void declarativeConfigSmokeTestOnJDK(int jdk) throws IOException, InterruptedException {
    // given
    startTargetOrSkipTest(
        linuxImage(
            "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-spring-boot:jdk"
                + jdk
                + "-20230509.4927886820"));

    Request request = new Request.Builder().url(getUrl("/greeting", false)).get().build();

    // when
    Response response = client.newCall(request).execute();

    // then
    // Just confirm that application started and any telemetry is reported
    assertThat(response.body().string()).isEqualTo("Hi!");
    TraceInspector traces = waitForTraces();
    assertThat(traces.getTraceIds()).hasSize(1);

    // cleanup
    stopTarget();
  }
}
