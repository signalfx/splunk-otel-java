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

package com.splunk.opentelemetry.sampler;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.splunk.opentelemetry.DeclarativeConfigTestExtension;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

class InternalRootOffSamplerComponentProviderTest {
  @RegisterExtension
  private static final DeclarativeConfigTestExtension configTestExtension =
      DeclarativeConfigTestExtension.create();

  @Test
  void shouldCreateSampler(@TempDir Path tempDir) throws IOException {
    String yaml =
        """
            file_format: "1.0-rc.1"
            tracer_provider:
              sampler:
                internal_root_off:
            """;

    OpenTelemetrySdk sdk =
        configTestExtension.createAutoConfiguredSdk(yaml, tempDir).getOpenTelemetrySdk();

    assertThat(sdk.getSdkTracerProvider().getSampler()).isInstanceOf(InternalRootOffSampler.class);
  }
}
