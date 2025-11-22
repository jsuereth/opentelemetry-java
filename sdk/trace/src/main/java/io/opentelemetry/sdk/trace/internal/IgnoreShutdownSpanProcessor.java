/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.trace.internal;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

/**
 * A span processor which delegates to another and ignores shutdown calls.
 *
 * <p>This is used for processing sub-resources of the SDK.
 *
 * <p>This class is internal and experimental. Its APIs are unstable and can change at any time. Its
 * APIs (or a version of them) may be promoted to the public stable API in the future, but no
 * guarantees are made.
 */
public final class IgnoreShutdownSpanProcessor implements SpanProcessor {
  private final SpanProcessor delegate;

  /** Construct a new processor that ignores shutdown calls. */
  public IgnoreShutdownSpanProcessor(SpanProcessor delegate) {
    this.delegate = delegate;
  }

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    delegate.onStart(parentContext, span);
  }

  @Override
  public boolean isStartRequired() {
    return delegate.isStartRequired();
  }

  @Override
  public void onEnd(ReadableSpan span) {
    delegate.onEnd(span);
  }

  @Override
  public boolean isEndRequired() {
    return delegate.isEndRequired();
  }

  @Override
  public String toString() {
    return "IgnoreShutdownSpanProcessor(" + delegate.toString() + ")";
  }
}
