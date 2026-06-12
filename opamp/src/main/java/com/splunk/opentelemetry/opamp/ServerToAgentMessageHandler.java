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

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.opamp.client.OpampClient;
import io.opentelemetry.opamp.client.internal.response.MessageData;

public class ServerToAgentMessageHandler {
  private final RemoteConfigProcessor remoteConfigProcessor;

  public ServerToAgentMessageHandler() {
    this(new RemoteConfigProcessor());
  }

  @VisibleForTesting
  ServerToAgentMessageHandler(RemoteConfigProcessor remoteConfigProcessor) {
    this.remoteConfigProcessor = remoteConfigProcessor;
  }

  public void handleMessage(MessageData message, OpampClient opampClient) {
    if (message.getRemoteConfig() != null) {
      remoteConfigProcessor.applyConfig(message.getRemoteConfig(), opampClient);
    }
  }
}
