package com.splunk.opentelemetry.profiler;

import io.opentelemetry.instrumentation.api.config.Config;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_ENABLE_PROFILER;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_INGEST_URL;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_KEEP_FILES;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_OTEL_OTLP_URL;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_PERIOD_PREFIX;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_PROFILER_DIRECTORY;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_RECORDING_DURATION;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_TLAB_ENABLED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ConfigurationLoggerTest {

    @Test
    void testLog() {
        Logger log = mock(Logger.class);
        Config config = mock(Config.class);

        when(config.getBoolean(CONFIG_KEY_ENABLE_PROFILER)).thenReturn(true);
        when(config.getString(CONFIG_KEY_PROFILER_DIRECTORY)).thenReturn("somedir");
        when(config.getString(CONFIG_KEY_RECORDING_DURATION)).thenReturn("33m");
        when(config.getBoolean(CONFIG_KEY_KEEP_FILES)).thenReturn(true);
        when(config.getString(CONFIG_KEY_INGEST_URL)).thenReturn("http://example.com");
        when(config.getString(CONFIG_KEY_OTEL_OTLP_URL)).thenReturn("http://otel.example.com");
        when(config.getBoolean(CONFIG_KEY_TLAB_ENABLED)).thenReturn(false);
        when(config.getString(CONFIG_KEY_PERIOD_PREFIX + ".jdk.threaddump")).thenReturn("500ms");

        ConfigurationLogger configurationLogger = new ConfigurationLogger(log);

        configurationLogger.log(config);

        InOrder inOrder = inOrder(log);
        inOrder.verify(log).info("-----------------------");
        inOrder.verify(log).info("Profiler configuration:");
        inOrder.verify(log).info(" {} : {}", "               splunk.profiler.enabled", true);
        inOrder.verify(log).info(" {} : {}", "             splunk.profiler.directory", "somedir");
        inOrder.verify(log).info(" {} : {}", "    splunk.profiler.recording.duration", "33m");
        inOrder.verify(log).info(" {} : {}", "            splunk.profiler.keep-files", true);
        inOrder.verify(log).info(" {} : {}", "         splunk.profiler.logs-endpoint", "http://example.com");
        inOrder.verify(log).info(" {} : {}", "           otel.exporter.otlp.endpoint", "http://otel.example.com");
        inOrder.verify(log).info(" {} : {}", "          splunk.profiler.tlab.enabled", false);
        inOrder.verify(log).info(" {} : {}", " splunk.profiler.period.jdk.threaddump", "500ms");
        inOrder.verify(log).info("-----------------------");
        verifyNoMoreInteractions(log);
    }

}