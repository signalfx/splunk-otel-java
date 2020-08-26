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

package com.signalfx.opentelemetry;

import static java.util.stream.Collectors.toList;

import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;
import java.util.Objects;

public class SplunkSpanExporter implements SpanExporter {
  private final SpanExporter delegate;

  public SplunkSpanExporter(SpanExporter delegate) {
    this.delegate = Objects.requireNonNull(delegate, "Delegate span exporter cannot be null");
  }

  @Override
  public ResultCode export(Collection<SpanData> spans) {
    return delegate.export(spans.stream().map(SpanDataWrapper::new).collect(toList()));
  }

  @Override
  public ResultCode flush() {
    return delegate.flush();
  }

  @Override
  public void shutdown() {
    delegate.shutdown();
  }
}
