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

import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_VERSION;
import static io.opentelemetry.semconv.incubating.DeploymentIncubatingAttributes.DEPLOYMENT_ENVIRONMENT_NAME;
import static io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_NAME;
import static io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_TYPE;
import static io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_VERSION;
import static io.opentelemetry.semconv.incubating.ServiceIncubatingAttributes.SERVICE_NAMESPACE;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.opamp.client.OpampClient;
import io.opentelemetry.opamp.client.internal.response.MessageData;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.testing.internal.armeria.common.HttpResponse;
import io.opentelemetry.testing.internal.armeria.common.HttpStatus;
import io.opentelemetry.testing.internal.armeria.common.MediaType;
import io.opentelemetry.testing.internal.armeria.testing.junit5.server.mock.MockWebServerExtension;
import io.opentelemetry.testing.internal.armeria.testing.junit5.server.mock.RecordedRequest;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import okio.ByteString;
import opamp.proto.AgentConfigFile;
import opamp.proto.AgentConfigMap;
import opamp.proto.AgentRemoteConfig;
import opamp.proto.AgentToServer;
import opamp.proto.AnyValue;
import opamp.proto.KeyValue;
import opamp.proto.ServerErrorResponse;
import opamp.proto.ServerToAgent;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class OpampActivatorTest {
  private static final MockWebServerExtension server = new MockWebServerExtension();

  @RegisterExtension static final AutoCleanupExtension cleanup = AutoCleanupExtension.create();

  @BeforeAll
  static void setUp() {
    server.start();
  }

  @BeforeEach
  void reset() {
    server.beforeTestExecution(null);
  }

  @AfterAll
  static void cleanUp() {
    server.stop();
  }

  @Test
  void testOpamp() throws Exception {
    // given
    Attributes attributes =
        Attributes.of(
                DEPLOYMENT_ENVIRONMENT_NAME,
                "test-deployment-env",
                SERVICE_NAME,
                "test-service",
                SERVICE_VERSION,
                "test-ver",
                SERVICE_NAMESPACE,
                "test-ns")
            .toBuilder()
            .put(OS_NAME, "test-os-name")
            .put(OS_TYPE, "test-os-type")
            .put(OS_VERSION, "test-os-ver")
            .build();
    Resource resource = Resource.create(attributes);
    Map<String, AgentConfigFile> configMap =
        Collections.singletonMap(
            "test-key",
            new AgentConfigFile.Builder().body(ByteString.encodeUtf8("test-value")).build());
    ServerToAgent response =
        new ServerToAgent.Builder()
            .remote_config(
                new AgentRemoteConfig.Builder()
                    .config(new AgentConfigMap.Builder().config_map(configMap).build())
                    .build())
            .build();
    server.enqueue(HttpResponse.of(HttpStatus.OK, MediaType.X_PROTOBUF, response.encode()));

    CompletableFuture<MessageData> result = new CompletableFuture<>();
    OpampClient client =
        OpampActivator.startOpampClient(
            server.httpUri().toString(),
            resource,
            new OpampClient.Callbacks() {
              @Override
              public void onConnect(OpampClient opampClient) {}

              @Override
              public void onConnectFailed(OpampClient opampClient, @Nullable Throwable throwable) {
                result.completeExceptionally(throwable);
              }

              @Override
              public void onErrorResponse(
                  OpampClient opampClient, ServerErrorResponse serverErrorResponse) {
                result.completeExceptionally(
                    new IllegalStateException(serverErrorResponse.toString()));
              }

              @Override
              public void onMessage(OpampClient opampClient, MessageData messageData) {
                result.complete(messageData);
              }
            });
    cleanup.deferCleanup(client);

    // when
    MessageData message = result.get(5, TimeUnit.SECONDS);
    AgentRemoteConfig remoteConfig = message.getRemoteConfig();

    // then
    assertThat(remoteConfig).isNotNull();
    assertThat(remoteConfig.config.config_map.get("test-key").body.utf8()).isEqualTo("test-value");

    RecordedRequest recordedRequest = server.takeRequest();
    byte[] body = recordedRequest.request().content().array();
    AgentToServer agentToServer = AgentToServer.ADAPTER.decode(body);
    assertIdentifying(agentToServer, DEPLOYMENT_ENVIRONMENT_NAME, "test-deployment-env");
    assertIdentifying(agentToServer, SERVICE_NAME, "test-service");
    assertIdentifying(agentToServer, SERVICE_VERSION, "test-ver");
    assertIdentifying(agentToServer, SERVICE_NAMESPACE, "test-ns");
    assertNonIdentifying(agentToServer, OS_NAME, "test-os-name");
    assertNonIdentifying(agentToServer, OS_TYPE, "test-os-type");
    assertNonIdentifying(agentToServer, OS_VERSION, "test-os-ver");
  }

  private void assertIdentifying(
      AgentToServer agentToServer, AttributeKey<String> attr, String expected) {
    KeyValue kv =
        new KeyValue(attr.getKey(), new AnyValue.Builder().string_value(expected).build());
    assertThat(agentToServer.agent_description.identifying_attributes).contains(kv);
  }

  private void assertNonIdentifying(
      AgentToServer agentToServer, AttributeKey<String> attr, String expected) {
    KeyValue kv =
        new KeyValue(attr.getKey(), new AnyValue.Builder().string_value(expected).build());
    assertThat(agentToServer.agent_description.non_identifying_attributes).contains(kv);
  }
}
