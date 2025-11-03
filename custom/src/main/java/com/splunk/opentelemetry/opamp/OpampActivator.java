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

import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getConfig;
import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getResource;
import static java.util.logging.Level.WARNING;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.opamp.client.internal.OpampClient;
import io.opentelemetry.opamp.client.internal.OpampClientBuilder;
import io.opentelemetry.opamp.client.internal.connectivity.http.OkHttpSender;
import io.opentelemetry.opamp.client.internal.request.service.HttpRequestService;
import io.opentelemetry.opamp.client.internal.response.MessageData;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ServiceAttributes;
import java.util.logging.Logger;
import opamp.proto.ServerErrorResponse;
import org.jetbrains.annotations.Nullable;

@AutoService(AgentListener.class)
public class OpampActivator implements AgentListener {
  private static final Logger logger = Logger.getLogger(OpampActivator.class.getName());

  private static final String OP_AMP_ENABLED_PROPERTY = "splunk.opamp.enabled";
  private static final String OP_AMP_ENDPOINT = "splunk.opamp.endpoint";

  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    ConfigProperties config = getConfig(autoConfiguredOpenTelemetrySdk);
    if (!config.getBoolean(OP_AMP_ENABLED_PROPERTY, false)) {
      return;
    }

    Resource resource = getResource(autoConfiguredOpenTelemetrySdk);
    String serviceName = resource.getAttribute(ServiceAttributes.SERVICE_NAME);

    String endpoint = config.getString(OP_AMP_ENDPOINT);
    startOpampClient(
        endpoint,
        serviceName,
        new OpampClient.Callbacks() {
          @Override
          public void onConnect() {}

          @Override
          public void onConnectFailed(@Nullable Throwable throwable) {
            logger.log(WARNING, "Connection to OpAMP server failed", throwable);
          }

          @Override
          public void onErrorResponse(ServerErrorResponse errorResponse) {
            logger.log(WARNING, "OpAMP server returned error " + errorResponse);
          }

          @Override
          public void onMessage(MessageData messageData) {}
        });
  }

  static OpampClient startOpampClient(
      String endpoint, String serviceName, OpampClient.Callbacks callbacks) {
    OpampClientBuilder builder = OpampClient.builder();
    builder.enableRemoteConfig();
    if (endpoint != null) {
      builder.setRequestService(HttpRequestService.create(OkHttpSender.create(endpoint)));
    }
    if (serviceName != null) {
      builder.putIdentifyingAttribute("service.name", serviceName);
    }

    return builder.build(callbacks);
  }
}
