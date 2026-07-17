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

import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.OTEL_INSTRUMENTATION_NAME;
import static io.opentelemetry.opamp.client.internal.request.service.HttpRequestService.DEFAULT_DELAY_BETWEEN_RETRIES;
import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getResource;
import static java.util.logging.Level.WARNING;

import com.google.auto.service.AutoService;
import com.splunk.opamp.remotecontrol.BigDumper;
import com.splunk.opamp.remotecontrol.CommandDispatcher;
import com.splunk.opamp.remotecontrol.CommandDispatcherImpl;
import com.splunk.opamp.remotecontrol.NoOpCommandDispatcher;
import com.splunk.opamp.remotecontrol.PprofThreadDumpExporter;
import com.splunk.opentelemetry.opamp.effectiveconfig.EffectiveConfigReporter;
import com.splunk.opentelemetry.opamp.effectiveconfig.UpdatableEffectiveConfigState;
import com.splunk.opentelemetry.profiler.InstrumentationSource;
import com.splunk.opentelemetry.profiler.ProfilingDataType;
import com.splunk.opentelemetry.profiler.ProfilingSupervisor;
import com.splunk.opentelemetry.profiler.exporter.PprofLogDataExporter;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.opamp.client.OpampClient;
import io.opentelemetry.opamp.client.OpampClientBuilder;
import io.opentelemetry.opamp.client.internal.connectivity.http.OkHttpSender;
import io.opentelemetry.opamp.client.internal.request.delay.PeriodicDelay;
import io.opentelemetry.opamp.client.internal.request.service.HttpRequestService;
import io.opentelemetry.opamp.client.internal.response.MessageData;
import io.opentelemetry.opamp.client.internal.state.State;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;
import opamp.proto.ComponentHealth;
import opamp.proto.ServerErrorResponse;
import org.jetbrains.annotations.Nullable;

@AutoService(AgentListener.class)
public class OpampActivator implements AgentListener {
  private static final Logger logger = Logger.getLogger(OpampActivator.class.getName());

  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    OpampClientConfiguration opampClientConfiguration =
        OpampClientConfigurationFactory.createConfiguration(autoConfiguredOpenTelemetrySdk);
    if (!opampClientConfiguration.isEnabled()) {
      return;
    }

    Resource resource = getResource(autoConfiguredOpenTelemetrySdk);
    UpdatableEffectiveConfigState effectiveConfigState = new UpdatableEffectiveConfigState();
    EffectiveConfigReporter effectiveConfigReporter =
        EffectiveConfigReporter.create(autoConfiguredOpenTelemetrySdk, effectiveConfigState);
    effectiveConfigReporter.reportEffectiveConfigIfChanged();

    CommandDispatcher commandDispatcher = new NoOpCommandDispatcher();
    if (opampClientConfiguration.remoteControlIsAllowed()) {
      io.opentelemetry.api.logs.Logger loggerOfCommands =
          autoConfiguredOpenTelemetrySdk
              .getOpenTelemetrySdk()
              .getSdkLoggerProvider()
              .get(OTEL_INSTRUMENTATION_NAME); // it's a sad sad thing to lie about this...
      PprofLogDataExporter logDataExporter =
          new PprofLogDataExporter(
              loggerOfCommands, ProfilingDataType.CPU, InstrumentationSource.THREADDUMP);
      PprofThreadDumpExporter threadDumpExporter = new PprofThreadDumpExporter(logDataExporter);
      commandDispatcher = new CommandDispatcherImpl(new BigDumper(threadDumpExporter::export));
    }
    ServerToAgentMessageHandler serverToAgentMessageHandler =
        new ServerToAgentMessageHandler(
            ProfilingSupervisor.SUPPLIER.get(), effectiveConfigReporter, commandDispatcher);

    OpampClient client =
        startOpampClient(
            opampClientConfiguration,
            resource,
            effectiveConfigState,
            new OpampClient.Callbacks() {
              @Override
              public void onConnect(OpampClient opampClient) {
                logger.fine("Connected to OpAMP server");
              }

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
              public void onMessage(OpampClient opampClient, MessageData messageData) {
                logger.fine(() -> "Received message: " + messageData);
                serverToAgentMessageHandler.handleMessage(messageData, opampClient);
              }
            });

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    client.close();
                  } catch (IOException e) {
                    logger.log(WARNING, "Error shutting down OpAMP client", e);
                  }
                }));
  }

  @Override
  public int order() {
    return Integer.MAX_VALUE;
  }

  static OpampClient startOpampClient(
      OpampClientConfiguration opampClientConfiguration,
      Resource resource,
      State.EffectiveConfig effectiveConfig,
      OpampClient.Callbacks callbacks) {

    OpampClientBuilder builder = OpampClient.builder();
    builder.enableEffectiveConfigReporting();
    builder.enableRemoteConfig();

    String endpoint = opampClientConfiguration.getEndpoint();
    long pollingDurationMillis = opampClientConfiguration.getPollingInterval();
    if (endpoint != null) {
      PeriodicDelay pollingDelay =
          PeriodicDelay.ofFixedDuration(Duration.ofMillis(pollingDurationMillis));
      OkHttpSender okhttp = OkHttpSender.create(endpoint);
      HttpRequestService httpSender =
          HttpRequestService.create(okhttp, pollingDelay, DEFAULT_DELAY_BETWEEN_RETRIES);
      builder.setRequestService(httpSender);
    }

    OpampAgentAttributes agentAttributes = new OpampAgentAttributes(resource);
    agentAttributes.addIdentifyingAttributes(builder);
    agentAttributes.addNonIdentifyingAttributes(builder);

    builder.setEffectiveConfigState(effectiveConfig);
    builder.enableHealthReporting(createInitialHealthReport());

    return builder.build(callbacks);
  }

  private static ComponentHealth createInitialHealthReport() {
    Instant now = Instant.now();
    long nowNanos = now.getEpochSecond() * 1_000_000_000L + now.getNano();
    return new ComponentHealth.Builder()
        .healthy(true)
        .status("OK")
        .start_time_unix_nano(nowNanos)
        .status_time_unix_nano(nowNanos)
        .build();
  }
}
