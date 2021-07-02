/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics;

import com.google.auto.value.AutoValue;
import io.opentelemetry.api.metrics.ObservableMeasurement;
import io.opentelemetry.sdk.metrics.aggregator.Aggregator;
import io.opentelemetry.sdk.metrics.aggregator.DoubleHistogramAggregator;
import io.opentelemetry.sdk.metrics.aggregator.DoubleSumAggregator;
import io.opentelemetry.sdk.metrics.aggregator.ExemplarSampler;
import io.opentelemetry.sdk.metrics.aggregator.LastValueAggregator;
import io.opentelemetry.sdk.metrics.aggregator.LongSumAggregator;
import io.opentelemetry.sdk.metrics.aggregator.SumConfig;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.instrument.InstrumentDescriptor;
import io.opentelemetry.sdk.metrics.state.InstrumentStorage;
import io.opentelemetry.sdk.metrics.state.MeterProviderSharedState;
import io.opentelemetry.sdk.metrics.state.MeterSharedState;
import io.opentelemetry.sdk.metrics.state.WriteableInstrumentStorage;
import java.util.function.Consumer;

/** A default implementation of measurement processor. */
@AutoValue
public abstract class DefaultMeasurementProcessor implements MeasurementProcessor {
  // Default histograms are designed for average HTTP/RPC latency measurements in "millisecond".
  // Note: These are similar to prometheus default buckets (although prometheus uses "seconds").
  private static final double[] DEFAULT_HISTOGRAM_BOUNDARIES = {
    5, 10, 25, 50, 75, 100, 250, 500, 750, 1_000, 2_500, 5_000, 7_500, 10_000
  };

  /**
   * Histogram boundaries that will be chosen when no hints given.
   *
   * <p>Note: Default histogram boundaries are designed for HTTP/RPC latencies measured in
   * nanoseconds, and are the following buckets: [5ms, 10ms, 25ms, 75ms, 100ms, 250ms, 500ms, 750ms,
   * 1s, 2.5s, 5s, 7.5s, 10s]
   */
  @SuppressWarnings("mutable")
  public abstract double[] getDefaultHistogramBoundaries();

  /** Sampler chosen for exemplars on insturments when no hints given. */
  public abstract ExemplarSampler getDefaultExemplarSampler();

  @Override
  public WriteableInstrumentStorage createStorage(
      InstrumentDescriptor instrument,
      MeterProviderSharedState meterProviderSharedState,
      MeterSharedState meterSharedState) {
    switch (instrument.getType()) {
      case COUNTER:
      case UP_DOWN_COUNTER:
        return InstrumentStorage.createSynchronous(
            instrument,
            meterProviderSharedState,
            meterSharedState,
            sum(instrument, meterProviderSharedState, meterSharedState));
      case HISTOGRAM:
        return InstrumentStorage.createSynchronous(
            instrument,
            meterProviderSharedState,
            meterSharedState,
            histogram(instrument, meterProviderSharedState, meterSharedState));
      default:
        throw new IllegalArgumentException(
            "Unsupported synchronous metric: " + instrument.getType());
    }
  }

  @Override
  public <T extends ObservableMeasurement> InstrumentStorage createAsynchronousStorage(
      InstrumentDescriptor instrument,
      MeterProviderSharedState meterProviderSharedState,
      MeterSharedState meterSharedState,
      Consumer<T> callback) {
    switch (instrument.getType()) {
      case OBSERVABLE_SUM:
      case OBSERVBALE_UP_DOWN_SUM:
        return InstrumentStorage.createAsynchronous(
            instrument, callback, sum(instrument, meterProviderSharedState, meterSharedState));
      case OBSERVABLE_GAUGE:
        return InstrumentStorage.createAsynchronous(
            instrument, callback, gauge(instrument, meterProviderSharedState, meterSharedState));
      default:
        throw new IllegalArgumentException(
            "Unsupported asynchronous metric: " + instrument.getType());
    }
  }

  protected Aggregator<?> gauge(
      InstrumentDescriptor instrument,
      MeterProviderSharedState meterProviderSharedState,
      MeterSharedState meterSharedState) {
    return new LastValueAggregator(
        instrument,
        meterProviderSharedState.getResource(),
        meterSharedState.getInstrumentationLibraryInfo(),
        meterProviderSharedState.getStartEpochNanos(),
        AggregationTemporality.CUMULATIVE,
        getDefaultExemplarSampler());
  }

  protected Aggregator<?> sum(
      InstrumentDescriptor instrument,
      MeterProviderSharedState meterProviderSharedState,
      MeterSharedState meterSharedState) {
    SumConfig config = SumConfig.buildDefaultFromInstrument(instrument);
    // Default aggregation for synchronous sum.
    switch (instrument.getValueType()) {
      case LONG:
        return new LongSumAggregator(
            config,
            meterProviderSharedState.getResource(),
            meterSharedState.getInstrumentationLibraryInfo(),
            meterProviderSharedState.getStartEpochNanos(),
            getDefaultExemplarSampler());
      case DOUBLE:
        return new DoubleSumAggregator(
            config,
            meterProviderSharedState.getResource(),
            meterSharedState.getInstrumentationLibraryInfo(),
            meterProviderSharedState.getStartEpochNanos(),
            getDefaultExemplarSampler());
    }
    throw new IllegalArgumentException("Unsupported sum: " + instrument.getValueType());
  }

  protected Aggregator<?> histogram(
      InstrumentDescriptor instrument,
      MeterProviderSharedState meterProviderSharedState,
      MeterSharedState meterSharedState) {
    // The Double processor will convert LongMeasurements to doubles.
    return new DoubleHistogramAggregator(
        instrument,
        meterProviderSharedState.getResource(),
        meterSharedState.getInstrumentationLibraryInfo(),
        meterProviderSharedState.getStartEpochNanos(),
        // TODO: Aggregation temporality, boundaries, sampling come from hint api..
        AggregationTemporality.CUMULATIVE,
        getDefaultHistogramBoundaries(),
        getDefaultExemplarSampler());
  }

  static Builder builder() {
    return new AutoValue_DefaultMeasurementProcessor.Builder()
        .setDefaultHistogramBoundaries(DEFAULT_HISTOGRAM_BOUNDARIES)
        .setDefaultExemplarSampler(ExemplarSampler.NEVER);
  }

  /** Builder for {@link DefaultMeasurementProcessor}. */
  @AutoValue.Builder
  abstract static class Builder {
    /** Sets the exemplar sampler to use when no hints are provided. */
    abstract Builder setDefaultExemplarSampler(ExemplarSampler sampler);

    /** Sets the histogram boundaries to use when no hints are provided. */
    abstract Builder setDefaultHistogramBoundaries(double[] boundaries);

    abstract DefaultMeasurementProcessor build();
  }
}
