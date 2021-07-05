/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.aggregator;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.instrument.Measurement;
import java.util.Map;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A processor that can generate aggregators for metrics streams while also combining those streams
 * into {@link MetricData}.
 *
 * <p>Aggregators are expected to be stateless, preserving all state in the assocaited type {@code
 * T}. This means all methods should only make use of passed in data to calculate outputs.
 */
@ThreadSafe
public interface Aggregator<T> {
  /**
   * Construct a handle for storing highly-concurrent measurement input.
   *
   * <p>SynchronousHandle instances *must* be threadsafe and allow for high contention across
   * threads.
   */
  public SynchronousHandle<T> createStreamStorage();

  /** 
   * Merge the currently recording metric point with the previous accumulation. 
   * 
   * <p> This is called when two paths can lead to a measurement and their values should be joined.
   */
  public T merge(T current, T accumulated);

  /** 
   * Return the accumulation for an asynchronously recorded measurement. 
   * 
   * This is called when an asycnhronous measurement is taken, and is expected to return a CUMULATIVE accumulation (to be later manipualted if calcualting DELTA).
   */
  public T asyncAccumulation(Measurement measurement);

  /**
   * Construct a metric stream for this aggregator.
   *
   * @param accumulated The underlying stream points, by {@link Attributes}.
   * @param startEpochNanos The start time for the metrics SDK.
   * @param lastEpochNanos The time of the last collection period (i.e. delta start time).
   * @param epochNanos The current collection period time (i.e. end time).
   */
  public MetricData buildMetric(
      Map<Attributes, T> accumulated, long startEpochNanos, long lastEpochNanos, long epochNanos);
}
