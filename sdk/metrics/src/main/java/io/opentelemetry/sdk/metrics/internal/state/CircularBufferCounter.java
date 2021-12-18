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
public class CircularBufferCounter implements ExponentialCounter {
  private static final int NULL_INDEX = Integer.MIN_VALUE;

  // TODO - we want to scale this in two ways, size of int used + size of backing array.
  private final int[] backing = new int[ExponentialBucketHistogramUtils.MAX_BUCKETS];
  private int endIndex = NULL_INDEX;
  private int startIndex = NULL_INDEX;
  private int baseIndex = NULL_INDEX;

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
      backing[0] = (int) delta;
      return true;
    }

    if (index > endIndex) {
      // Move end, check max size
      if (index - startIndex + 1 > backing.length) {
        return false;
      }
      endIndex = index;
    } else if (index < startIndex) {
      // Move end, check max size
      if (endIndex - index + 1 > backing.length) {
        return false;
      }
      startIndex = index;
    }
    int realIdx = toBufferIndex(index);
    // TODO - Atomic operation on array.
    backing[realIdx] += (int) delta;
    return true;
  }

  @Override
  public long get(int index) {
    return backing[toBufferIndex(index)];
  }

  @Override
  public boolean isEmpty() {
    return baseIndex == NULL_INDEX;
  }

  private int toBufferIndex(int index) {
    // Figure out the index relative to the start of the circular buffer.
    int result = index - baseIndex;
    if (result >= backing.length) {
      result -= backing.length;
    } else if (result < 0) {
      result += backing.length;
    }
    return result;
  }

  @Override
  public String toString() {
    return "CircularBuffer";
  }
}
