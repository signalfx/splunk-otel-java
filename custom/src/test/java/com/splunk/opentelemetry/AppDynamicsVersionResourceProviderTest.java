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

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.Test;

class AppDynamicsVersionResourceProviderTest {

  @Test
  void noConfigValue() {
    var provider = new AppDynamicsVersionResourceProvider();
    ConfigProperties config = mock();
    Resource result = provider.createResource(config);
    assertThat(result).isSameAs(Resource.empty());
  }

  @Test
  void addsAppDynamicsVersion() {
    var provider = new AppDynamicsVersionResourceProvider();
    ConfigProperties config = mock();
    when(config.getString("appdynamics.agent.version")).thenReturn("1.2.3-20250910");
    Resource result = provider.createResource(config);
    assertThat(result.getAttribute(stringKey("appdynamics.agent.version")))
        .isEqualTo("1.2.3-20250910");
  }
}
