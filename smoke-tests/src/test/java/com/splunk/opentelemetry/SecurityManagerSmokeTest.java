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
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.splunk.opentelemetry.helper.TargetWaitStrategy;
import com.splunk.opentelemetry.helper.TestImage;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class SecurityManagerSmokeTest extends SmokeTest {

  private TestImage getTargetImage(int jdk) {
    return linuxImage(
        "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-security-manager:jdk"
            + jdk
            + "-20230323.4502979551");
  }

  @Override
  protected Map<String, String> getExtraEnv() {
    return Map.of(
        "OTEL_JAVAAGENT_EXPERIMENTAL_SECURITY_MANAGER_SUPPORT_ENABLED",
        "true",
        // AbstractTestContainerManager sets sampler to "internal_root_off" which isn't suitable for
        // this test, overwrite it
        "OTEL_TRACES_SAMPLER",
        "always_on");
  }

  @Override
  protected TargetWaitStrategy getWaitStrategy() {
    return new TargetWaitStrategy.Log(Duration.ofMinutes(1), ".*opentelemetry-javaagent.*");
  }

  @ParameterizedTest(name = "{index} => SecurityManager SmokeTest On JDK{0}.")
  @ValueSource(ints = {8, 11, 17, 19})
  void securityManagerSmokeTestOnJDK(int jdk) throws IOException, InterruptedException {
    startTargetOrSkipTest(getTargetImage(jdk));

    assertEquals(1, waitForTraces().countSpansByName("test"));

    stopTarget();
  }
}
