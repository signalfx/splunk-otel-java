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

import com.splunk.opamp.remotecontrol.CommandDispatcher;
import io.opentelemetry.opamp.client.OpampClient;
import io.opentelemetry.opamp.client.internal.response.MessageData;
import opamp.proto.AgentRemoteConfig;
import opamp.proto.CustomMessage;

public class ServerToAgentMessageHandler {
  public static final String HACKY_CMD_CAPABILITY = "com.splunk.opamp.experimental_command/v1";
  public static final String HACKY_CMD_TYPE = "command";
  private final RemoteConfigProcessor remoteConfigProcessor;
  private final CommandDispatcher commandDispatcher;

  ServerToAgentMessageHandler(
      RemoteConfigProcessor remoteConfigProcessor, CommandDispatcher commandDispatcher) {
    this.remoteConfigProcessor = remoteConfigProcessor;
    this.commandDispatcher = commandDispatcher;
  }

  public void handleMessage(MessageData message, OpampClient opampClient) {
    AgentRemoteConfig remoteConfig = message.getRemoteConfig();
    if (remoteConfig != null) {
      remoteConfigProcessor.applyConfig(remoteConfig, opampClient);
    }
    CustomMessage customMessage = message.getCustomMessage();
    if (customMessage != null
        && HACKY_CMD_CAPABILITY.equals(customMessage.capability)
        && HACKY_CMD_TYPE.equals(customMessage.type)) {
      String body = customMessage.data.string(UTF_8);
      commandDispatcher.dispatch(customMessage.type, body);
    }
  }
}
