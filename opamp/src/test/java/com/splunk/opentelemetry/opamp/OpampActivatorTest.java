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

import static com.splunk.opentelemetry.opamp.OpampActivator.buildEffectiveConfig;
import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.api.common.AttributeKey.doubleKey;
import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.valueKey;
import static io.opentelemetry.semconv.DeploymentAttributes.DEPLOYMENT_ENVIRONMENT_NAME;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_INSTANCE_ID;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAMESPACE;
import static io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_NAME;
import static io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_TYPE;
import static io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_VERSION;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.opamp.client.OpampClient;
import io.opentelemetry.opamp.client.internal.response.MessageData;
import io.opentelemetry.opamp.client.internal.state.State;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.testing.internal.armeria.common.HttpResponse;
import io.opentelemetry.testing.internal.armeria.common.HttpStatus;
import io.opentelemetry.testing.internal.armeria.common.MediaType;
import io.opentelemetry.testing.internal.armeria.testing.junit5.server.mock.MockWebServerExtension;
import io.opentelemetry.testing.internal.armeria.testing.junit5.server.mock.RecordedRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import okio.ByteString;
import opamp.proto.AgentConfigFile;
import opamp.proto.AgentConfigMap;
import opamp.proto.AgentRemoteConfig;
import opamp.proto.AgentToServer;
import opamp.proto.AnyValue;
import opamp.proto.ArrayValue;
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
                SERVICE_INSTANCE_ID,
                "test-instance",
                SERVICE_NAMESPACE,
                "test-ns")
            .toBuilder()
            .put(OS_NAME, "test-os-name")
            .put(OS_TYPE, "test-os-type")
            .put(OS_VERSION, "test-os-ver")
            .put(longKey("long"), 12L)
            .put(doubleKey("double"), 99.0)
            .put(booleanKey("bool"), true)
            .put(valueKey("val"), Value.of("vvv"))
            .put("longarr", new long[] {2L, 3L, 5L})
            .put(AttributeKey.longArrayKey("longobjarr"), Arrays.asList(2L, 3L, 5L))
            .put("doublearr", new double[] {2.0, 3.0})
            .put(AttributeKey.doubleArrayKey("doubleobjarr"), Arrays.asList(5.0, 6.0))
            .put("stringarr", new String[] {"foo", "flimflam"})
            .put(AttributeKey.stringArrayKey("stringobjarr"), Arrays.asList("flim", "jibberjo"))
            .put("boolarr", new boolean[] {true, false})
            .put(AttributeKey.booleanArrayKey("boolobjarr"), Arrays.asList(true, true, false, true))
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

    ConfigProperties config = DefaultConfigProperties.createFromMap(Map.of());
    State.EffectiveConfig effectiveConfig =
        buildEffectiveConfig(new EnvVarsEffectiveConfigFileFactory(config));

    CompletableFuture<MessageData> result = new CompletableFuture<>();
    OpampClient client =
        OpampActivator.startOpampClient(
            effectiveConfig,
            server.httpUri().toString(),
            resource,
            500,
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

    assertIdentifyingString(agentToServer, SERVICE_NAME, "test-service");
    assertIdentifyingString(agentToServer, SERVICE_INSTANCE_ID, "test-instance");
    assertIdentifyingString(agentToServer, SERVICE_NAMESPACE, "test-ns");

    List<KeyValue> identifyingAttributes = agentToServer.agent_description.identifying_attributes;
    assertThat(identifyingAttributes).hasSize(3);

    List<KeyValue> nonIdentifyingAttributes =
        agentToServer.agent_description.non_identifying_attributes;
    assertThat(nonIdentifyingAttributes)
        .anyMatch(
            kv ->
                kv.key.equals(DEPLOYMENT_ENVIRONMENT_NAME.getKey())
                    && kv.value.string_value.equals("test-deployment-env"));
    assertThat(nonIdentifyingAttributes)
        .anyMatch(kv -> kv.key.equals("long") && kv.value.int_value.equals(12L));
    assertThat(nonIdentifyingAttributes)
        .anyMatch(kv -> kv.key.equals("double") && kv.value.double_value.equals(99.0));
    assertThat(nonIdentifyingAttributes)
        .anyMatch(kv -> kv.key.equals("bool") && kv.value.bool_value.equals(true));
    assertThat(nonIdentifyingAttributes)
        .anyMatch(kv -> kv.key.equals("val") && kv.value.string_value.equals("vvv"));
    AnyValue longsArray =
        createArrayAttribute(
            new AnyValue.Builder().int_value(2L).build(),
            new AnyValue.Builder().int_value(3L).build(),
            new AnyValue.Builder().int_value(5L).build());
    assertThat(nonIdentifyingAttributes)
        .anyMatch(kv -> kv.key.equals("longarr") && kv.value.equals(longsArray));
    assertThat(nonIdentifyingAttributes)
        .anyMatch(kv -> kv.key.equals("longobjarr") && kv.value.equals(longsArray));
    assertThat(nonIdentifyingAttributes)
        .anyMatch(
            matching(
                "doublearr",
                new AnyValue.Builder().double_value(2.0).build(),
                new AnyValue.Builder().double_value(3.0).build()));
    assertThat(nonIdentifyingAttributes)
        .anyMatch(
            matching(
                "doubleobjarr",
                new AnyValue.Builder().double_value(5.0).build(),
                new AnyValue.Builder().double_value(6.0).build()));

    assertThat(nonIdentifyingAttributes)
        .anyMatch(
            matching(
                "stringarr",
                new AnyValue.Builder().string_value("foo").build(),
                new AnyValue.Builder().string_value("flimflam").build()));
    assertThat(nonIdentifyingAttributes)
        .anyMatch(
            matching(
                "stringobjarr",
                new AnyValue.Builder().string_value("flim").build(),
                new AnyValue.Builder().string_value("jibberjo").build()));
    assertThat(nonIdentifyingAttributes)
        .anyMatch(
            matching(
                "boolarr",
                new AnyValue.Builder().bool_value(true).build(),
                new AnyValue.Builder().bool_value(false).build()));
    assertThat(nonIdentifyingAttributes)
        .anyMatch(
            matching(
                "boolobjarr",
                new AnyValue.Builder().bool_value(true).build(),
                new AnyValue.Builder().bool_value(true).build(),
                new AnyValue.Builder().bool_value(false).build(),
                new AnyValue.Builder().bool_value(true).build()));
    assertThat(nonIdentifyingAttributes)
        .anyMatch(
            kv -> kv.key.equals(OS_NAME.getKey()) && kv.value.string_value.equals("test-os-name"));
    assertThat(nonIdentifyingAttributes)
        .anyMatch(
            kv -> kv.key.equals(OS_TYPE.getKey()) && kv.value.string_value.equals("test-os-type"));
    assertThat(nonIdentifyingAttributes)
        .anyMatch(
            kv ->
                kv.key.equals(OS_VERSION.getKey()) && kv.value.string_value.equals("test-os-ver"));
  }

  private static Predicate<? super KeyValue> matching(String key, AnyValue... values) {
    return kv -> kv.key.equals(key) && kv.value.equals(createArrayAttribute(values));
  }

  private static AnyValue createArrayAttribute(AnyValue... values) {
    return new AnyValue.Builder()
        .array_value(new ArrayValue.Builder().values(Arrays.asList(values)).build())
        .build();
  }

  private void assertIdentifyingString(
      AgentToServer agentToServer, AttributeKey<String> attr, String expected) {
    assertThat(agentToServer.agent_description.identifying_attributes)
        .anyMatch(kv -> kv.key.equals(attr.getKey()) && kv.value.string_value.equals(expected));
  }
}
