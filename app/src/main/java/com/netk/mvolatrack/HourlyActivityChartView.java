package com.netk.mvolatrack;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** Compact, touch-selectable line chart containing the intervals of one day. */
public final class HourlyActivityChartView extends View {
    private static final int MAX_VALUE_TICKS = 5;
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private final NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.FRENCH);
    private int[] values = new int[24];
    private int maximum = 1;
    private int intervalMinutes = 60;
    private int selectedInterval = -1;
    private String descriptionLabel = "Activité";
    private String singularValueLabel = "transaction";
    private String pluralValueLabel = "transactions";

    public HourlyActivityChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        int secondary = ContextCompat.getColor(context, R.color.sms_text_secondary);
        textPaint.setColor(secondary);
        textPaint.setTextSize(11 * getResources().getDisplayMetrics().scaledDensity);
        gridPaint.setColor(ContextCompat.getColor(context, R.color.statistics_grid));
        gridPaint.setStrokeWidth(dp(1));
        linePaint.setColor(ContextCompat.getColor(context, R.color.sms_accent));
        linePaint.setStrokeWidth(dp(2));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        pointPaint.setColor(ContextCompat.getColor(context, R.color.sms_accent));
        setFocusable(true);
    }

    void setData(int[] intervalValues, int minutes) {
        setData(intervalValues, minutes, "Activité", "transaction", "transactions");
    }

    void setData(int[] intervalValues, int minutes, String description, String singular,
                 String plural) {
        if (minutes != 15 && minutes != 30 && minutes != 60) {
            throw new IllegalArgumentException("Interval must be 15, 30 or 60 minutes");
        }
        int expectedSize = 24 * 60 / minutes;
        values = intervalValues == null ? new int[expectedSize] : intervalValues.clone();
        if (values.length != expectedSize) {
            throw new IllegalArgumentException(expectedSize + " interval values required");
        }
        intervalMinutes = minutes;
        descriptionLabel = description;
        singularValueLabel = singular;
        pluralValueLabel = plural;
        maximum = 1;
        for (int value : values) maximum = Math.max(maximum, value);
        selectedInterval = -1;
        setContentDescription(buildDescription());
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        List<Integer> referenceValues = referenceValues(values, MAX_VALUE_TICKS);
        float left = chartLeft(referenceValues);
        float right = getWidth() - getPaddingRight() - dp(10);
        float top = getPaddingTop() + dp(30);
        float bottom = getHeight() - getPaddingBottom() - dp(35);
        float width = Math.max(1, right - left);
        float height = Math.max(1, bottom - top);

        textPaint.setTextAlign(Paint.Align.RIGHT);
        drawReferenceLine(canvas, 0, left, right, bottom, height);
        for (int value : referenceValues) {
            float y = bottom - height * value / maximum;
            canvas.drawLine(left, y, right, y, gridPaint);
            canvas.drawText(numberFormat.format(value), left - dp(7), y + dp(4), textPaint);
        }
        Path path = new Path();
        for (int index = 0; index < values.length; index++) {
            float x = pointX(left, width, index);
            float y = bottom - height * values[index] / maximum;
            if (index == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        canvas.drawPath(path, linePaint);
        float normalRadius = intervalMinutes == 60 ? dp(3) : dp(1.5f);
        for (int index = 0; index < values.length; index++) {
            float x = pointX(left, width, index);
            float y = bottom - height * values[index] / maximum;
            canvas.drawCircle(x, y, index == selectedInterval ? dp(5) : normalRadius, pointPaint);
        }
        textPaint.setTextAlign(Paint.Align.CENTER);
        int intervalsPerHour = 60 / intervalMinutes;
        for (int index = 0; index < values.length; index++) {
            int hour = index / intervalsPerHour;
            if ((index % (3 * intervalsPerHour) == 0) || index == values.length - 1) {
                float x = pointX(left, width, index);
                canvas.drawText(String.format(Locale.FRENCH, "%02dh", index == values.length - 1 ? 23 : hour), x,
                        bottom + dp(20), textPaint);
            }
        }
        if (selectedInterval >= 0) {
            String suffix = values[selectedInterval] == 1
                    ? " " + singularValueLabel : " " + pluralValueLabel;
            textPaint.setColor(ContextCompat.getColor(getContext(), R.color.sms_text_primary));
            textPaint.setFakeBoldText(true);
            canvas.drawText(intervalLabel(selectedInterval) + " · "
                            + numberFormat.format(values[selectedInterval]) + suffix,
                    (left + right) / 2,
                    getPaddingTop() + dp(15), textPaint);
            textPaint.setFakeBoldText(false);
            textPaint.setColor(ContextCompat.getColor(getContext(), R.color.sms_text_secondary));
        }
    }

    private void drawReferenceLine(Canvas canvas, int value, float left, float right,
                                   float bottom, float height) {
        float y = bottom - height * value / maximum;
        canvas.drawLine(left, y, right, y, gridPaint);
        canvas.drawText(numberFormat.format(value), left - dp(7), y + dp(4), textPaint);
    }

    /** Returns distinct point levels to display on Y, capped to keep the chart readable. */
    static List<Integer> referenceValues(int[] intervalValues, int maximumTickCount) {
        if (intervalValues == null || maximumTickCount <= 0) return Collections.emptyList();
        LinkedHashSet<Integer> distinct = new LinkedHashSet<>();
        for (int value : intervalValues) if (value > 0) distinct.add(value);
        List<Integer> sorted = new ArrayList<>(distinct);
        Collections.sort(sorted);
        if (sorted.size() <= maximumTickCount) return sorted;

        List<Integer> selected = new ArrayList<>(maximumTickCount);
        for (int index = 0; index < maximumTickCount; index++) {
            int sourceIndex = Math.round(index * (sorted.size() - 1f) / (maximumTickCount - 1f));
            selected.add(sorted.get(sourceIndex));
        }
        return selected;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            float left = chartLeft(referenceValues(values, MAX_VALUE_TICKS));
            float width = Math.max(1, getWidth() - getPaddingRight() - dp(10) - left);
            selectedInterval = Math.max(0, Math.min(values.length - 1,
                    Math.round((event.getX() - left) * (values.length - 1) / width)));
            invalidate();
            performClick();
        }
        return true;
    }

    private float chartLeft(List<Integer> references) {
        float widestTick = textPaint.measureText("0");
        for (int value : references) {
            widestTick = Math.max(widestTick, textPaint.measureText(numberFormat.format(value)));
        }
        return getPaddingLeft() + widestTick + dp(9);
    }

    @Override public boolean performClick() { super.performClick(); return true; }

    private String buildDescription() {
        StringBuilder description = new StringBuilder(descriptionLabel).append(intervalMinutes == 60
                ? " par heure. " : " par intervalle. ");
        for (int index = 0; index < values.length; index++) {
            description.append(intervalLabel(index)).append(" : ").append(values[index])
                    .append(values[index] == 1 ? " " + singularValueLabel + ". "
                            : " " + pluralValueLabel + ". ");
        }
        return description.toString();
    }

    private float pointX(float left, float width, int index) {
        return left + width * index / Math.max(1f, values.length - 1f);
    }

    private String intervalLabel(int index) {
        int start = index * intervalMinutes;
        int end = start + intervalMinutes - 1;
        return String.format(Locale.FRENCH, "%02d:%02d–%02d:%02d",
                start / 60, start % 60, end / 60, end % 60);
    }

    private float dp(int value) { return value * density; }
    private float dp(float value) { return value * density; }
}
