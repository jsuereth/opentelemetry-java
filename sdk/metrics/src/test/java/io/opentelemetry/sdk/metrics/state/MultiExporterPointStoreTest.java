/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.state;

import static io.opentelemetry.sdk.testing.assertj.metrics.MetricAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.metrics.aggregator.Aggregator;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.LongSumData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class MultiExporterPointStoreTest {
  private static final Resource RESOURCE = Resource.empty();
  private static final InstrumentationLibraryInfo INSTRUMENTATION_LIBRARY_INFO =
      InstrumentationLibraryInfo.empty();
  private final CollectionHandle collector1 = new CollectionHandle(1);
  private final CollectionHandle collector2 = new CollectionHandle(2);
  private final Set<CollectionHandle> allCollectors =
      CollectionHandle.setOf(collector1, collector2);

  private static final Attributes KV_ATTR = Attributes.builder().put("k", "v").build();

  @Test
  @SuppressWarnings("unchecked")
  public void collectAndReset_remembersValuesForCollectors() {
    final Aggregator<Long> aggregator = Mockito.mock(Aggregator.class);
    final AccumulationProvider<Long> provider = Mockito.mock(AccumulationProvider.class);
    final MultiExporterPointStore<Long> pointStore =
        new MultiExporterPointStore<>(provider, aggregator);
    final Map<Attributes, Long> accumulationResult = new HashMap<>();
    accumulationResult.put(KV_ATTR, 1L);
    // Stub provider to return same value every time.
    // This means *EVERY* call to `collectAndReset` will produce a new 1-value.
    when(provider.accumulateThenReset()).thenReturn(accumulationResult);
    // Stub aggregator to merge Long, and create metric from longs.
    when(aggregator.merge(anyObject(), anyObject()))
        .thenAnswer(i -> i.<Long>getArgument(0) + i.<Long>getArgument(1));
    when(aggregator.buildMetric(anyObject(), anyLong(), anyLong(), anyLong()))
        .thenAnswer((i) -> createSum(i.getArgument(1), i.getArgument(3), i.getArgument(0)));

    // Calling collect will create a second 1-value point that's immediately returned for collector1
    assertThat(pointStore.collectAndReset(collector1, allCollectors, 0, 10))
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasLongSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point).hasStartEpochNanos(0).hasEpochNanos(10).hasValue(1)));
    // Calling collect will create a second 1-value point that's immediately returned for collector1
    assertThat(pointStore.collectAndReset(collector1, allCollectors, 0, 20))
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasLongSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point).hasStartEpochNanos(0).hasEpochNanos(20).hasValue(1)));
    // Calling collect will create a second 1-value point that's added to previously unseen 2-points
    // from other collectors.
    assertThat(pointStore.collectAndReset(collector2, allCollectors, 0, 20))
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasLongSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point).hasStartEpochNanos(0).hasEpochNanos(20).hasValue(3)));
    // Collector 1 should now see the point from collector 2's collection, but no new points.
    assertThat(pointStore.collectAndReset(collector1, allCollectors, 0, 30))
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasLongSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point).hasStartEpochNanos(0).hasEpochNanos(30).hasValue(2)));
  }

  private static MetricData createSum(
      long startEpochNanos, long endEpochNanos, Map<Attributes, Long> points) {
    Collection<LongPointData> finalPoints =
        points.entrySet().stream()
            .map(
                (e) ->
                    LongPointData.create(startEpochNanos, endEpochNanos, e.getKey(), e.getValue()))
            .collect(Collectors.toList());
    return MetricData.createLongSum(
        RESOURCE,
        INSTRUMENTATION_LIBRARY_INFO,
        "test",
        "",
        "1",
        LongSumData.create(
            /* isMonotonic= */ true, AggregationTemporality.CUMULATIVE, finalPoints));
  }
}
