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

import com.google.auto.service.AutoService;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.resources.Resource;

@AutoService(ComponentProvider.class)
public class AppDynamicsVersionResourceDetector implements ComponentProvider {
  static final AttributeKey<String> APPD_AGENT_VER_KEY = stringKey("appdynamics.agent.version");

  @Override
  public Class<Resource> getType() {
    return Resource.class;
  }

  @Override
  public String getName() {
    return "appd_agent_version";
  }

  @Override
  public Resource create(DeclarativeConfigProperties config) {
    // When AppD Agent starts Splunk Agent then it passes its version to Splunk Agent as a system
    // property
    String version = System.getProperty(APPD_AGENT_VER_KEY.getKey());
    if (version == null) {
      return Resource.empty();
    }

    Attributes attributes = Attributes.of(APPD_AGENT_VER_KEY, version);
    return Resource.create(attributes);
  }
}
