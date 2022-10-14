package com.splunk.opentelemetry.servicename;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

final class CommonAppServersServiceNameDetector implements ServiceNameDetector {

  private final DelegatingServiceNameDetector delegate;

  static ServiceNameDetector create(){
    DelegatingServiceNameDetector delegate = new DelegatingServiceNameDetector(detectors());
    return new CommonAppServersServiceNameDetector(delegate);
  }

  private CommonAppServersServiceNameDetector(DelegatingServiceNameDetector delegate) {
    this.delegate = delegate;
  }

  @Override
  public String detect() throws Exception {
    return delegate.detect();
  }

  private static List<ServiceNameDetector> detectors() {
    ResourceLocator locator = new ResourceLocatorImpl();

    List<ServiceNameDetector> detectors = new ArrayList<>();
    detectors.add(new TomeeServiceNameDetector(locator, new TomeeAppServer(locator)));
    detectors.add(new TomcatServiceNameDetector(locator, new TomcatAppServer(locator)));
    detectors.add(new JettyServiceNameDetector(locator, new JettyAppServer(locator)));
    detectors.add(new LibertyServiceNameDetector(locator, new LibertyAppService(locator)));
    detectors.add(new WildflyServiceNameDetector(locator, new WildflyAppServer(locator)));
    detectors.add(new GlassfishServiceNameDetector(locator, new GlassfishAppServer(locator)));
    detectors.add(new WebSphereServiceNameDetector(locator, new WebSphereAppServer(locator)));

    return detectors;
  }

  private static class ResourceLocatorImpl implements ResourceLocator {

    @Override
    public Class<?> findClass(String className) {
      try {
        return Class.forName(className, false, ClassLoader.getSystemClassLoader());
      } catch (ClassNotFoundException | LinkageError exception) {
        return null;
      }
    }

    @Override
    public URL getClassLocation(Class<?> clazz) {
      return clazz.getProtectionDomain().getCodeSource().getLocation();
    }
  }
}
