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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.Test;

class AppDynamicsVersionResourceDetectorTest {
  @Test
  void shouldDetectAppDynamicsVersion() {
    try {
      // given
      String appDVersion = "version X";
      System.setProperty(
          AppDynamicsVersionResourceDetector.APPD_AGENT_VER_KEY.getKey(), appDVersion);
      AppDynamicsVersionResourceDetector detector = new AppDynamicsVersionResourceDetector();

      // when
      Resource resource = detector.create(null);

      // then
      assertThat(
              resource.getAttributes().get(AppDynamicsVersionResourceDetector.APPD_AGENT_VER_KEY))
          .isEqualTo(appDVersion);
    } finally {
      System.clearProperty(AppDynamicsVersionResourceDetector.APPD_AGENT_VER_KEY.getKey());
    }
  }

  @Test
  void shouldHandleMissingAppDynamicsVersion() {
    // given
    AppDynamicsVersionResourceDetector detector = new AppDynamicsVersionResourceDetector();

    // when
    Resource resource = detector.create(null);

    // then
    assertThat(resource.getAttributes().isEmpty()).isTrue();
  }
}
