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

import static com.splunk.opentelemetry.SplunkConfiguration.METRICS_FULL_COMMAND_LINE;
import static io.opentelemetry.sdk.autoconfigure.AdditionalPropertiesUtil.getAdditionalPropertyOrDefault;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ExperimentalResourceDetectionModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ExperimentalResourceDetectorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ResourceModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public class TruncateCommandLineCustomizerProvider
    implements DeclarativeConfigurationCustomizerProvider {

  private static final Logger logger =
      Logger.getLogger(TruncateCommandLineCustomizerProvider.class.getName());

  @Override
  public void customize(DeclarativeConfigurationCustomizer autoConfiguration) {
    autoConfiguration.addModelCustomizer(
        model -> {
          if ((model.getInstrumentationDevelopment() == null)
              || (model.getInstrumentationDevelopment().getJava() == null)) {
            return model;
          }

          Map<String, Object> javaConfigProperties =
              model.getInstrumentationDevelopment().getJava().getAdditionalProperties();
          boolean forceFullCommandline =
              Boolean.parseBoolean(
                  getAdditionalPropertyOrDefault(
                          javaConfigProperties, METRICS_FULL_COMMAND_LINE, false)
                      .toString());

          if (!forceFullCommandline) {
            ResourceModel resourceModel = model.getResource();
            if (resourceModel == null) {
              resourceModel = new ResourceModel();
              model.withResource(resourceModel);
            }

            ExperimentalResourceDetectionModel detectionDevelopment =
                resourceModel.getDetectionDevelopment();
            if (detectionDevelopment == null) {
              detectionDevelopment = new ExperimentalResourceDetectionModel();
              resourceModel.withDetectionDevelopment(detectionDevelopment);
            }

            List<ExperimentalResourceDetectorModel> detectors = detectionDevelopment.getDetectors();
            if (detectors == null) {
              detectors = new ArrayList<>();
              detectionDevelopment.withDetectors(detectors);
            }

            ExperimentalResourceDetectorModel truncatingDetector =
                new ExperimentalResourceDetectorModel();
            truncatingDetector.setAdditionalProperty(
                TruncateCommandLineResourceDetectorComponentProvider.PROVIDER_NAME, null);
            if (!detectors.contains(truncatingDetector)) {
              detectors.add(truncatingDetector);
            }
          }

          return model;
        });
  }

  @Override
  public int order() {
    // Need to run late so that we can override other resource customizers
    return 9999;
  }

  private static final int MAX_LENGTH = 255;

  public void applyCustomization(ResourceModel resourceModel, Map<String, Object> config) {
    boolean forceFullCommandline =
        Boolean.parseBoolean(
            getAdditionalPropertyOrDefault(config, METRICS_FULL_COMMAND_LINE, false).toString());

    if (!forceFullCommandline) {}
  }
}
