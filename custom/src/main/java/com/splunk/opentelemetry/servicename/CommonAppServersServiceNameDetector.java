package com.splunk.opentelemetry.servicename;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * This class provides a ServiceNameDetector that knows how to find and parse the
 * most common application server configuration files.
 */
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
    return Arrays.asList(
      detectorFor(new TomeeAppServer(locator)),
      detectorFor(new TomcatAppServer(locator)),
      detectorFor(new JettyAppServer(locator)),
      detectorFor(new LibertyAppService(locator)),
      detectorFor(new WildflyAppServer(locator)),
      detectorFor(new GlassfishAppServer(locator)),
      new WebSphereServiceNameDetector(new WebSphereAppServer(locator))
    );
  }

  private static AppServerServiceNameDetector detectorFor(AppServer appServer){
    return new AppServerServiceNameDetector(appServer);
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
