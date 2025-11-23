/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.trace;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.entity.internal.EntityUtil;
import io.opentelemetry.sdk.entity.internal.SdkEntity;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.internal.SdkTracerProviderUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExtendedTracerProviderTest {

  private CapturingSpanProcessor spanProcessor;
  private SdkTracerProvider tracerProvider;

  @BeforeEach
  void setUp() {
    spanProcessor = new CapturingSpanProcessor();
    tracerProvider = SdkTracerProvider.builder().addSpanProcessor(spanProcessor).build();
  }

  @Test
  void shuttingdownNestedDoesNotKillSpanProcessor() {
    SdkEntity entity =
        SdkEntity.builder("test")
            .setIdentity(Attributes.builder().put("test.id", 1).build())
            .build();
    SdkTracerProvider nested = SdkTracerProviderUtil.withEntity(tracerProvider, entity);
    nested.shutdown();
    assertThat(spanProcessor.isShutdown()).isFalse();
    tracerProvider.shutdown();
    assertThat(spanProcessor.isShutdown()).isTrue();
  }

  @Test
  void produceTraceOnAlternativeResource() {
    SdkEntity entity =
        SdkEntity.builder("test")
            .setIdentity(Attributes.builder().put("test.id", 1).build())
            .build();
    SdkTracerProvider nested = SdkTracerProviderUtil.withEntity(tracerProvider, entity);

    // Export a nseted span.
    nested.get("nested").spanBuilder("test-nested").startSpan().end();
    // Export a non-nested span.
    tracerProvider.get("normal").spanBuilder("test-normal").startSpan().end();

    // Verify exports
    List<SpanData> spans = spanProcessor.getExported();
    // Assert that we've exported the spans we expect.
    assertThat(spans)
        .satisfiesExactly(
            // Nested has the entity.
            s -> assertThat(EntityUtil.getEntities(s.getResource())).contains(entity),
            // Non-Nested does not.
            s -> assertThat(EntityUtil.getEntities(s.getResource())).doesNotContain(entity));
  }

  // Helper to capture all recorded spans in a list.
  static class CapturingSpanProcessor implements SpanProcessor {
    private final ArrayList<SpanData> exported = new ArrayList<>();
    private final AtomicBoolean stopped = new AtomicBoolean();

    @Override
    public boolean isEndRequired() {
      return true;
    }

    @Override
    public boolean isStartRequired() {
      return false;
    }

    @Override
    public void onEnd(ReadableSpan span) {
      exported.add(span.toSpanData());
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {}

    @Override
    public CompletableResultCode shutdown() {
      stopped.lazySet(true);
      return forceFlush();
    }

    public List<SpanData> getExported() {
      return exported;
    }

    public boolean isShutdown() {
      return stopped.get();
    }
  }
}
