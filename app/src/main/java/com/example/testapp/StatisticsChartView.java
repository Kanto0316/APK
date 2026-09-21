package com.example.testapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Lightweight horizontal bar chart: X is the SMS count and Y is the local calendar date. */
public final class StatisticsChartView extends View {
    private static final int TICK_COUNT = 4;
    private final float density;
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy", Locale.FRENCH);
    private List<SmsStatistics.DailyCount> data = new ArrayList<>();
    private int scaleMaximum = 1;

    public StatisticsChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        textPaint.setColor(ContextCompat.getColor(context, R.color.sms_text_secondary));
        textPaint.setTextSize(12 * getResources().getDisplayMetrics().scaledDensity);
        gridPaint.setColor(ContextCompat.getColor(context, R.color.statistics_grid));
        gridPaint.setStrokeWidth(density);
        barPaint.setColor(ContextCompat.getColor(context, R.color.sms_accent));
        setMinimumHeight(dp(190));
    }

    void setData(List<SmsStatistics.DailyCount> dailyCounts) {
        data = dailyCounts == null ? new ArrayList<>() : new ArrayList<>(dailyCounts);
        int maximum = 1;
        for (SmsStatistics.DailyCount item : data) maximum = Math.max(maximum, item.count);
        scaleMaximum = readableMaximum(maximum);
        setMinimumHeight(dp(82 + data.size() * 48));
        setContentDescription(buildDescription());
        requestLayout();
        invalidate();
    }

    static int readableMaximum(int maximum) {
        if (maximum <= 4) return Math.max(4, maximum);
        int magnitude = 1;
        while (maximum / magnitude >= 10) magnitude *= 10;
        int step = Math.max(1, magnitude / 2);
        return ((maximum + step - 1) / step) * step;
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = Math.max(getSuggestedMinimumHeight(), dp(82 + data.size() * 48));
        setMeasuredDimension(resolveSize(dp(320), widthMeasureSpec),
                resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data.isEmpty()) return;
        float labelWidth = dp(72);
        float valueWidth = dp(34);
        float chartLeft = getPaddingLeft() + labelWidth;
        float chartRight = getWidth() - getPaddingRight() - valueWidth;
        float chartWidth = Math.max(1, chartRight - chartLeft);
        float axisTop = getPaddingTop() + dp(30);
        float rowsTop = axisTop + dp(25);
        float rowsBottom = rowsTop + data.size() * dp(48);

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.sms_text_primary));
        textPaint.setFakeBoldText(true);
        canvas.drawText("Nombre de SMS", chartLeft, getPaddingTop() + dp(15), textPaint);
        textPaint.setFakeBoldText(false);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.sms_text_secondary));
        for (int tick = 0; tick <= TICK_COUNT; tick++) {
            float x = chartLeft + chartWidth * tick / TICK_COUNT;
            int value = Math.round(scaleMaximum * tick / (float) TICK_COUNT);
            textPaint.setTextAlign(tick == 0 ? Paint.Align.LEFT
                    : tick == TICK_COUNT ? Paint.Align.RIGHT : Paint.Align.CENTER);
            canvas.drawText(String.valueOf(value), x, axisTop, textPaint);
            canvas.drawLine(x, axisTop + dp(7), x, rowsBottom, gridPaint);
        }

        for (int index = 0; index < data.size(); index++) {
            SmsStatistics.DailyCount item = data.get(index);
            float centerY = rowsTop + index * dp(48) + dp(17);
            textPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(dateFormat.format(new Date(item.localDayTimestamp)),
                    chartLeft - dp(8), centerY + dp(4), textPaint);
            float barRight = chartLeft + chartWidth * item.count / scaleMaximum;
            canvas.drawRoundRect(new RectF(chartLeft, centerY - dp(10), barRight,
                    centerY + dp(10)), dp(4), dp(4), barPaint);
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setColor(ContextCompat.getColor(getContext(), R.color.sms_text_primary));
            textPaint.setFakeBoldText(true);
            canvas.drawText(String.valueOf(item.count), Math.min(barRight + dp(7), chartRight + dp(7)),
                    centerY + dp(4), textPaint);
            textPaint.setFakeBoldText(false);
            textPaint.setColor(ContextCompat.getColor(getContext(), R.color.sms_text_secondary));
        }
    }

    private String buildDescription() {
        StringBuilder result = new StringBuilder("Activité des messages. ");
        for (SmsStatistics.DailyCount item : data) {
            result.append(dateFormat.format(new Date(item.localDayTimestamp))).append(" : ")
                    .append(item.count).append(" SMS. ");
        }
        return result.toString();
    }

    private int dp(int value) { return Math.round(value * density); }
}
