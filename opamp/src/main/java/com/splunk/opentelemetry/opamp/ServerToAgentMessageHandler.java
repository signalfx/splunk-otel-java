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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.annotations.VisibleForTesting;
import com.splunk.opamp.remotecontrol.CommandDispatcher;
import com.splunk.opentelemetry.opamp.effectiveconfig.EffectiveConfigReporter;
import com.splunk.opentelemetry.profiler.ProfilingSupervisor;
import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilingSupervisor;
import io.opentelemetry.opamp.client.OpampClient;
import io.opentelemetry.opamp.client.internal.response.MessageData;
import opamp.proto.AgentConfigFile;
import opamp.proto.AgentRemoteConfig;

public class ServerToAgentMessageHandler {
  public static final String MAGIC_CMD_STRING = "COMMAND_HACKS";
  private final RemoteConfigProcessor remoteConfigProcessor;
  private final CommandDispatcher commandDispatcher;

  public ServerToAgentMessageHandler(
      ProfilingSupervisor profilingSupervisor,
      SnapshotProfilingSupervisor snapshotProfilingSupervisor,
      EffectiveConfigReporter effectiveConfigReporter,
      CommandDispatcher commandDispatcher) {
    this(
        new RemoteConfigProcessor(
            profilingSupervisor, snapshotProfilingSupervisor, effectiveConfigReporter),
        commandDispatcher);
  }

  @VisibleForTesting
  ServerToAgentMessageHandler(
      RemoteConfigProcessor remoteConfigProcessor, CommandDispatcher commandDispatcher) {
    this.remoteConfigProcessor = remoteConfigProcessor;
    this.commandDispatcher = commandDispatcher;
  }

  public void handleMessage(MessageData message, OpampClient opampClient) {
    AgentRemoteConfig remoteConfig = message.getRemoteConfig();
    if (remoteConfig != null) {

      if (remoteConfig.config.config_map.containsKey(MAGIC_CMD_STRING)) {
        AgentConfigFile agentConfigFile = remoteConfig.config.config_map.get(MAGIC_CMD_STRING);
        String contentType = agentConfigFile.content_type;
        String body = agentConfigFile.body.string(UTF_8);
        commandDispatcher.dispatch(contentType, body);
        if (remoteConfig.config.config_map.size() == 1) { // just this command
          return;
        }
      }

      remoteConfigProcessor.applyConfig(remoteConfig, opampClient);
    }
  }
}
