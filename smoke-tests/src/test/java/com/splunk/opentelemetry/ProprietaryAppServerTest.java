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

import org.junit.jupiter.api.Assertions;
import org.testcontainers.DockerClientFactory;

/**
 * This is the base class for smoke test for OpenTelemetry Java agent with proprietary app-servers.
 * See the manual in `matrix` sub-project for instructions on how to build required images.
 */
abstract class ProprietaryAppServerTest extends AppServerTest {

  protected void additionalWebAppTraceAssertions(
      TraceInspector traces, ExpectedServerAttributes serverAttributes) {
    Assertions.assertEquals(
        1, traces.countSpansByName("GreetingServlet.withSpan"), "Span for the annotated method");
  }

  protected int totalNumberOfSpansInWebappTrace() {
    // test app in proprietary images also has one additional span for WithSpan annotation.
    return super.totalNumberOfSpansInWebappTrace() + 1;
  }

  protected boolean localDockerImageIsPresent(String imageName) {
    try {
      DockerClientFactory.lazyClient().inspectImageCmd(imageName).exec();
      return true;
    } catch (Exception e) {
      System.out.println(e.getMessage());
      return false;
    }
  }
}
