/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.aggregator;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.internal.GuardedBy;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.DoubleHistogramData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.instrument.InstrumentDescriptor;
import io.opentelemetry.sdk.metrics.instrument.Measurement;
import io.opentelemetry.sdk.resources.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Accumulates measurements into a Histogram metric.
 *
 * <p>This aggregator supports {@code DoubleMeasurement} and {@code LongMeasurement} inputs.
 */
public class DoubleHistogramAggregator extends AbstractAggregator<HistogramAccumulation> {
  private final InstrumentDescriptor instrument;
  private final Resource resource;
  private final InstrumentationLibraryInfo instrumentationLibrary;
  private final AggregationTemporality temporality;
  private final ExemplarSampler sampler;

  private final double[] boundaries;
  // a cache for converting to MetricData
  private final List<Double> boundaryList;

  public DoubleHistogramAggregator(
      InstrumentDescriptor instrument,
      Resource resource,
      InstrumentationLibraryInfo instrumentationLibrary,
      long startEpochNanos,
      AggregationTemporality temporality,
      double[] boundaries,
      ExemplarSampler sampler) {
    super(startEpochNanos);
    this.instrument = instrument;
    this.resource = resource;
    this.instrumentationLibrary = instrumentationLibrary;
    this.temporality = temporality;
    this.boundaries = boundaries;
    this.sampler = sampler;
    List<Double> boundaryList = new ArrayList<>(this.boundaries.length);
    for (double v : this.boundaries) {
      boundaryList.add(v);
    }
    this.boundaryList = Collections.unmodifiableList(boundaryList);
  }

  // Benchmark shows that linear search performs better than binary search with ordinary
  // buckets.
  private static int findBucketIndex(double[] boundaries, double value) {
    for (int i = 0; i < boundaries.length; ++i) {
      if (value <= boundaries[i]) {
        return i;
      }
    }
    return boundaries.length;
  }

  private static double valueOf(Measurement measurement) {
    return measurement.asDouble().getValue();
  }

  @Override
  public SynchronousHandle<HistogramAccumulation> createStreamStorage() {
    return new MyHandle(boundaries, sampler);
  }

  // Note:  Storage handle has high contention and need atomic increments.
  static class MyHandle extends SynchronousHandle<HistogramAccumulation> {
    // read-only
    private final double[] boundaries;

    @GuardedBy("lock")
    private double sum;

    @GuardedBy("lock")
    private final long[] counts;

    private final ReentrantLock lock = new ReentrantLock();

    MyHandle(double[] boundaries, ExemplarSampler sampler) {
      super(sampler);
      this.boundaries = boundaries;
      this.counts = new long[this.boundaries.length + 1];
      this.sum = 0;
    }

    @Override
    protected void doRecord(Measurement measurement) {
      double value = valueOf(measurement);
      int bucketIndex = findBucketIndex(this.boundaries, value);

      lock.lock();
      try {
        this.sum += value;
        this.counts[bucketIndex]++;
      } finally {
        lock.unlock();
      }
    }

    @Override
    protected HistogramAccumulation doAccumulateThenReset(Iterable<Measurement> exemplars) {
      lock.lock();
      try {
        HistogramAccumulation acc =
            HistogramAccumulation.create(sum, Arrays.copyOf(counts, counts.length), exemplars);
        this.sum = 0;
        Arrays.fill(this.counts, 0);
        return acc;
      } finally {
        lock.unlock();
      }
    }
  }

  @Override
  protected boolean isStatefulCollector() {
    return temporality == AggregationTemporality.CUMULATIVE;
  }

  @Override
  HistogramAccumulation asyncAccumulation(Measurement measurement) {
    double value = valueOf(measurement);
    int bucketIndex = findBucketIndex(this.boundaries, value);
    long[] counts = new long[this.boundaries.length + 1];
    counts[bucketIndex] = 1;
    return HistogramAccumulation.create(value, counts, Collections.emptyList());
  }

  @Override
  public final HistogramAccumulation merge(
      HistogramAccumulation current, HistogramAccumulation accumulated) {
    long[] mergedCounts = new long[current.getCounts().length];
    for (int i = 0; i < current.getCounts().length; ++i) {
      mergedCounts[i] = current.getCounts()[i] + accumulated.getCounts()[i];
    }

    return HistogramAccumulation.create(
        current.getSum() + accumulated.getSum(),
        mergedCounts,
        // We drop old exemplars when pulling new ones.
        current.getExemplars());
  }

  @Override
  protected MetricData buildMetric(
      Map<Attributes, HistogramAccumulation> accumulated,
      long startEpochNanos,
      long lastEpochNanos,
      long epochNanos) {
    return MetricData.createDoubleHistogram(
        resource,
        instrumentationLibrary,
        instrument.getName(),
        instrument.getDescription(),
        instrument.getUnit(),
        DoubleHistogramData.create(
            temporality,
            MetricDataUtils.toDoubleHistogramPointList(
                accumulated,
                isStatefulCollector() ? startEpochNanos : lastEpochNanos,
                epochNanos,
                boundaryList)));
  }
}
