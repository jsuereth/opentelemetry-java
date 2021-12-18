/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.internal.state;

import io.opentelemetry.sdk.metrics.internal.aggregator.ExponentialBucketHistogramUtils;

/**
 * A circle-buffer-backed exponential counter.
 *
 * <p>The first recorded value becomes the 'baseIndex'. Going backwards leads to start/stop index
 *
 * <p>This expand start/End index as it sees values.
 *
 * <p>This class is NOT thread-safe. It is expected to be behind a synchronized incrementer.
 */
public class AdaptingCircularBufferCounter implements ExponentialCounter {
  private static final int NULL_INDEX = Integer.MIN_VALUE;
  private int endIndex;
  private int startIndex;
  private int baseIndex;
  private final AdaptingIntegerArray backing;

  /** Instantiate a MapCounter. */
  public AdaptingCircularBufferCounter() {
    this.backing = new AdaptingIntegerArray(ExponentialBucketHistogramUtils.MAX_BUCKETS);
    this.endIndex = NULL_INDEX;
    this.startIndex = NULL_INDEX;
    this.baseIndex = NULL_INDEX;
  }

  /**
   * Create an independent copy of another ExponentialCounter.
   *
   * @param otherCounter another exponential counter to make a deep copy of.
   */
  public AdaptingCircularBufferCounter(ExponentialCounter otherCounter) {
    if (otherCounter instanceof AdaptingCircularBufferCounter) {
      this.startIndex = otherCounter.getIndexStart();
      this.endIndex = otherCounter.getIndexEnd();
      this.baseIndex = ((AdaptingCircularBufferCounter) otherCounter).baseIndex;
      this.backing =
          new AdaptingIntegerArray(((AdaptingCircularBufferCounter) otherCounter).backing);
    } else {
      this.backing = new AdaptingIntegerArray(ExponentialBucketHistogramUtils.MAX_BUCKETS);
      // copy values
      for (int i = otherCounter.getIndexStart(); i <= otherCounter.getIndexEnd(); i++) {
        long val = otherCounter.get(i);
        if (val != 0) {
          this.increment(i, val);
        }
      }
    }
  }

  @Override
  public int getIndexStart() {
    return startIndex;
  }

  @Override
  public int getIndexEnd() {
    return endIndex;
  }

  @Override
  public boolean increment(int index, long delta) {
    if (baseIndex == NULL_INDEX) {
      startIndex = index;
      endIndex = index;
      baseIndex = index;
      backing.increment(0, delta);
      return true;
    }

    if (index > endIndex) {
      // Move end, check max size
      if (index - startIndex + 1 > backing.length()) {
        return false;
      }
      endIndex = index;
    } else if (index < startIndex) {
      // Move end, check max size
      if (endIndex - index + 1 > backing.length()) {
        return false;
      }
      startIndex = index;
    }
    int realIdx = toBufferIndex(index);
    backing.increment(realIdx, delta);
    return true;
  }

  @Override
  public long get(int index) {
    return backing.get(toBufferIndex(index));
  }

  @Override
  public boolean isEmpty() {
    return baseIndex == NULL_INDEX;
  }

  private int toBufferIndex(int index) {
    // Figure out the index relative to the start of the circular buffer.
    int result = index - baseIndex;
    if (result >= backing.length()) {
      result -= backing.length();
    } else if (result < 0) {
      result += backing.length();
    }
    return result;
  }

  @Override
  public String toString() {
    return "AdaptingCircularBuffer";
  }

  /** Factory that creates/copys this bucket type. */
  public static final class Factory implements ExponentialCounterFactory {
    @Override
    public ExponentialCounter create() {
      return new AdaptingCircularBufferCounter();
    }

    @Override
    public ExponentialCounter copy(ExponentialCounter other) {
      return new AdaptingCircularBufferCounter(other);
    }
  }
}
