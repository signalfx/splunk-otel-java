package com.splunk.opentelemetry.profiler.snapshot;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Closeable;
import java.io.IOException;
import io.opentelemetry.sdk.common.CompletableResultCode;
import org.junit.jupiter.api.Test;

class CloserTest {
  private final Closer closer = new Closer();

  @Test
  void closeClosesAddedCloseable() {
    var closeable = new SuccessfulCloseable();
    closer.add(closeable);
    closer.close();

    assertThat(closeable.closed).isTrue();
  }

  @Test
  void closeClosesMultipleAddedCloseable() {
    var one = new SuccessfulCloseable();
    var two = new SuccessfulCloseable();

    closer.add(one);
    closer.add(two);
    closer.close();

    assertThat(one.closed).isTrue();
    assertThat(two.closed).isTrue();
  }

  @Test
  void closeReportsSuccessAllCloseablesCloseSuccessfully() {
    closer.add(new SuccessfulCloseable());
    closer.add(new SuccessfulCloseable());

    var result = closer.close();
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  void closeReportsFailureWhenCloseableFailsToClose() {
    closer.add(new ExceptionThrowingCloseable());

    var result = closer.close();
    assertThat(result.isSuccess()).isFalse();
  }

  @Test
  void closeReportsFailureWhenAtLeaseCloseableFailsToClose() {
    closer.add(new SuccessfulCloseable());
    closer.add(new ExceptionThrowingCloseable());
    closer.add(new SuccessfulCloseable());

    var result = closer.close();
    assertThat(result.isSuccess()).isFalse();
  }

  private static class SuccessfulCloseable implements Closeable {
    private boolean closed;

    @Override
    public void close() {
      closed = true;
    }
  }

  private static class ExceptionThrowingCloseable implements Closeable {
    @Override
    public void close() throws IOException {
      throw new IOException();
    }
  }
}
