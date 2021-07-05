/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.state;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.ObservableMeasurement;
import io.opentelemetry.sdk.metrics.aggregator.Aggregator;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.instrument.Measurement;
import io.opentelemetry.sdk.metrics.view.AttributesProcessor;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** A storage location for metric data collected by a specific instrument instance. */
public interface InstrumentStorage {
  // TODO - Instrument storage sxhould have an identifier of what metric it produces.

  /** Collects the metrics from this storage and resets for the next collection period. */
  List<MetricData> collectAndReset(
      CollectionHandle handle,
      Set<CollectionHandle> allCollectors,
      long startEpochNanos,
      long epochNanos);

  /**
   * Construct storage for a synchronous insturment.
   *
   * <p>This guarantees a high-concurrency friendly implementation.
   */
  public static <T> WriteableInstrumentStorage createSynchronous(
      Aggregator<T> aggregator, AttributesProcessor processor) {
    final SynchronousInstrumentStorage<T> provider =
        new SynchronousInstrumentStorage<T>(aggregator, processor);
    final MultiExporterPointStore<T> pointStore =
        new MultiExporterPointStore<T>(provider, aggregator);
    // TODO: clean this up.
    return new WriteableInstrumentStorage() {

      @Override
      public List<MetricData> collectAndReset(
          CollectionHandle handle,
          Set<CollectionHandle> allCollectors,
          long startEpochNanos,
          long epochNanos) {
        return pointStore.collectAndReset(handle, allCollectors, startEpochNanos, epochNanos);
      }

      @Override
      public StorageHandle bind(Attributes attributes) {
        return provider.bind(attributes);
      }

      @Override
      public void record(Measurement measurement) {
        provider.record(measurement);
      }
    };
  }

  /**
   * Construct a "storage" for aysnchronous instrument.
   *
   * <p>This storage will poll for data when asked.
   */
  public static <T> InstrumentStorage createAsynchronous(
      Consumer<? extends ObservableMeasurement> callback,
      Aggregator<T> aggregator,
      AttributesProcessor processor) {
    AsynchronousInstrumentStorage<T> provider =
        new AsynchronousInstrumentStorage<>(callback, aggregator, processor);
    MultiExporterPointStore<T> pointStore = new MultiExporterPointStore<T>(provider, aggregator);
    return pointStore;
  }
}
