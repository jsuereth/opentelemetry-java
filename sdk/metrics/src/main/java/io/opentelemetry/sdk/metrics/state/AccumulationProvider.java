/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.state;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;

import java.util.Map;

/** This interface denotes a mechanism to pull Attribtes->Accumulated Value points. */
public interface AccumulationProvider<T> {
  /**
   * Pull the current accumulated metric values, and reset any storage required for continued
   * collection.
   */
  public Map<Attributes, T> accumulateThenReset();
}
