package com.netk.mvolatrack;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HourlyActivityChartViewTest {
    @Test public void positivePointHasAValueLabel() {
        assertTrue(HourlyActivityChartView.shouldDrawValueLabel(1));
        assertTrue(HourlyActivityChartView.shouldDrawValueLabel(9));
    }

    @Test public void zeroAndInvalidNegativePointsHaveNoValueLabel() {
        assertFalse(HourlyActivityChartView.shouldDrawValueLabel(0));
        assertFalse(HourlyActivityChartView.shouldDrawValueLabel(-1));
    }
}
