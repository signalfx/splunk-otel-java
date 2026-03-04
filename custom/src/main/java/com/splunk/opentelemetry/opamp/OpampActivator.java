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

import static io.opentelemetry.opamp.client.internal.request.service.HttpRequestService.DEFAULT_DELAY_BETWEEN_REQUESTS;
import static io.opentelemetry.opamp.client.internal.request.service.HttpRequestService.DEFAULT_DELAY_BETWEEN_RETRIES;
import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getConfig;
import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getResource;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_INSTANCE_ID;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAMESPACE;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_VERSION;
import static io.opentelemetry.semconv.incubating.DeploymentIncubatingAttributes.DEPLOYMENT_ENVIRONMENT_NAME;
import static io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_NAME;
import static io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_TYPE;
import static io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_VERSION;
import static java.util.logging.Level.WARNING;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.opamp.client.OpampClient;
import io.opentelemetry.opamp.client.OpampClientBuilder;
import io.opentelemetry.opamp.client.internal.connectivity.http.OkHttpSender;
import io.opentelemetry.opamp.client.internal.request.delay.PeriodicDelay;
import io.opentelemetry.opamp.client.internal.request.service.HttpRequestService;
import io.opentelemetry.opamp.client.internal.response.MessageData;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Duration;
import java.util.logging.Logger;
import opamp.proto.ServerErrorResponse;
import org.jetbrains.annotations.Nullable;

@AutoService(AgentListener.class)
public class OpampActivator implements AgentListener {
  private static final Logger logger = Logger.getLogger(OpampActivator.class.getName());

  private static final String OP_AMP_ENABLED_PROPERTY = "splunk.opamp.enabled";
  private static final String OP_AMP_ENDPOINT = "splunk.opamp.endpoint";
  private static final String OP_AMP_POLLING_INTERVAL = "splunk.opamp.polling.interval";

  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    ConfigProperties config = getConfig(autoConfiguredOpenTelemetrySdk);
    if (!config.getBoolean(OP_AMP_ENABLED_PROPERTY, false)) {
      return;
    }

    Resource resource = getResource(autoConfiguredOpenTelemetrySdk);
    long pollingDuration =
        config.getLong(
            OP_AMP_POLLING_INTERVAL, DEFAULT_DELAY_BETWEEN_REQUESTS.getNextDelay().toMillis());

    String endpoint = config.getString(OP_AMP_ENDPOINT);
    startOpampClient(
        endpoint,
        resource,
        pollingDuration,
        new OpampClient.Callbacks() {
          @Override
          public void onConnect(OpampClient opampClient) {}

          @Override
          public void onConnectFailed(OpampClient opampClient, @Nullable Throwable throwable) {
            logger.log(WARNING, "Connection to OpAMP server failed", throwable);
          }

          @Override
          public void onErrorResponse(
              OpampClient opampClient, ServerErrorResponse serverErrorResponse) {
            logger.log(WARNING, "OpAMP server returned error " + serverErrorResponse);
          }

          @Override
          public void onMessage(OpampClient opampClient, MessageData messageData) {}
        });
  }

  static OpampClient startOpampClient(
      String endpoint,
      Resource resource,
      long pollingDurationMillis,
      OpampClient.Callbacks callbacks) {

    OpampClientBuilder builder = OpampClient.builder();
    // TODO: Uncomment once we are able to report our effective config
    // builder.enableEffectiveConfigReporting();
    if (endpoint != null) {
      PeriodicDelay pollingDelay =
          PeriodicDelay.ofFixedDuration(Duration.ofMillis(pollingDurationMillis));
      OkHttpSender okhttp = OkHttpSender.create(endpoint);
      HttpRequestService httpSender =
          HttpRequestService.create(okhttp, pollingDelay, DEFAULT_DELAY_BETWEEN_RETRIES);
      builder.setRequestService(httpSender);
    }
    addIdentifying(builder, resource, DEPLOYMENT_ENVIRONMENT_NAME);
    addIdentifying(builder, resource, SERVICE_NAME);
    addIdentifying(builder, resource, SERVICE_VERSION);
    addIdentifying(builder, resource, SERVICE_NAMESPACE);
    addIdentifying(builder, resource, SERVICE_INSTANCE_ID);

    addNonIdentifying(builder, resource, OS_NAME);
    addNonIdentifying(builder, resource, OS_TYPE);
    addNonIdentifying(builder, resource, OS_VERSION);

    return builder.build(callbacks);
  }

  static void addIdentifying(
      OpampClientBuilder builder, Resource res, AttributeKey<String> resourceAttr) {
    String attr = res.getAttribute(resourceAttr);
    if (attr != null) {
      builder.putIdentifyingAttribute(resourceAttr.getKey(), attr);
    }
  }

  static void addNonIdentifying(
      OpampClientBuilder builder, Resource res, AttributeKey<String> resourceAttr) {
    String attr = res.getAttribute(resourceAttr);
    if (attr != null) {
      builder.putNonIdentifyingAttribute(resourceAttr.getKey(), attr);
    }
  }
}
