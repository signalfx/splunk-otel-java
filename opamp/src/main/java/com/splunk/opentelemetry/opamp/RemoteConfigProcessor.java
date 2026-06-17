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

import static io.opentelemetry.api.incubator.config.DeclarativeConfigProperties.empty;

import com.google.common.annotations.VisibleForTesting;
import com.splunk.opentelemetry.profiler.ProfilerDeclarativeConfiguration;
import com.splunk.opentelemetry.profiler.ProfilingSupervisor;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.opamp.client.OpampClient;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import opamp.proto.AgentConfigFile;
import opamp.proto.AgentRemoteConfig;
import opamp.proto.RemoteConfigStatus;
import opamp.proto.RemoteConfigStatuses;

public class RemoteConfigProcessor {
  private static final Logger logger = Logger.getLogger(RemoteConfigProcessor.class.getName());

  private static final String REMOTE_CONFIG_FILE_NAME = "splunk.remote.config";
  private static final String PROFILING_NODE_NAME = "profiling";

  public ProfilingSupervisor profilingSupervisor;

  public RemoteConfigProcessor(ProfilingSupervisor profilingSupervisor) {
    this.profilingSupervisor = Objects.requireNonNull(profilingSupervisor);
  }

  public void applyConfig(AgentRemoteConfig remoteConfig, OpampClient opampClient) {
    Objects.requireNonNull(opampClient);

    Map<String, AgentConfigFile> configMap = remoteConfig.config.config_map;
    AgentConfigFile configFile = configMap.get(REMOTE_CONFIG_FILE_NAME);
    if (configFile == null) {
      logger.warning(
          "Received server message with remote config, but config is missing '"
              + REMOTE_CONFIG_FILE_NAME
              + "' file. Files provided: "
              + configMap.keySet());
      return;
    }

    // TODO: provide implementation
    DeclarativeConfigProperties remoteConfigProperties = toDeclarativeConfigProperties(configFile);
    DeclarativeConfigProperties splunkDistributionConfigProperties =
        remoteConfigProperties
            .getStructured("distribution", empty())
            .getStructured("splunk", empty());

    // Update profiler configuration only when profiling node exists
    if (!splunkDistributionConfigProperties.getPropertyKeys().contains(PROFILING_NODE_NAME)) {
      return;
    }

    DeclarativeConfigProperties profilingConfigProperties =
        splunkDistributionConfigProperties.getStructured(PROFILING_NODE_NAME, empty());
    ProfilerDeclarativeConfiguration profilingConfig =
        new ProfilerDeclarativeConfiguration(profilingConfigProperties);
    // TODO: should be merged with current profiling config. Probably we will need profiler
    //       configuration refactoring and some listeners implemented for profiler configuration
    //       changes. For POC use this temporary solution
    if (profilingConfig.isEnabled()) {
      profilingSupervisor.requestStartProfiling();
    } else {
      profilingSupervisor.requestStopProfiling();
    }

    // Confirm to the OpAMP Server that remote config has been applied.
    opampClient.setRemoteConfigStatus(
        new RemoteConfigStatus.Builder()
            .last_remote_config_hash(remoteConfig.config_hash)
            .status(RemoteConfigStatuses.RemoteConfigStatuses_APPLIED)
            .build());
  }

  @VisibleForTesting
  static DeclarativeConfigProperties toDeclarativeConfigProperties(AgentConfigFile configFile) {
    return DeclarativeConfiguration.toConfigProperties(
        new ByteArrayInputStream(configFile.body.toByteArray()));
  }
}
