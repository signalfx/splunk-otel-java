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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TomcatServiceNameDetectorTest {

  @Test
  void simpleServiceName() throws Exception {
    var detector = new TomcatServiceNameDetector(new TestResourceLocator("simple-service-name"));
    assertEquals("test-service-name", detector.detect());
  }

  @Test
  void multiLangServiceName() throws Exception {
    var detector =
        new TomcatServiceNameDetector(new TestResourceLocator("multi-lang-service-name"));
    assertEquals("test-service-name", detector.detect());
  }

  @Test
  void ignoreTomcatDefaultApps() throws Exception {
    var detector =
        new TomcatServiceNameDetector(new TestResourceLocator("ignore-tomcat-default-apps"));
    assertNull(detector.detect());
  }

  @Test
  void simpleServiceNamePackagedWar(@TempDir Path outputDir) throws Exception {
    Path webappsPath = outputDir.resolve("webapps");
    Files.createDirectory(webappsPath);
    createJar(
        "testapp.war",
        webappsPath,
        jarOutputStream -> {
          try {
            jarOutputStream.putNextEntry(new JarEntry("WEB-INF/web.xml"));
            jarOutputStream.write(
                Files.readAllBytes(
                    Path.of(
                        "src/test/resources/servicename/simple-service-name/webapps/testapp/WEB-INF/web.xml")));
            jarOutputStream.closeEntry();
          } catch (IOException exception) {
            throw new IllegalStateException(exception);
          }
        });

    var detector = new TomcatServiceNameDetector(new TestResourceLocator(outputDir));
    assertEquals("test-service-name", detector.detect());
  }

  static String createJar(String name, Path directory, Consumer<JarOutputStream> action)
      throws Exception {
    Path jarPath = directory.resolve(name);
    createJar(jarPath, action);
    return jarPath.toAbsolutePath().toString();
  }

  private static void createJar(Path path, Consumer<JarOutputStream> action) throws Exception {
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(path), manifest)) {
      action.accept(jar);
    }
  }

  static class TestResourceLocator implements ResourceLocator {
    private final String basePath;

    TestResourceLocator(String testName) {
      basePath = "src/test/resources/servicename/" + testName;
    }

    TestResourceLocator(Path path) {
      basePath = path.toString();
    }

    @Override
    public Class<?> findClass(String className) {
      // as we don't use the class in getClassLocation it doesn't matter what is returned from here
      return TestResourceLocator.class;
    }

    @Override
    public URL getClassLocation(Class<?> clazz) {
      try {
        // TomcatServiceNameDetector calls getParent twice on the returned path so "a" and "b" will
        // get stripped from the path before webapps is appended
        return Path.of(basePath, "a", "b").toUri().toURL();
      } catch (MalformedURLException exception) {
        throw new IllegalStateException(exception);
      }
    }
  }
}
