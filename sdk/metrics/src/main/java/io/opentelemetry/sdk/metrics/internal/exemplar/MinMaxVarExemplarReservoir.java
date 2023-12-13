/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.internal.exemplar;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.metrics.data.DoubleExemplarData;
import io.opentelemetry.sdk.metrics.data.ExemplarData;
import io.opentelemetry.sdk.metrics.data.LongExemplarData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A {@link FixedSizeExemplarReservoir} which attempts to preserve more values at extreme ranges.
 *
 * <p>This reservoir divides itself into four components:
 *
 * <ul>
 *   <li>The latest maximum value
 *   <li>The latest minimum value
 *   <li>Values mostly within standard deviation
 *   <li>Values mostly outside standard deviation
 * </ul>
 *
 * <p>This algorithm does not guarantee any distribution or preservation of distribution.
 */
class MinMaxVarExemplarReservoir<T extends ExemplarData> implements ExemplarReservoir<T> {

  private final ReservoirCell minCell;
  private final ReservoirCell maxCell;
  private final ReservoirCell[] large;
  private final ReservoirCell[] small;

  // Tau is effectively - "sum of weights" / "count of weights"
  private final StatisticsAlgorithmHelper stats = new StatisticsAlgorithmHelper();

  private final BiFunction<ReservoirCell, Attributes, T> mapAndResetCell;
  private final Supplier<Random> randomSupplier;

  private MinMaxVarExemplarReservoir(
      Clock clock,
      int size,
      Supplier<Random> randomSupplier,
      BiFunction<ReservoirCell, Attributes, T> mapAndResetCell) {
    this.minCell = new ReservoirCell(clock);
    this.maxCell = new ReservoirCell(clock);
    int halfSize = size / 2;
    this.large = new ReservoirCell[halfSize];
    this.small = new ReservoirCell[halfSize];
    for (int i = 0; i < halfSize; ++i) {
      this.large[i] = new ReservoirCell(clock);
      this.small[i] = new ReservoirCell(clock);
    }
    this.randomSupplier = randomSupplier;
    this.mapAndResetCell = mapAndResetCell;
  }

  static MinMaxVarExemplarReservoir<LongExemplarData> createLong(
      Clock clock, int size, Supplier<Random> randomSupplier) {
    return new MinMaxVarExemplarReservoir<>(
        clock, size, randomSupplier, ReservoirCell::getAndResetLong);
  }

  static MinMaxVarExemplarReservoir<DoubleExemplarData> createDouble(
      Clock clock, int size, Supplier<Random> randomSupplier) {
    return new MinMaxVarExemplarReservoir<>(
        clock, size, randomSupplier, ReservoirCell::getAndResetDouble);
  }

  private Optional<ReservoirCell> pickCell(ReservoirCell[] reservoir) {
    long count = stats.numMeasurements();
    int index = this.randomSupplier.get().nextInt(count > 0 ? (int) count : 1);
    if (index < reservoir.length) {
      return Optional.of(reservoir[index]);
    }
    return Optional.empty();
  }

  @Override
  @SuppressWarnings("SystemOut")
  public void offerDoubleMeasurement(double value, Attributes attributes, Context context) {
    switch (stats.offerWeighted(value)) {
      case REPLACE_MIN:
        // Swap existing max cell into min or large.
        if (stats.numMeasurements() == 2) {
          this.maxCell.swap(this.minCell);
        } else {
          pickCell(this.large).ifPresent(cell -> cell.swap(minCell));
        }
        this.minCell.recordDoubleMeasurement(value, attributes, context);
        break;
      case REPLACE_MAX:
        // Swap existing min cell into large reservoir.
        // Note - We should, ideally, figure out which reservoir
        // to put backup cells into.
        pickCell(this.large).ifPresent(cell -> cell.swap(maxCell));
        this.maxCell.recordDoubleMeasurement(value, attributes, context);
        break;
      case SAMPLE_OUTSIDE_VARIANCE:
        pickCell(this.large)
            .ifPresent(cell -> cell.recordDoubleMeasurement(value, attributes, context));
        break;
      case SAMPLE_WITHIN_VARIANCE:
        pickCell(this.small)
            .ifPresent(cell -> cell.recordDoubleMeasurement(value, attributes, context));
        break;
    }
  }

  @Override
  public void offerLongMeasurement(long value, Attributes attributes, Context context) {
    switch (stats.offerWeighted((double) value)) {
      case REPLACE_MIN:
        // Swap existing max cell into min or large.
        if (stats.numMeasurements() == 2) {
          this.maxCell.swap(this.minCell);
        } else {
          pickCell(this.large).ifPresent(cell -> cell.swap(minCell));
        }
        this.minCell.recordLongMeasurement(value, attributes, context);
        break;
      case REPLACE_MAX:
        // Swap existing min cell into large reservoir.
        pickCell(this.large).ifPresent(cell -> cell.swap(maxCell));
        this.maxCell.recordLongMeasurement(value, attributes, context);
        break;
      case SAMPLE_OUTSIDE_VARIANCE:
        pickCell(this.large)
            .ifPresent(cell -> cell.recordLongMeasurement(value, attributes, context));
        break;
      case SAMPLE_WITHIN_VARIANCE:
        pickCell(this.small)
            .ifPresent(cell -> cell.recordLongMeasurement(value, attributes, context));
        break;
    }
  }

  @Override
  public List<T> collectAndReset(Attributes pointAttributes) {
    if (!stats.hasMeasurements()) {
      return Collections.emptyList();
    }
    // Note: we are collecting exemplars from buckets piecemeal, but we
    // could still be sampling exemplars during this process.
    List<T> results = new ArrayList<>();
    T maxResult = mapAndResetCell.apply(maxCell, pointAttributes);
    if (maxResult != null) {
      results.add(maxResult);
    }
    for (ReservoirCell reservoirCell : this.large) {
      T result = mapAndResetCell.apply(reservoirCell, pointAttributes);
      if (result != null) {
        results.add(result);
      }
    }
    for (ReservoirCell reservoirCell : this.small) {
      T result = mapAndResetCell.apply(reservoirCell, pointAttributes);
      if (result != null) {
        results.add(result);
      }
    }
    T minResult = mapAndResetCell.apply(minCell, pointAttributes);
    if (minResult != null) {
      results.add(minResult);
    }
    stats.reset();
    return Collections.unmodifiableList(results);
  }

  /** Determines which reservoir to sample a value in, by weight. */
  private enum AlgorithmDecision {
    REPLACE_MIN, // Replace the min-bucket sample
    REPLACE_MAX, // Replace the max-bucket sample
    SAMPLE_WITHIN_VARIANCE, // Randomnly sample in the "small" reservoir
    SAMPLE_OUTSIDE_VARIANCE // Randomnly sample in the "large" reservoir
  }

  /**
   * Keeps track of statistics of seen measurements to better optimise kept exemplars.
   *
   * <p>Tracks number of measurements, sum of weights, sum^2 of weights, current minimum weight and
   * current maximum weight. When seeing a new measurement, one of four reservoirs is picked: MIN,
   * MAX, WITHIN_VARIANCE or OUTSIDE_VARIANCE.
   *
   * <p>This must be thread safe.
   */
  private static class StatisticsAlgorithmHelper {
    private long count = 0;
    private double sum = 0;
    private double sumSquared = 0;
    private double curMin = Double.MAX_VALUE;
    private double curMax = Double.MIN_VALUE;

    synchronized void reset() {
      this.count = 0;
      this.sum = 0;
      this.sumSquared = 0;
      this.curMin = Double.MAX_VALUE;
      this.curMax = Double.MIN_VALUE;
    }

    synchronized long numMeasurements() {
      return count;
    }

    synchronized boolean hasMeasurements() {
      return count > 0;
    }

    synchronized AlgorithmDecision offerWeighted(double weight) {
      this.count += 1;
      this.sum += weight;
      this.sumSquared += weight * weight;
      if (weight < curMin) {
        curMin = weight;
        return AlgorithmDecision.REPLACE_MIN;
      } else if (weight > curMax) {
        curMax = weight;
        return AlgorithmDecision.REPLACE_MAX;
      } else {
        double mean = sum / count;
        double var = Math.abs(Math.sqrt((sumSquared / count) - (mean * mean)));
        if (weight <= (mean + var) && weight >= (mean - var)) {
          return AlgorithmDecision.SAMPLE_WITHIN_VARIANCE;
        }
      }
      return AlgorithmDecision.SAMPLE_OUTSIDE_VARIANCE;
    }
  }
}
