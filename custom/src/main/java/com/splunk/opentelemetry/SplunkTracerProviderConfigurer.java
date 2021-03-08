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

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.SdkTracerProviderConfigurer;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanLimits;
import io.opentelemetry.sdk.trace.samplers.Sampler;

@AutoService(SdkTracerProviderConfigurer.class)
public class SplunkTracerProviderConfigurer implements SdkTracerProviderConfigurer {
  @Override
  public void configure(SdkTracerProviderBuilder tracerProvider) {
    tracerProvider
        .setSampler(Sampler.alwaysOn())
        .setSpanLimits(
            SpanLimits.builder()
                .setMaxNumberOfAttributes(Integer.MAX_VALUE)
                .setMaxNumberOfEvents(Integer.MAX_VALUE)
                .setMaxNumberOfLinks(1000)
                .setMaxNumberOfAttributesPerEvent(Integer.MAX_VALUE)
                .setMaxNumberOfAttributesPerLink(Integer.MAX_VALUE)
                .build());
  }
}
