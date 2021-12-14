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

package com.splunk.opentelemetry.logs;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.collector.logs.v1.LogsServiceGrpc;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.sdk.logs.data.LogData;
import java.util.List;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OtlpLogsExporter implements LogsExporter {
  private static final Logger logger = LoggerFactory.getLogger(OtlpLogsExporter.class);

  private final ResourceLogsAdapter adapter;
  private final LogsServiceGrpc.LogsServiceFutureStub client;

  public OtlpLogsExporter(
      LogsServiceGrpc.LogsServiceFutureStub client, ResourceLogsAdapter adapter) {
    this.client = client;
    this.adapter = adapter;
  }

  @Override
  public void export(List<LogData> logs) {
    ResourceLogs resourceLogs = adapter.apply(logs);
    ExportLogsServiceRequest request =
        ExportLogsServiceRequest.newBuilder().addResourceLogs(resourceLogs).build();

    ResponseHandler responseHandler = new ResponseHandler(logs);
    Futures.addCallback(client.export(request), responseHandler, directExecutor());
  }

  public String getEndpoint() {
    return client.getChannel().authority();
  }

  public ResourceLogsAdapter getAdapter() {
    return adapter;
  }

  public static OtlpLogsExporterBuilder builder() {
    return new OtlpLogsExporterBuilder();
  }

  @VisibleForTesting
  static class ResponseHandler implements FutureCallback<ExportLogsServiceResponse> {

    private final List<LogData> logs;

    public ResponseHandler(List<LogData> logs) {
      this.logs = logs;
    }

    @Override
    public void onSuccess(@Nullable ExportLogsServiceResponse result) {
      logger.debug("Exported {} logs successfully.", logs.size());
    }

    @Override
    public void onFailure(Throwable th) {
      logger.warn("Splunk profiling agent failed to export logs", th);
    }
  }
}
