package com.splunk.opentelemetry.middleware;

import static io.opentelemetry.javaagent.tooling.ClassLoaderMatcher.hasClassesNamed;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isProtected;
import static net.bytebuddy.matcher.ElementMatchers.named;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.javaagent.shared.MiddlewareHolder;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.eclipse.jetty.server.Server;

@AutoService(InstrumentationModule.class)
public class JettyAttributesInstrumentationModule extends InstrumentationModule {

  public JettyAttributesInstrumentationModule() {
    super("jetty");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new Instrumentation());
  }

  public static class Instrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<ClassLoader> classLoaderMatcher() {
      return hasClassesNamed("org.eclipse.jetty.server.Server");
    }

    @Override
    public ElementMatcher<? super TypeDescription> typeMatcher() {
      return named("org.eclipse.jetty.server.Server");
    }

    @Override
    public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
      return Collections.singletonMap(
          isMethod().and(isProtected()).and(named("doStart")),
          JettyAttributesInstrumentationModule.class.getName() + "$MiddlewareInitializedAdvice");
    }
  }

  @SuppressWarnings("unused")
  public static class MiddlewareInitializedAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter() {
      MiddlewareHolder.trySetVersion(Server.getVersion());
      MiddlewareHolder.trySetName("jetty");
    }
  }
}