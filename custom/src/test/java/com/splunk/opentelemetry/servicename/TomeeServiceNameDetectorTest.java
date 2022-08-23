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

package com.splunk.opentelemetry.servicename;

import static com.splunk.opentelemetry.servicename.TomcatServiceNameDetectorTest.createJar;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.splunk.opentelemetry.servicename.TomcatServiceNameDetectorTest.TestResourceLocator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TomeeServiceNameDetectorTest {

  @Test
  void simpleServiceNameEar() throws Exception {
    var detector = detector("simple-service-name-ear");
    assertEquals("test-service-name", detector.detect());
  }

  @Test
  void simpleServiceNamePackagedEar(@TempDir Path outputDir) throws Exception {
    Path webappsPath = outputDir.resolve("webapps");
    Files.createDirectory(webappsPath);
    createJar(
        "testapp.ear",
        webappsPath,
        jarOutputStream -> {
          try {
            jarOutputStream.putNextEntry(new JarEntry("META-INF/application.xml"));
            jarOutputStream.write(
                Files.readAllBytes(
                    Path.of(
                        "src/test/resources/servicename/simple-service-name-ear/webapps/testapp/META-INF/application.xml")));
            jarOutputStream.closeEntry();
          } catch (IOException exception) {
            throw new IllegalStateException(exception);
          }
        });

    var detector = new TomeeServiceNameDetector(new TestResourceLocator(outputDir), "webapps");
    assertEquals("test-service-name", detector.detect());
  }

  private static TomeeServiceNameDetector detector(String testName) {
    return new TomeeServiceNameDetector(new TestResourceLocator(testName), "webapps");
  }
}
