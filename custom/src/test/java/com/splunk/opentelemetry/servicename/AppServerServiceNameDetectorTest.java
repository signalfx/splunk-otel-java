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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AppServerServiceNameDetectorTest {

  @Test
  void simpleServiceName() throws Exception {
    var detector = detector("simple-service-name");
    assertEquals("test-service-name", detector.detect());
  }

  @Test
  void multiLangServiceName() throws Exception {
    var detector = detector("multi-lang-service-name");
    assertEquals("test-service-name", detector.detect());
  }

  @Test
  void ignoreTomcatDefaultApps() throws Exception {
    var detector = detector("ignore-tomcat-default-apps");
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

    var detector = detector(outputDir);
    assertEquals("test-service-name", detector.detect());
  }

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

    var detector = detector(outputDir);
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

  private static TestServiceNameDetector detector(Path testPath) {
    return new TestServiceNameDetector(new TestResourceLocator(), testPath);
  }

  private static TestServiceNameDetector detector(String testName) {
    return detector(Paths.get("src/test/resources/servicename/" + testName));
  }

  private static class TestServiceNameDetector extends AppServerServiceNameDetector {
    private final Path testPath;

    TestServiceNameDetector(ResourceLocator locator, Path testPath) {
      super(new TestAppServer(testPath), locator);
      this.testPath = testPath;
    }

    private static class TestAppServer implements AppServer {
      private final Path testPath;

      public TestAppServer(Path testPath) {
        this.testPath = testPath;
      }

      @Override
      public Path getDeploymentDir() throws Exception {
        return testPath.resolve("webapps");
      }

      @Override
      public Class<?> getServerClass() {
        return null;
      }


      @Override
      public boolean isValidAppName(Path path) {
        if (Files.isDirectory(path)) {
          String name = path.getFileName().toString();
          return !"docs".equals(name)
              && !"examples".equals(name)
              && !"host-manager".equals(name)
              && !"manager".equals(name);
        }
        return true;
      }

      @Override
      public boolean isValidResult(Path path, String result) {
        String name = path.getFileName().toString();
        return !"ROOT".equals(name) || !"Welcome to Tomcat".equals(result);
      }
    }
  }

  private static class TestResourceLocator implements ResourceLocator {

    @Override
    public Class<?> findClass(String className) {
      // as we don't use the class in getClassLocation it doesn't matter what is returned from here
      return TestResourceLocator.class;
    }

    @Override
    public URL getClassLocation(Class<?> clazz) {
      throw new IllegalStateException("shouldn't be called");
    }
  }
}
