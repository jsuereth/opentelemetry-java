/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.aggregator;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.LongSumData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.instrument.LongMeasurement;
import io.opentelemetry.sdk.metrics.instrument.Measurement;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * An aggregator which returns Sum metrics.
 *
 * <p>This aggregator supports generating DELTA or CUMULATIVE sums, as well as monotonic or
 * non-monotonic.
 */
public class LongSumAggregator extends AbstractAggregator<LongAccumulation> {
  private final SumConfig config;
  private final Resource resource;
  private final InstrumentationLibraryInfo instrumentationLibrary;
  private final ExemplarSampler sampler;

  /**
   * Construct a sum from measurements.
   * 
   * @param config Configuration for the sum aggregation.
   * @param resource Resource to assocaiate metrics.
   * @param instrumentationLibrary InstrumentationLibrary to associate metrics.
   * @param startEpochNanos The start-of-application time.
   * @param sampler When/how to pull Exemplars.
   */
  public LongSumAggregator(
      SumConfig config,
      Resource resource,
      InstrumentationLibraryInfo instrumentationLibrary,
      long startEpochNanos,
      ExemplarSampler sampler) {
    super(startEpochNanos);
    this.config = config;
    this.resource = resource;
    this.instrumentationLibrary = instrumentationLibrary;
    this.sampler = sampler;
  }

  @Override
  public SynchronousHandle<LongAccumulation> createStreamStorage() {
    return new MyHandle(sampler);
  }

  // Note:  Storage handle has high contention and need atomic increments.
  static class MyHandle extends SynchronousHandle<LongAccumulation> {
    private final LongAdder count = new LongAdder();

    MyHandle(ExemplarSampler sampler) {
      super(sampler);
    }

    @Override
    protected void doRecord(Measurement value) {
      count.add(((LongMeasurement) value).getValue());
    }

    @Override
    protected LongAccumulation doAccumulateThenReset(Iterable<Measurement> exemplars) {
      return LongAccumulation.create(count.sumThenReset(), exemplars);
    }
  }

  @Override
  LongAccumulation asyncAccumulation(Measurement measurement) {
    if (measurement instanceof LongMeasurement) {
      return LongAccumulation.create(((LongMeasurement) measurement).getValue());
    }
    throw new IllegalArgumentException("LongSumAggregation can only handle long measurements.");
  }

  @Override
  protected boolean isStatefulCollector() {
    return config.getMeasurementTemporality() == AggregationTemporality.DELTA
        && config.getTemporality() == AggregationTemporality.CUMULATIVE;
  }

  @Override
  protected LongAccumulation merge(LongAccumulation current, LongAccumulation accumulated) {
    return LongAccumulation.create(
        current.getValue() + accumulated.getValue(), current.getExemplars());
  }

  @Override
  protected MetricData buildMetric(
      Map<Attributes, LongAccumulation> accumulated,
      long startEpochNanos,
      long lastEpochNanos,
      long epochNanos) {
    return MetricData.createLongSum(
        resource,
        instrumentationLibrary,
        config.getName(),
        config.getDescription(),
        config.getUnit(),
        LongSumData.create(
            config.isMonotonic(),
            config.getTemporality(),
            MetricDataUtils.toLongPointList(
                accumulated,
                config.getTemporality() == AggregationTemporality.CUMULATIVE
                    ? startEpochNanos
                    : lastEpochNanos,
                epochNanos)));
  }
}
