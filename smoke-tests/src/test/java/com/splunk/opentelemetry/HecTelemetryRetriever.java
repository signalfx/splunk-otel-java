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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.ClearType;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

class HecTelemetryRetriever {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final JsonFactory JSON_FACTORY = new JsonFactory(OBJECT_MAPPER);
  private static final String HEC_PATH = "/services/collector/event";

  private final MockServerClient client;

  HecTelemetryRetriever(int backendPort) {
    this.client = new MockServerClient("localhost", backendPort);
  }

  void initializeEndpoints() {
    client
        .when(HttpRequest.request(HEC_PATH))
        .respond(HttpResponse.response("").withStatusCode(200));
  }

  void clearTelemetry() {
    client.clear(HttpRequest.request(), ClearType.LOG);
  }

  List<JsonNode> waitForEntries() throws IOException, InterruptedException {
    int previousSize = 0;
    long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
    HttpRequest[] requests = new HttpRequest[0];

    while (System.currentTimeMillis() < deadline) {
      requests = client.retrieveRecordedRequests(HttpRequest.request(HEC_PATH));

      if (requests.length > 0 && requests.length == previousSize) {
        break;
      }
      previousSize = requests.length;
      System.out.printf("Current HEC entry count %d%n", previousSize);
      TimeUnit.MILLISECONDS.sleep(500);
    }

    return extractJsonEntries(requests);
  }

  private List<JsonNode> extractJsonEntries(HttpRequest[] requests) throws IOException {
    List<JsonNode> entryNodes = new ArrayList<>();

    for (HttpRequest request : requests) {
      // HEC format just concatenates multiple JSON bodies after one another without using a JSON
      // array, that's why we use readValuesAs. Also use getBodyAsRawBytes instead of
      // getBodyAsString - MockServerClient parses the content and reformats if content type is
      // JSON, keeping only the JSON body.
      try (JsonParser parser = JSON_FACTORY.createParser(request.getBodyAsRawBytes())) {
        Iterator<JsonNode> entries = parser.readValuesAs(JsonNode.class);

        while (entries.hasNext()) {
          entryNodes.add(entries.next());
        }
      }
    }

    return entryNodes;
  }
}
