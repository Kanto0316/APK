package com.netk.mvolatrack;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class HourlyActivityChartViewTest {
    @Test public void distinctPointValuesBecomeSortedYAxisReferences() {
        assertEquals(Arrays.asList(2, 5, 11),
                HourlyActivityChartView.referenceValues(
                        new int[]{0, 11, 2, 5, 2, 0}, 5));
    }

    @Test public void emptyAndInvalidValuesDoNotAddYAxisReferences() {
        assertEquals(Collections.emptyList(),
                HourlyActivityChartView.referenceValues(new int[]{0, -1}, 5));
        assertEquals(Collections.emptyList(),
                HourlyActivityChartView.referenceValues(null, 5));
    }

    @Test public void manyPointValuesAreCappedButKeepTheRangeExtremes() {
        assertEquals(Arrays.asList(1, 3, 6, 8, 10),
                HourlyActivityChartView.referenceValues(
                        new int[]{10, 9, 8, 7, 6, 5, 4, 3, 2, 1}, 5));
    }
}
