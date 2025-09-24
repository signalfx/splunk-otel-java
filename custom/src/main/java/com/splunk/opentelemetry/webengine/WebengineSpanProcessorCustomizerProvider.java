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
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanProcessorModel;
import java.util.logging.Logger;

@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public class WebengineSpanProcessorCustomizerProvider
    implements DeclarativeConfigurationCustomizerProvider {
  private static final Logger logger =
      Logger.getLogger(WebengineSpanProcessorCustomizerProvider.class.getName());

  @Override
  public void customize(DeclarativeConfigurationCustomizer autoConfiguration) {
    autoConfiguration.addModelCustomizer(
        model -> {
          if (model.getTracerProvider() != null) {
            logger.fine("Adding webengine span processor to the configuration");
            model
                .getTracerProvider()
                .getProcessors()
                .add(
                    0,
                    new SpanProcessorModel()
                        .withAdditionalProperty(
                            WebengineSpanProcessorComponentProvider.PROVIDER_NAME, null));
          }
          return model;
        });
  }

  @Override
  public int order() {
    // Make sure other customizers had a chance to add trace providers.
    return Integer.MAX_VALUE - 1;
  }
}
