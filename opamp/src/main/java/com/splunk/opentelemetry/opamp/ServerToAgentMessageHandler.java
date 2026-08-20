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

import io.opentelemetry.opamp.client.OpampClient;
import io.opentelemetry.opamp.client.internal.response.MessageData;
import opamp.proto.AgentRemoteConfig;

public class ServerToAgentMessageHandler {
  private final RemoteConfigProcessor remoteConfigProcessor;

  ServerToAgentMessageHandler(RemoteConfigProcessor remoteConfigProcessor) {
    this.remoteConfigProcessor = remoteConfigProcessor;
  }

  public void handleMessage(MessageData message, OpampClient opampClient) {
    AgentRemoteConfig remoteConfig = message.getRemoteConfig();
    if (remoteConfig != null) {
      remoteConfigProcessor.applyConfig(remoteConfig, opampClient);
    }
  }
}
