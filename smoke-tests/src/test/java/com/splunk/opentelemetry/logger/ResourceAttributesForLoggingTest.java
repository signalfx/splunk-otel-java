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

package com.splunk.opentelemetry.logger;

import com.splunk.opentelemetry.SmokeTest;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Main feature why we expose resource attributes as system properties is to be able to use them in
 * logs. This is a smoke test, because there are several moving parts involved. If everything
 * together doesn't work, there is no point in any simpler unit/integration test.
 */
public class ResourceAttributesForLoggingTest {

  @Test
  void resourceAttributesCanBeLogged() throws IOException, InterruptedException {
    Assumptions.assumeTrue(SmokeTest.agentPath != null, "Agent path is specified");

    Process process = startProcessWithJavaagent();

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    process.getInputStream().transferTo(out);
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    process.getErrorStream().transferTo(err);

    Assertions.assertTrue(process.waitFor(30L, TimeUnit.SECONDS), "Process exited");

    Assertions.assertEquals(
        0,
        process.exitValue(),
        "Unexpected exit code. Process exited with stderr: \n" + err.toString());

    String stdout = out.toString();
    Assertions.assertTrue(
        stdout.contains(
            "LoggerIntegrationTest - service=MyService, env=development: This is an important message"),
        "Process output `" + stdout + "` contains log line with expected resource attributes");
  }

  private Process startProcessWithJavaagent() throws IOException {
    File javaBinary = new File(System.getProperty("java.home"), "bin/java");
    String javaagent = "-javaagent:" + SmokeTest.agentPath;
    String resourceAttributes =
        "-Dotel.resource.attributes=service.name=MyService,environment=development";
    String classpath = System.getProperty("java.class.path");
    String[] strings = {
      javaBinary.getAbsolutePath(),
      javaagent,
      resourceAttributes,
      "-cp",
      classpath,
      LoggerTestMain.class.getName()
    };
    ProcessBuilder processBuilder = new ProcessBuilder();
    System.out.println(String.join(" ", strings));
    processBuilder.command(strings);
    return processBuilder.start();
  }
}
