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

package com.splunk.opentelemetry.webengine;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;

@SuppressWarnings("rawtypes")
@AutoService(ComponentProvider.class)
public class WebengineSpanProcessorComponentProvider implements ComponentProvider<SpanProcessor> {
  static String PROVIDER_NAME = "web_engine";

  @Override
  public Class<SpanProcessor> getType() {
    return SpanProcessor.class;
  }

  // TODO: Add to Splunk specific yaml config file format documentation
  @Override
  public String getName() {
    return PROVIDER_NAME;
  }

  @Override
  public SpanProcessor create(DeclarativeConfigProperties config) {
    return new WebengineAttributeSpanProcessor();
  }
}
