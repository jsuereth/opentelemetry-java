/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.state;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.metrics.aggregator.Aggregator;
import io.opentelemetry.sdk.metrics.instrument.DoubleMeasurement;
import io.opentelemetry.sdk.metrics.view.AttributesProcessor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class AsynchronousInstrumentStorageTest {

  private static final Attributes ATTR_KV = Attributes.of(stringKey("k"), "v");

  @Test
  @SuppressWarnings("unchecked")
  public void asynchronousStorage_usesAttributesProcessor() {
    final Aggregator<Object> mockAggregator = Mockito.mock(Aggregator.class);
    AsynchronousInstrumentStorage<Object> storage =
        new AsynchronousInstrumentStorage<>(
            (ObservableDoubleMeasurement measure) -> measure.observe(1.0, ATTR_KV),
            mockAggregator,
            (attributes, context) -> Attributes.empty());
    assertThat(storage.accumulateThenReset()).containsKey(Attributes.empty());

    Mockito.verify(mockAggregator)
        .asyncAccumulation(DoubleMeasurement.create(1.0, Attributes.empty(), Context.root()));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void asynchronousStorage_sendsMeasurementsToAggregatorAndCompletes() {
    final Aggregator<Object> mockAggregator = Mockito.mock(Aggregator.class);
    AsynchronousInstrumentStorage<Object> storage =
        new AsynchronousInstrumentStorage<>(
            (ObservableDoubleMeasurement measure) -> measure.observe(1.0, ATTR_KV),
            mockAggregator,
            AttributesProcessor.NOOP);
    assertThat(storage.accumulateThenReset()).containsKey(ATTR_KV);

    // Verify aggregator received mesurement and completion timestmap.
    Mockito.verify(mockAggregator)
        .asyncAccumulation(
            DoubleMeasurement.create(1.0, Attributes.of(stringKey("k"), "v"), Context.root()));
  }
}
