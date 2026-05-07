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

import com.google.auto.service.AutoService;
import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import javax.annotation.Nullable;

/** Intercept the declarative configuration model used to configure SDK. */
@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public class DeclarativeConfigurationInterceptor
    implements DeclarativeConfigurationCustomizerProvider {
  private static OpenTelemetryConfigurationModel configurationModel;

  @Nullable
  public static OpenTelemetryConfigurationModel getConfigurationModel() {
    return configurationModel;
  }

  @VisibleForTesting
  static void reset() {
    configurationModel = null;
  }

  @Override
  public void customize(DeclarativeConfigurationCustomizer customizer) {
    customizer.addModelCustomizer(
        (model) -> {
          configurationModel = model;
          return model;
        });
  }

  @Override
  public int order() {
    // Should be returned as the very last after all the customizations because in theory some
    // customizer can return new instance of model
    return Integer.MAX_VALUE;
  }
}
