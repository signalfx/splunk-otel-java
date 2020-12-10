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

package com.splunk.opentelemetry.appservers.javaee;

import io.opentelemetry.extension.annotations.WithSpan;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GreetingServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String path = (req.getContextPath() + "/headers").replace("//", "/");
    URL url = new URL("http", "localhost", req.getLocalPort(), path);
    URLConnection urlConnection = url.openConnection();
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try (InputStream remoteInputStream = urlConnection.getInputStream()) {
      long bytesRead = transfer(remoteInputStream, buffer);
      String responseBody = buffer.toString("UTF-8");
      ServletOutputStream outputStream = resp.getOutputStream();
      outputStream.print(
          withSpan(
              bytesRead
                  + " bytes read by "
                  + urlConnection.getClass().getName()
                  + "\n"
                  + responseBody));
      outputStream.flush();
    }
  }

  @WithSpan
  public String withSpan(String responseBody) {
    return responseBody;
  }

  // We have to run on Java 8, so no Java 9 stream transfer goodies for us.
  private long transfer(InputStream from, OutputStream to) throws IOException {
    Objects.requireNonNull(to, "out");
    long transferred = 0;
    byte[] buffer = new byte[65535];
    int read;
    while ((read = from.read(buffer, 0, buffer.length)) >= 0) {
      to.write(buffer, 0, read);
      transferred += read;
    }
    return transferred;
  }
}
