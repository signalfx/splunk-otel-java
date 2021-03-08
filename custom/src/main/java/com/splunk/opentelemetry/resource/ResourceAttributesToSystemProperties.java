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

import com.google.auto.service.AutoService;
import io.opentelemetry.api.common.AttributeType;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.javaagent.spi.ComponentInstaller;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkAutoConfiguration;

/**
 * This component exposes String resource attributes as system properties with <code>otel.resource.
 * </code> prefix. Resource attributes represented as separate system properties then can be used by
 * logging frameworks to add resource attributes to logs without any additional instrumentation or
 * integration code by just using standard ways to access system properties in logging patterns.
 */
@AutoService(ComponentInstaller.class)
public class ResourceAttributesToSystemProperties implements ComponentInstaller {

  @Override
  public void beforeByteBuddyAgent() {}

  @Override
  public void afterByteBuddyAgent() {
    Attributes attributes = OpenTelemetrySdkAutoConfiguration.getResource().getAttributes();
    attributes.forEach(
        (k, v) -> {
          if (k.getType() == AttributeType.STRING) {
            System.setProperty("otel.resource." + k.getKey(), v.toString());
          }
        });
  }
}
