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

package com.splunk.opentelemetry.weblogic.rest;

import io.opentelemetry.extension.auto.annotations.WithSpan;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TheController {

  @GetMapping("/greeting")
  @ResponseBody
  public String sayHello() {
    return withSpan("This is fine!");
  }

  @GetMapping(value = "/greetingRemote", produces = "text/plain")
  @ResponseBody
  public String sayRemoteHello(@RequestParam("url") String urlInput) throws IOException {
    URL url = new URL(urlInput);
    URLConnection urlConnection = url.openConnection();
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try (InputStream remoteInputStream = urlConnection.getInputStream()) {
      long bytesRead = transfer(buffer, remoteInputStream);
      String responseBody = buffer.toString("UTF-8");
      return withSpan(
          bytesRead + " bytes read by " + urlConnection.getClass().getName() + "\n" + responseBody);
    }
  }

  @GetMapping(value = "/headers", produces = "text/plain")
  @ResponseBody
  String showRequestHeaders(HttpServletRequest req) {
    StringWriter response = new StringWriter();
    Enumeration<String> headerNames = req.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String headerName = headerNames.nextElement();
      response.write(headerName + ": ");

      List<String> headers = Collections.list(req.getHeaders(headerName));
      if (headers.size() == 1) {
        response.write(headers.get(0));
      } else {
        response.write("[");
        for (String header : headers) {
          response.write("  " + header + ",\n");
        }
        response.write("]");
      }
      response.write("\n");
    }
    return response.toString();
  }

  @WithSpan
  public String withSpan(String response) {
    return response;
  }

  // We have to run on Java 8, so no Java 9 stream transfer goodies for us.
  private long transfer(OutputStream out, InputStream in) throws IOException {
    Objects.requireNonNull(out, "out");
    long transferred = 0;
    byte[] buffer = new byte[65535];
    int read;
    while ((read = in.read(buffer, 0, buffer.length)) >= 0) {
      out.write(buffer, 0, read);
      transferred += read;
    }
    return transferred;
  }
}
