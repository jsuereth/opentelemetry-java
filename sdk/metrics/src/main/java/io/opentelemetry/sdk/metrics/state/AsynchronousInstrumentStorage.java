/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.state;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.api.metrics.ObservableMeasurement;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.metrics.aggregator.Aggregator;
import io.opentelemetry.sdk.metrics.instrument.DoubleMeasurement;
import io.opentelemetry.sdk.metrics.instrument.LongMeasurement;
import io.opentelemetry.sdk.metrics.view.AttributesProcessor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** This class is responsible for pulling asynchronous instrument readings on demand. */
public final class AsynchronousInstrumentStorage<T> implements AccumulationProvider<T> {
  private final ReentrantLock collectLock = new ReentrantLock();
  private final Consumer<? extends ObservableMeasurement> callback;
  private final Aggregator<T> aggregator;
  private final AttributesProcessor attributesProcessor;

  AsynchronousInstrumentStorage(
      Consumer<? extends ObservableMeasurement> callback,
      Aggregator<T> aggregator,
      AttributesProcessor attributesProcessor) {
    this.callback = callback;
    this.aggregator = aggregator;
    this.attributesProcessor = attributesProcessor;
  }

  @Override
  @SuppressWarnings(
      "unchecked") // We implement both types of observbale measurement, so this is wonky but safe.
  public Map<Attributes, T> accumulateThenReset() {
    collectLock.lock();
    try {
      Map<Attributes, T> result = new HashMap<>();
      ((Consumer<ObservableMeasurement>) callback).accept(new MyObservableMeasurement(result::put));
      return result;
    } finally {
      collectLock.unlock();
    }
  }

  /** Converts from observable callbacks to measurements. */
  class MyObservableMeasurement implements ObservableLongMeasurement, ObservableDoubleMeasurement {
    private final BiConsumer<Attributes, T> handler;

    MyObservableMeasurement(BiConsumer<Attributes, T> handler) {
      this.handler = handler;
    }

    @Override
    public void observe(double value, Attributes attributes) {
      final Attributes realAttributes = attributesProcessor.process(attributes, Context.current());
      T accumulation =
          aggregator.asyncAccumulation(DoubleMeasurement.createNoContext(value, realAttributes));
      handler.accept(realAttributes, accumulation);
    }

    @Override
    public void observe(double value) {
      observe(value, Attributes.empty());
    }

    @Override
    public void observe(long value, Attributes attributes) {
      final Attributes realAttributes = attributesProcessor.process(attributes, Context.current());
      T accumulation =
          aggregator.asyncAccumulation(LongMeasurement.createNoContext(value, realAttributes));
      handler.accept(realAttributes, accumulation);
    }

    @Override
    public void observe(long value) {
      observe(value, Attributes.empty());
    }
  }
}
