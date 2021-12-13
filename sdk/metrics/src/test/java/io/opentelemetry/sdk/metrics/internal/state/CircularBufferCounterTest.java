/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.internal.state;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CircularBufferCounterTest {

  private ExponentialCounter counter;

  @BeforeEach
  void setUp() {
    counter = new CircularBufferCounter();
  }

  @Test
  void expandLower() {
    assertThat(counter.increment(10, 1)).isTrue();
    // Add BEFORE the initial see (array index 0) and make sure we wrap around the datastructure.
    assertThat(counter.increment(0, 1)).isTrue();
    assertThat(counter.get(10)).isEqualTo(1);
    assertThat(counter.get(0)).isEqualTo(1);
    assertThat(counter.getIndexStart()).as("index start").isEqualTo(0);
    assertThat(counter.getIndexEnd()).as("index end").isEqualTo(10);
    // Add AFTER initial entry and just push back end.
    assertThat(counter.increment(20, 1)).isTrue();
    assertThat(counter.get(20)).isEqualTo(1);
    assertThat(counter.get(10)).isEqualTo(1);
    assertThat(counter.get(0)).isEqualTo(1);
    assertThat(counter.getIndexStart()).isEqualTo(0);
    assertThat(counter.getIndexEnd()).isEqualTo(20);
  }

  @Test
  void shouldFailAtLimit() {
    assertThat(counter.increment(0, 1)).isTrue();
    assertThat(counter.increment(319, 1)).isTrue();
    // Check state
    assertThat(counter.getIndexStart()).as("index start").isEqualTo(0);
    assertThat(counter.getIndexEnd()).as("index start").isEqualTo(319);
    assertThat(counter.get(0)).as("counter[0]").isEqualTo(1);
    assertThat(counter.get(319)).as("counter[319]").isEqualTo(1);
    // Adding over the maximum # of buckets
    assertThat(counter.increment(3000, 1)).isFalse();
  }
}
