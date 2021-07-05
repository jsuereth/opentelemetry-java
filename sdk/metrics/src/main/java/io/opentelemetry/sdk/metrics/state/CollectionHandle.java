/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.state;

import java.util.HashSet;
import java.util.Set;

/**
 * This class creates a highly efficient tracking of which processor-exporter pipelines have read
 * which metric data, allowing for async cleanup.
 */
public class CollectionHandle {
  private int bitIndex = 1;

  /** Construct a new, optimised, set for storing collection handles. */
  public static Set<CollectionHandle> mutableEmptySet() {
    // TODO: Optimise.
    // - contains
    // - containsAll
    // - add
    return new HashSet<>();
  }

  public static Set<CollectionHandle> setOf(CollectionHandle... handles) {
    Set<CollectionHandle> result = mutableEmptySet();
    for (CollectionHandle h : handles) {
      result.add(h);
    }
    return result;
  }

  // TODO: make this private.
  public CollectionHandle(int bitIndex) {
    this.bitIndex = bitIndex;
  }

  /** Returns the index for the bit flag of this collection handle. */
  int bitIndex() {
    return bitIndex;
  }

  @Override
  public int hashCode() {
    return bitIndex;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof CollectionHandle)) {
      return false;
    }
    return ((CollectionHandle) other).bitIndex == bitIndex;
  }

  @Override
  public String toString() {
    return "CollectionHandle(" + bitIndex + ")";
  }
}
