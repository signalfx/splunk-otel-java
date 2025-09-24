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
public class ProcessResourceDetectorCustomizer
    implements DeclarativeConfigurationCustomizerProvider {

  private static final Logger logger =
      Logger.getLogger(ProcessResourceDetectorCustomizer.class.getName());

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

          // Use standard ProcessResourceComponentProvider resource detector from the upstream, or
          // truncate command line resource detector depending on the settings
          ExperimentalResourceDetectorModel truncatingDetector =
              new ExperimentalResourceDetectorModel();
          String detectorName =
              forceFullCommandline ? "process" : TruncateCommandLineResourceDetector.PROVIDER_NAME;
          truncatingDetector.setAdditionalProperty(detectorName, null);

          if (!detectors.contains(truncatingDetector)) {
            detectors.add(truncatingDetector);
          }

          return model;
        });
  }

  @Override
  public int order() {
    // Need to run late so that we can override other resource customizers
    return Integer.MAX_VALUE;
  }
}
