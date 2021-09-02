package com.splunk.opentelemetry.profiler;

import io.opentelemetry.instrumentation.api.config.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigurationTest {

    String logsEndpoint = "http://logs.example.com";
    String otelEndpoint = "http://otel.example.com";

    @Test
    void getConfigUrl_endpointDefined() {
        Config config = mock(Config.class);
        when(config.getString(Configuration.CONFIG_KEY_INGEST_URL)).thenReturn(logsEndpoint);
        when(config.getString(Configuration.CONFIG_KEY_OTEL_OTLP_URL)).thenReturn(otelEndpoint);
        String result = Configuration.getConfigUrl(config);
        assertEquals(result, logsEndpoint);
    }

    @Test
    void getConfigUrl_endpointNotDefined() {
        Config config = mock(Config.class);
        when(config.getString(Configuration.CONFIG_KEY_INGEST_URL)).thenReturn(null);
        when(config.getString(Configuration.CONFIG_KEY_OTEL_OTLP_URL)).thenReturn(otelEndpoint);
        String result = Configuration.getConfigUrl(config);
        assertEquals(result, otelEndpoint);
    }

    @Test
    void getConfigUrlNull() {
        Config config = mock(Config.class);
        when(config.getString(Configuration.CONFIG_KEY_INGEST_URL)).thenReturn(null);
        when(config.getString(Configuration.CONFIG_KEY_OTEL_OTLP_URL)).thenReturn(null);
        String result = Configuration.getConfigUrl(config);
        assertNull(result);
    }
}