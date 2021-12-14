/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.internal.state;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ExponentialCounterTest {

  static Stream<ExponentialCounter> counterProviders() {
    return Stream.of(
        new AdaptingCircularBufferCounter(), new CircularBufferCounter(), new MapCounter());
  }

  @ParameterizedTest
  @MethodSource("counterProviders")
  void expandLower(ExponentialCounter counter) {
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

  @ParameterizedTest
  @MethodSource("counterProviders")
  void shouldFailAtLimit(ExponentialCounter counter) {
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
