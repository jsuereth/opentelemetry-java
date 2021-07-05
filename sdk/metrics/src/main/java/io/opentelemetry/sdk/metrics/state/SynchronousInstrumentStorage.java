/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.state;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.metrics.aggregator.Aggregator;
import io.opentelemetry.sdk.metrics.aggregator.SynchronousHandle;
import io.opentelemetry.sdk.metrics.instrument.Measurement;
import io.opentelemetry.sdk.metrics.view.AttributesProcessor;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/** This class provides concurrent storage for synchronous instrument measurements. */
public class SynchronousInstrumentStorage<T> implements AccumulationProvider<T> {
  private final Aggregator<T> aggregator;
  private final ConcurrentHashMap<Attributes, SynchronousHandle<T>> perAttrributeStorage;
  private final ReentrantLock collectLock;
  private final AttributesProcessor attributesProcessor;

  SynchronousInstrumentStorage(Aggregator<T> aggregator, AttributesProcessor attributesProcessor) {
    perAttrributeStorage = new ConcurrentHashMap<>();
    collectLock = new ReentrantLock();
    this.aggregator = aggregator;
    this.attributesProcessor = attributesProcessor;
  }

  /** Collects bucketed metrics and resets the underyling storage for the next collection period. */
  @Override
  public Map<Attributes, T> accumulateThenReset() {
    // TODO - Figure out how DELTA + CUMULATIVE interact with this.  Right now we
    // only get delta values from synchronous instruments.
    collectLock.lock();
    try {
      Map<Attributes, T> result = new HashMap<>();
      for (Map.Entry<Attributes, SynchronousHandle<T>> entry : perAttrributeStorage.entrySet()) {
        boolean unmappedEntry = entry.getValue().tryUnmap();
        if (unmappedEntry) {
          // If able to unmap then remove the record from the current Map. This can race with the
          // acquire but because we requested a specific value only one will succeed.
          perAttrributeStorage.remove(entry.getKey(), entry.getValue());
        }
        T accumulation = entry.getValue().accumulateThenReset();
        if (accumulation == null) {
          continue;
        }
        // Feed latest batch to the aggregator.
        result.put(entry.getKey(), accumulation);
      }
      return result;
    } finally {
      collectLock.unlock();
    }
  }

  private final StorageHandle lateBoundStorageHandle =
      new StorageHandle() {
        @Override
        public void record(Measurement measurement) {
          SynchronousInstrumentStorage.this.record(measurement);
        }

        @Override
        public void release() {}
      };

  /**
   * Obtain exclusive write access to metric stream for this instrument defined by this set of
   * attributes.
   */
  public StorageHandle bind(Attributes attributes) {
    Objects.requireNonNull(attributes, "attributes");
    if (attributesProcessor.usesContext()) {
      return lateBoundStorageHandle;
    }
    return doBind(attributesProcessor.process(attributes, Context.current()));
  }

  /** version of "bind" that does NOT call attributesProcessor. */
  private StorageHandle doBind(Attributes attributes) {
    SynchronousHandle<T> storageHandle = perAttrributeStorage.get(attributes);
    if (storageHandle != null && storageHandle.acquire()) {
      // At this moment it is guaranteed that the Bound is in the map and will not be removed.
      return storageHandle;
    }

    // Missing entry or no longer mapped, try to add a new entry.
    storageHandle = aggregator.createStreamStorage();
    while (true) {
      SynchronousHandle<T> boundStorageHandle =
          perAttrributeStorage.putIfAbsent(attributes, storageHandle);
      if (boundStorageHandle != null) {
        if (boundStorageHandle.acquire()) {
          // At this moment it is guaranteed that the Bound is in the map and will not be removed.
          return boundStorageHandle;
        }
        // Try to remove the boundAggregator. This will race with the collect method, but only one
        // will succeed.
        perAttrributeStorage.remove(attributes, boundStorageHandle);
        continue;
      }
      return storageHandle;
    }
  }
  /** Writes a measurement into the appropriate metric stream. */
  public void record(Measurement measurement) {
    StorageHandle handle =
        doBind(attributesProcessor.process(measurement.getAttributes(), measurement.getContext()));
    try {
      handle.record(measurement);
    } finally {
      handle.release();
    }
  }
}
