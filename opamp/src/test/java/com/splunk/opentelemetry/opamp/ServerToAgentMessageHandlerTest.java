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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.splunk.opamp.remotecontrol.CommandDispatcher;
import io.opentelemetry.opamp.client.OpampClient;
import io.opentelemetry.opamp.client.internal.response.MessageData;
import okio.ByteString;
import opamp.proto.AgentRemoteConfig;
import opamp.proto.CustomMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServerToAgentMessageHandlerTest {

  @Mock RemoteConfigProcessor remoteConfigProcessor;
  @Mock CommandDispatcher commandDispatcher;
  @Mock OpampClient opampClient;
  private ServerToAgentMessageHandler handler;

  @BeforeEach
  void setUp() {
    handler = new ServerToAgentMessageHandler(remoteConfigProcessor, commandDispatcher);
  }

  @Test
  void dispatchesMatchingCustomCommand() {
    String body = "thread.dump\njob-123\n3\n250";
    MessageData message =
        MessageData.builder()
            .setCustomMessage(
                customMessage(
                    ServerToAgentMessageHandler.CMD_CAPABILITY,
                    ServerToAgentMessageHandler.CMD_TYPE,
                    body))
            .build();

    handler.handleMessage(message, opampClient);

    verify(commandDispatcher).dispatch(body);
    verifyNoInteractions(remoteConfigProcessor, opampClient);
  }

  @Test
  void ignoresCustomMessageForDifferentCapability() {
    MessageData message =
        MessageData.builder()
            .setCustomMessage(
                customMessage(
                    "com.example.other/v1",
                    ServerToAgentMessageHandler.CMD_TYPE,
                    "thread.dump\njob-123"))
            .build();

    handler.handleMessage(message, opampClient);

    verifyNoInteractions(commandDispatcher, remoteConfigProcessor, opampClient);
  }

  @Test
  void ignoresCustomMessageForDifferentType() {
    MessageData message =
        MessageData.builder()
            .setCustomMessage(
                customMessage(
                    ServerToAgentMessageHandler.CMD_CAPABILITY,
                    "not-a-command",
                    "thread.dump\njob-123"))
            .build();

    handler.handleMessage(message, opampClient);

    verifyNoInteractions(commandDispatcher, remoteConfigProcessor, opampClient);
  }

  @Test
  void handlesRemoteConfigAndCustomCommandFromSameMessage() {
    String body = "thread.dump\njob-123";
    AgentRemoteConfig remoteConfig = new AgentRemoteConfig.Builder().build();
    MessageData message =
        MessageData.builder()
            .setRemoteConfig(remoteConfig)
            .setCustomMessage(
                customMessage(
                    ServerToAgentMessageHandler.CMD_CAPABILITY,
                    ServerToAgentMessageHandler.CMD_TYPE,
                    body))
            .build();

    handler.handleMessage(message, opampClient);

    verify(remoteConfigProcessor).applyConfig(remoteConfig, opampClient);
    verify(commandDispatcher).dispatch(body);
  }

  private static CustomMessage customMessage(String capability, String type, String body) {
    return new CustomMessage.Builder()
        .capability(capability)
        .type(type)
        .data(ByteString.encodeUtf8(body))
        .build();
  }
}
