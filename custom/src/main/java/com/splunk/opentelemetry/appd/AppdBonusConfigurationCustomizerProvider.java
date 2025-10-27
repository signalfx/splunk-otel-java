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

package com.splunk.opentelemetry.appd;

import static io.opentelemetry.sdk.autoconfigure.AdditionalPropertiesUtil.getAdditionalPropertyOrDefault;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.PropagatorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.TracerProviderModel;
import java.util.Map;

@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public final class AppdBonusConfigurationCustomizerProvider
    implements DeclarativeConfigurationCustomizerProvider {

  private static final String CONFIG_CISCO_CTX_ENABLED = "cisco.ctx.enabled";

  @Override
  public void customize(DeclarativeConfigurationCustomizer autoConfiguration) {
    autoConfiguration.addModelCustomizer(
        model -> {
          if (model.getInstrumentationDevelopment() == null
              || model.getInstrumentationDevelopment().getJava() == null) {
            return model;
          }
          Map<String, Object> properties =
              model.getInstrumentationDevelopment().getJava().getAdditionalProperties();

          if (isFeatureEnabled(model, properties)) {
            if (maybeAddAppdBonusPropagator(model)) {
              // Appd propagator has been added so add also a corresponding Appd span processor
              SpanProcessorModel appdSpanProcessorModel =
                  new SpanProcessorModel()
                      .withAdditionalProperty(
                          AppdBonusSpanProcessorComponentProvider.NAME, null);
              if (model.getTracerProvider() == null) {
                model.withTracerProvider(new TracerProviderModel());
              }
              model.getTracerProvider().getProcessors().add(appdSpanProcessorModel);
            }
          }

          return model;
        });
  }

  private static boolean isFeatureEnabled(
      OpenTelemetryConfigurationModel model, Map<String, Object> properties) {
    return (getAdditionalPropertyOrDefault(properties, CONFIG_CISCO_CTX_ENABLED, false));
  }

  private static boolean canAddPropagator(String compositeList) {
    for (String propagatorNames : compositeList.split(",")) {
      if (propagatorNames.trim().equals("none")) {
        return false;
      }
    }
    return true;
  }

  private static boolean maybeAddAppdBonusPropagator(OpenTelemetryConfigurationModel model) {
    if (model.getPropagator() == null) {
      model.withPropagator(new PropagatorModel());
    }

    String compositeList = model.getPropagator().getCompositeList();
    if (compositeList == null) {
      compositeList = "";
    } else if (!canAddPropagator(compositeList)) {
      return false;
    }

    // Possible duplicates are handled by the upstream
    compositeList =
        compositeList.isEmpty()
            ? AppdBonusPropagator.NAME
            : AppdBonusPropagator.NAME + "," + compositeList;

    model.getPropagator().withCompositeList(compositeList);

    return true;
  }
}
