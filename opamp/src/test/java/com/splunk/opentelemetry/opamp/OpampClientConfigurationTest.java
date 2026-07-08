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

package com.splunk.opentelemetry.opamp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpampClientConfigurationTest {

  @Test
  void shouldConfigureAccessToken() {
    OpampClientConfiguration configuration =
        OpampClientConfiguration.builder().withAccessToken("secret-token").build();

    assertThat(configuration.getAccessToken()).isEqualTo("secret-token");
    assertThat(configuration.toString())
        .contains("accessToken='***redacted***'")
        .doesNotContain("secret-token");
  }

  @Test
  void shouldDefaultAccessTokenToNull() {
    OpampClientConfiguration configuration = OpampClientConfiguration.builder().build();

    assertThat(configuration.getAccessToken()).isNull();
    assertThat(configuration.toString()).contains("accessToken='<null>'");
  }
}
