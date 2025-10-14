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

package com.splunk.opentelemetry.resource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mockStatic;

import io.opentelemetry.instrumentation.resources.ProcessResource;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.incubating.ProcessIncubatingAttributes;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class TruncateCommandLineResourceDetectorTest {
  @Test
  public void shouldDoNothingWhenNoNeedToTruncateResourceAttributes() {
    try (MockedStatic<ProcessResource> mockedStatic = mockStatic(ProcessResource.class)) {
      // given
      List<String> args = List.of("ABC");
      String commandLine = "RUN";
      Resource detectedResource =
          Resource.builder()
              .put(ProcessIncubatingAttributes.PROCESS_COMMAND_ARGS, args)
              .put(ProcessIncubatingAttributes.PROCESS_COMMAND_LINE, commandLine)
              .build();
      mockedStatic.when(ProcessResource::get).thenReturn(detectedResource);

      TruncateCommandLineResourceDetector provider = new TruncateCommandLineResourceDetector();

      // when
      Resource resource = provider.create(null);

      // then
      assertThat(resource.getAttribute(ProcessIncubatingAttributes.PROCESS_COMMAND_ARGS))
          .isEqualTo(List.of("ABC"));
      assertThat(resource.getAttribute(ProcessIncubatingAttributes.PROCESS_COMMAND_LINE))
          .isEqualTo("RUN");
    }
  }

  @Test
  public void shouldTruncateProcessCommandLine() {
    try (MockedStatic<ProcessResource> mockedStatic = mockStatic(ProcessResource.class)) {
      // given
      List<String> args = List.of("X".repeat(512));
      String commandLine = "Y".repeat(512);
      Resource detectedResource =
          Resource.builder()
              .put(ProcessIncubatingAttributes.PROCESS_COMMAND_ARGS, args)
              .put(ProcessIncubatingAttributes.PROCESS_COMMAND_LINE, commandLine)
              .build();
      mockedStatic.when(ProcessResource::get).thenReturn(detectedResource);

      TruncateCommandLineResourceDetector provider = new TruncateCommandLineResourceDetector();

      // when
      Resource resource = provider.create(null);

      // then
      // TODO: Seems like a small bug. It should be 252 + ...
      List<String> expectedArgs = List.of("X".repeat(248) + "...");
      assertThat(resource.getAttribute(ProcessIncubatingAttributes.PROCESS_COMMAND_ARGS))
          .isEqualTo(expectedArgs);

      String expectedCommandLine = "Y".repeat(252) + "...";
      assertThat(resource.getAttribute(ProcessIncubatingAttributes.PROCESS_COMMAND_LINE))
          .isEqualTo(expectedCommandLine);
    }
  }
}
