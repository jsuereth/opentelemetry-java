/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.state;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.aggregator.Aggregator;
import io.opentelemetry.sdk.metrics.data.MetricData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class stores accumulation points and exports appropriately for mutliple exporters.
 *
 * <p>Internally, this class tracks collection per-handle, and will preserve accumulation values
 * across multiple export calls. Every time `collectAndReset` is called, any live metrics are pulled
 * from synchronous or asynchrous instruments.
 */
public final class MultiExporterPointStore<T> implements InstrumentStorage {
  /** A lambda which will pull the latest accumulation values for this point. */
  private final AccumulationProvider<T> pointProvider;

  private final Aggregator<T> aggregator;
  private final List<Point> historicalPoints;
  private final Map<CollectionHandle, Long> lastTimestamps;
  private final ReentrantLock collectLock = new ReentrantLock();

  public MultiExporterPointStore(AccumulationProvider<T> pointProvider, Aggregator<T> aggregator) {
    this.pointProvider = pointProvider;
    this.aggregator = aggregator;
    this.historicalPoints = new ArrayList<Point>();
    this.lastTimestamps = new HashMap<>();
  }

  /**
   * Collect the metric data associated with this point store.
   *
   * @param handle The current pipeline pulling metrics.
   * @param allCollectors All possible pipelines that could use this metric.
   * @param startEpochNanos The start timestamp of this MeterProvider.
   * @param epochNanos The current timestamp of this metrics collection.
   * @return The metric to expose, or {@code null}.
   */
  @Override
  public List<MetricData> collectAndReset(
      CollectionHandle handle,
      Set<CollectionHandle> allCollectors,
      long startEpochNanos,
      long epochNanos) {
    collectLock.lock();
    try {
      // Next we pull the latest aggregation values since the last time and dump then in our point
      // list.
      collectLatestValues();
      Map<Attributes, T> accumulated = new HashMap<>();
      long lastEpochNanos = lastTimestamps.getOrDefault(handle, startEpochNanos);
      // Now we iterator + mark our points for this exporter.
      for (Point p : historicalPoints) {
        if (!p.wasReadBy(handle)) {
          accumulated = merge(accumulated, p.read(handle));
        }
      }
      // Finally, we clean up any points that have been read by all collectors and mark our last
      // read time.
      cleanupStalePoints(allCollectors);
      lastTimestamps.put(handle, epochNanos);
      // Now we return our metric stream point, if we have any values.
      if (accumulated.isEmpty()) {
        return Collections.emptyList();
      }
      // TODO: Figure out how to merge CUMULATIVE SYNCHRONOUS measurements with previous cumulative.
      // TODO: Figure out how to diff DELTA ASYCNHRONOUS measuremnets with previous recorded value.
      MetricData result =
          aggregator.buildMetric(accumulated, startEpochNanos, lastEpochNanos, epochNanos);
      if (result == null) {
        return Collections.emptyList();
      }
      return Arrays.asList(result);
    } finally {
      collectLock.unlock();
    }
  }

  /** Merges the next point into the (mutable) accumulated map of existing points. */
  private Map<Attributes, T> merge(Map<Attributes, T> accumulated, Map<Attributes, T> next) {
    next.forEach(
        (k, v) -> {
          if (accumulated.containsKey(k)) {
            accumulated.put(k, aggregator.merge(accumulated.get(k), v));
          } else {
            accumulated.put(k, v);
          }
        });
    return accumulated;
  }

  /** Remove all points from our historical list that have been collected. */
  private void cleanupStalePoints(Set<CollectionHandle> allCollectors) {
    Iterator<Point> i = historicalPoints.iterator();
    while (i.hasNext()) {
      Point p = i.next();
      if (p.wasReadByAll(allCollectors)) {
        i.remove();
      }
    }
  }

  /** Pulls the latest metric values from synchronous/asynchronous storage. */
  private void collectLatestValues() {
    // we always append to the end of the list, implciitly keeping the list sorted by timestamp.
    historicalPoints.add(new Point(pointProvider.accumulateThenReset()));
  }

  // TODO - Do points need to remember their "start" time?
  private class Point {
    private final Set<CollectionHandle> readers;
    private final Map<Attributes, T> accumulation;

    Point(Map<Attributes, T> accumulation) {
      this.readers = CollectionHandle.mutableEmptySet();
      this.accumulation = accumulation;
    }
    /** Returns true if this collection pipeline already used this point. */
    boolean wasReadBy(CollectionHandle handle) {
      return readers.contains(handle);
    }
    /** Returns true if all collection pipelines have used this point. */
    boolean wasReadByAll(Set<CollectionHandle> handles) {
      return readers.containsAll(handles);
    }

    /** Read the value of this point, and mark it read by a given collection pipeline. */
    Map<Attributes, T> read(CollectionHandle handle) {
      this.readers.add(handle);
      return accumulation;
    }
  }
}
