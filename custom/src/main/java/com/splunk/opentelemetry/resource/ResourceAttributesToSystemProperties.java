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
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeType;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.javaagent.spi.ComponentInstaller;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
    Attributes attributes = getResource().getAttributes();
    attributes.forEach(
        (k, v) -> {
          if (k.getType() == AttributeType.STRING) {
            System.setProperty("otel.resource." + k.getKey(), v.toString());
          }
        });
  }

  private static Resource getResource() {
    Method unobfuscate = null;
    Field sharedStateField = null;
    Field resourceField = null;
    try {
      OpenTelemetrySdk openTelemetry = (OpenTelemetrySdk) GlobalOpenTelemetry.get();
      TracerProvider obfuscated = openTelemetry.getTracerProvider();
      unobfuscate = obfuscated.getClass().getDeclaredMethod("unobfuscate");
      unobfuscate.setAccessible(true);
      SdkTracerProvider tracerProvider = (SdkTracerProvider) unobfuscate.invoke(obfuscated);
      sharedStateField = SdkTracerProvider.class.getDeclaredField("sharedState");
      sharedStateField.setAccessible(true);
      Object sharedState = sharedStateField.get(tracerProvider);
      resourceField = sharedState.getClass().getDeclaredField("resource");
      resourceField.setAccessible(true);
      return (Resource) resourceField.get(sharedState);
    } catch (Exception e) {
      return Resource.getDefault();
    } finally {
      if (unobfuscate != null) {
        unobfuscate.setAccessible(false);
      }
      if (sharedStateField != null) {
        unobfuscate.setAccessible(false);
      }
      if (resourceField != null) {
        unobfuscate.setAccessible(false);
      }
    }
  }
}
