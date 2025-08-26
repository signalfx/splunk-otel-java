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
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanLimitsModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.TracerProviderModel;
import java.util.logging.Logger;

@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public class SplunkSpanLimitsCustomizerProvider
    implements DeclarativeConfigurationCustomizerProvider {
  private static final Logger logger =
      Logger.getLogger(SplunkSpanLimitsCustomizerProvider.class.getName());

  private static final int SPLUNK_MAX_ATTRIBUTE_VALUE_LENGTH = 12000;
  private static final int SPLUNK_MAX_LINK_COUNT = 1000;

  @Override
  public void customize(DeclarativeConfigurationCustomizer autoConfiguration) {
    autoConfiguration.addModelCustomizer(
        model -> {
          TracerProviderModel tracerProviderModel = model.getTracerProvider();
          if (tracerProviderModel == null) {
            return model;
          }

          if (tracerProviderModel.getLimits() == null) {
            SpanLimitsModel spanLimitsModel = new SpanLimitsModel();
            tracerProviderModel.withLimits(spanLimitsModel);

            spanLimitsModel
                .withAttributeCountLimit(Integer.MAX_VALUE)
                .withEventCountLimit(Integer.MAX_VALUE)
                .withLinkCountLimit(SPLUNK_MAX_LINK_COUNT)
                .withEventAttributeCountLimit(Integer.MAX_VALUE)
                .withLinkAttributeCountLimit(Integer.MAX_VALUE)
                .withAttributeValueLengthLimit(SPLUNK_MAX_ATTRIBUTE_VALUE_LENGTH);

            logger.fine(() -> "Span limits set to defaults: " + spanLimitsModel);
          } else {
            logger.fine("Span limits defined in the configuration model.");
          }
          return model;
        });
  }

  @Override
  public int order() {
    // Make sure other customizers had a chance to add trace providers
    return Integer.MAX_VALUE - 1;
  }
}
