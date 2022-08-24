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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

abstract class AppServerServiceNameDetector extends ServiceNameDetector {
  private static final Logger log = LoggerFactory.getLogger(AppServerServiceNameDetector.class);

  /** Use to ignore default applications that are bundled with the app server. */
  boolean isValidAppName(String name) {
    return true;
  }

  /** Use to ignore default applications that are bundled with the app server. */
  boolean isValidResult(String name, String result) {
    return true;
  }

  boolean supportsEar() {
    return false;
  }

  /** Path to directory to be scanned for deployments. */
  abstract Path getDeploymentDir() throws Exception;

  @Override
  String detect() throws Exception {
    Path deploymentDir = getDeploymentDir();
    if (deploymentDir == null) {
      return null;
    }

    if (Files.exists(deploymentDir)) {
      log.debug("Looking for deployments in '{}'.", deploymentDir);
      for (Path path : Files.list(deploymentDir).collect(Collectors.toList())) {
        String name = detectName(path);
        if (name != null) {
          return name;
        }
      }
    } else {
      log.debug("Deployment dir '{}' doesn't exist.", deploymentDir);
    }

    return null;
  }

  private String detectName(Path path) {
    String name = path.getFileName().toString();
    if (!isValidAppName(name)) {
      log.debug("Skipping '{}'.", path);
      return null;
    }

    log.debug("Attempting service name detection in '{}'.", path);
    boolean supportsEar = supportsEar();
    if (Files.isDirectory(path)) {
      return handleExplodedApp(path, name, supportsEar);
    } else if (name.endsWith(".war")) {
      return handlePackagedWar(path, name);
    } else if (supportsEar && name.endsWith(".ear")) {
      return handlePackagedEar(path, name);
    }

    return null;
  }

  private String handleExplodedApp(Path path, String name, boolean handleEar) {
    {
      Path webXmlPath = path.resolve("WEB-INF/web.xml");
      String result = handleExplodedWar(webXmlPath, name);
      if (result != null) {
        return result;
      }
    }
    if (handleEar) {
      Path applicationXmlPath = path.resolve("META-INF/application.xml");
      String result = handleExplodedEar(applicationXmlPath, name);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  private String handlePackagedWar(Path path, String name) {
    return handlePackaged(path, name, "WEB-INF/web.xml", new WebXmlHandler());
  }

  private String handlePackagedEar(Path path, String name) {
    return handlePackaged(path, name, "META-INF/application.xml", new ApplicationXmlHandler());
  }

  private String handlePackaged(
      Path path, String name, String descriptorPath, DescriptorHandler handler) {
    try {
      ZipFile zip = new ZipFile(path.toFile());
      ZipEntry zipEntry = zip.getEntry(descriptorPath);
      if (zipEntry != null) {
        return handle(() -> zip.getInputStream(zipEntry), name, handler);
      }
    } catch (IOException exception) {
      log.warn("Failed to read '{}' from zip '{}'.", descriptorPath, path, exception);
    }

    return null;
  }

  private String handleExplodedWar(Path webXmlPath, String name) {
    return handleExploded(webXmlPath, name, new WebXmlHandler());
  }

  private String handleExplodedEar(Path applicationXmlPath, String name) {
    return handleExploded(applicationXmlPath, name, new ApplicationXmlHandler());
  }

  private String handleExploded(Path descriptor, String name, DescriptorHandler handler) {
    if (Files.exists(descriptor)) {
      return handle(() -> Files.newInputStream(descriptor), name, handler);
    }

    return null;
  }

  private String handle(InputStreamSupplier supplier, String name, DescriptorHandler handler) {
    try {
      try (InputStream inputStream = supplier.supply()) {
        String candidate = parseDescriptor(inputStream, handler);
        if (isValidResult(name, candidate)) {
          return candidate;
        }
      }
    } catch (Exception exception) {
      log.warn("Failed to parse descriptor", exception);
    }

    return null;
  }

  private static String parseDescriptor(InputStream inputStream, DescriptorHandler handler)
      throws ParserConfigurationException, SAXException, IOException {
    if (SaxParserFactoryHolder.saxParserFactory == null) {
      return null;
    }
    SAXParser saxParser = SaxParserFactoryHolder.saxParserFactory.newSAXParser();
    saxParser.parse(inputStream, handler);
    return handler.displayName;
  }

  private interface InputStreamSupplier {
    InputStream supply() throws IOException;
  }

  private static class WebXmlHandler extends DescriptorHandler {

    WebXmlHandler() {
      super("web-app");
    }
  }

  private static class ApplicationXmlHandler extends DescriptorHandler {

    ApplicationXmlHandler() {
      super("application");
    }
  }

  private static class DescriptorHandler extends DefaultHandler {
    private final String rootElementName;
    private final Deque<String> currentElement = new ArrayDeque<>();
    private boolean setDisplayName;
    String displayName;

    DescriptorHandler(String rootElementName) {
      this.rootElementName = rootElementName;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
      if (displayName == null
          && rootElementName.equals(currentElement.peek())
          && "display-name".equals(qName)) {
        String lang = attributes.getValue("xml:lang");
        if (lang == null || "".equals(lang)) {
          lang = "en"; // en is the default language
        }
        if ("en".equals(lang)) {
          setDisplayName = true;
        }
      }
      currentElement.push(qName);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
      currentElement.pop();
      setDisplayName = false;
    }

    @Override
    public void characters(char[] ch, int start, int length) {
      if (setDisplayName) {
        displayName = new String(ch, start, length);
      }
    }
  }

  private static class SaxParserFactoryHolder {
    private static final SAXParserFactory saxParserFactory = getSaxParserFactory();

    private static SAXParserFactory getSaxParserFactory() {
      try {
        return SAXParserFactory.newInstance();
      } catch (Throwable throwable) {
        log.warn("XML parser not available.", throwable);
      }
      return null;
    }
  }
}
