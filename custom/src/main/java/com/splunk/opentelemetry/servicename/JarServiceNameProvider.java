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

import static java.util.logging.Level.FINE;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ConditionalResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.annotation.Nullable;

@AutoService(ResourceProvider.class)
public class JarServiceNameProvider implements ConditionalResourceProvider {

  private static final Logger logger = Logger.getLogger(WebXmlServiceNameProvider.class.getName());

  private final Supplier<String[]> getProcessHandleArguments;
  private final Function<String, String> getSystemProperty;
  private final Predicate<Path> fileExists;

  @SuppressWarnings("unused") // SPI
  public JarServiceNameProvider() {
    this(JarServiceNameProvider::getArgumentsFromProcessHandle, System::getProperty, Files::exists);
  }

  // visible for tests
  JarServiceNameProvider(
      Supplier<String[]> getProcessHandleArguments,
      Function<String, String> getSystemProperty,
      Predicate<Path> fileExists) {
    this.getProcessHandleArguments = getProcessHandleArguments;
    this.getSystemProperty = getSystemProperty;
    this.fileExists = fileExists;
  }

  @Override
  public Resource createResource(ConfigProperties config) {
    Path jarPath = getJarPathFromProcessHandle();
    if (jarPath == null) {
      jarPath = getJarPathFromSunCommandLine();
    }
    if (jarPath == null) {
      return Resource.empty();
    }
    String serviceName = getServiceName(jarPath);
    logger.log(FINE, "Auto-detected service name from the jar file name: {0}", serviceName);
    return Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName));
  }

  @Override
  public boolean shouldApply(ConfigProperties config, Resource existing) {
    return ServiceNameChecker.serviceNameNotConfigured(config, existing);
  }

  @Nullable
  private Path getJarPathFromProcessHandle() {
    String[] javaArgs = getProcessHandleArguments.get();
    for (int i = 0; i < javaArgs.length; ++i) {
      if ("-jar".equals(javaArgs[i]) && (i < javaArgs.length - 1)) {
        return Paths.get(javaArgs[i + 1]);
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private static String[] getArgumentsFromProcessHandle() {
    try {
      Class<?> clazz = Class.forName("java.lang.ProcessHandle");
      Method currentMethod = clazz.getDeclaredMethod("current");
      Method infoMethod = clazz.getDeclaredMethod("info");
      Object currentInstance = currentMethod.invoke(null);
      Object info = infoMethod.invoke(currentInstance);
      Class<?> infoClass = Class.forName("java.lang.ProcessHandle$Info");
      Method argumentsMethod = infoClass.getMethod("arguments");
      Optional<String[]> optionalArgs = (Optional<String[]>) argumentsMethod.invoke(info);
      return optionalArgs.orElse(new String[0]);
    } catch (ClassNotFoundException
        | InvocationTargetException
        | NoSuchMethodException
        | IllegalAccessException e) {
      return new String[0];
    }
  }

  @Nullable
  private Path getJarPathFromSunCommandLine() {
    // the jar file is the first argument in the command line string
    String programArguments = getSystemProperty.apply("sun.java.command");
    if (programArguments == null) {
      return null;
    }

    // Take the path until the first space. If the path doesn't exist extend it up to the next
    // space. Repeat until a path that exists is found or input runs out.
    int next = 0;
    while (true) {
      int nextSpace = programArguments.indexOf(' ', next);
      if (nextSpace == -1) {
        Path candidate = Paths.get(programArguments);
        return fileExists.test(candidate) ? candidate : null;
      }
      Path candidate = Paths.get(programArguments.substring(0, nextSpace));
      next = nextSpace + 1;
      if (fileExists.test(candidate)) {
        return candidate;
      }
    }
  }

  private static String getServiceName(Path jarPath) {
    String jarName = jarPath.getFileName().toString();
    int dotIndex = jarName.lastIndexOf(".");
    return dotIndex == -1 ? jarName : jarName.substring(0, dotIndex);
  }

  @Override
  public int order() {
    // make it run later than ServletServiceNameProvider (200)
    return 300;
  }
}
