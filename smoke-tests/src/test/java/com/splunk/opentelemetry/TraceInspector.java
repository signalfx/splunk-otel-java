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

package com.splunk.opentelemetry;

import com.google.protobuf.ByteString;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.Span;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TraceInspector {
  final Collection<ExportTraceServiceRequest> traces;

  public TraceInspector(Collection<ExportTraceServiceRequest> traces) {
    this.traces = traces;
  }

  public Stream<Span> getSpanStream() {
    return traces.stream()
        .flatMap(it -> it.getResourceSpansList().stream())
        .flatMap(it -> it.getInstrumentationLibrarySpansList().stream())
        .flatMap(it -> it.getSpansList().stream());
  }

  public long countFilteredAttributes(String attributeName, Object attributeValue) {
    return getSpanStream()
        .flatMap(s -> s.getAttributesList().stream())
        .filter(a -> a.getKey().equals(attributeName))
        .map(a -> a.getValue().getStringValue())
        .filter(s -> s.equals(attributeValue))
        .count();
  }

  protected int countSpansByName(String spanName) {
    return (int) getSpanStream().filter(it -> it.getName().equals(spanName)).count();
  }

  protected int countSpansByKind(Span.SpanKind spanKind) {
    return (int) getSpanStream().filter(it -> it.getKind().equals(spanKind)).count();
  }

  public int size() {
    return traces.size();
  }

  public Set<String> getTraceIds() {
    return getSpanStream()
        .map(Span::getTraceId)
        .map(ByteString::toByteArray)
        .map(TraceId::bytesToHex)
        .collect(Collectors.toSet());
  }

  /**
   * This method returns the value for the requested attribute of the *first* server span. Be
   * careful when using on a distributed trace with several server spans.
   */
  public String getServerSpanAttribute(String attributeKey) {
    return getSpanStream()
        .filter(span -> span.getKind() == Span.SpanKind.SPAN_KIND_SERVER)
        .map(Span::getAttributesList)
        .flatMap(Collection::stream)
        .filter(attr -> attributeKey.equals(attr.getKey()))
        .map(keyValue -> keyValue.getValue().getStringValue())
        .findFirst()
        .orElseThrow(
            () ->
                new NoSuchElementException(
                    "Attribute " + attributeKey + " is not found on server span"));
  }
}
