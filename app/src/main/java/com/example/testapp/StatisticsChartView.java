package com.example.testapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.OverScroller;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Horizontally scrollable vertical daily bar chart. */
public final class StatisticsChartView extends View {
    private static final int TARGET_TICK_COUNT = 5;
    // A fixed slot keeps bars visually consistent and shows roughly 5–7 days on a phone.
    static final int SLOT_WIDTH_DP = 48;
    static final int BAR_WIDTH_DP = 22;
    private static final int CHART_HEIGHT_DP = 232;

    private final float density;
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SimpleDateFormat shortDateFormat = new SimpleDateFormat("dd/MM", Locale.FRENCH);
    private final SimpleDateFormat accessibleDateFormat =
            new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
    private final NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.FRENCH);
    private final OverScroller scroller;
    private final GestureDetector gestureDetector;
    private List<SmsStatistics.DailyCount> data = new ArrayList<>();
    private long scaleMaximum = 1;
    private long tickStep = 1;
    private float horizontalOffset;
    private boolean ariaryValues;

    public StatisticsChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        textPaint.setColor(ContextCompat.getColor(context, R.color.sms_text_secondary));
        textPaint.setTextSize(12 * getResources().getDisplayMetrics().scaledDensity);
        gridPaint.setColor(ContextCompat.getColor(context, R.color.statistics_grid));
        gridPaint.setStrokeWidth(density);
        axisPaint.setColor(ContextCompat.getColor(context, R.color.sms_text_secondary));
        axisPaint.setStrokeWidth(density);
        barPaint.setColor(ContextCompat.getColor(context, R.color.sms_accent));
        scroller = new OverScroller(context);
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent event) {
                if (!scroller.isFinished()) scroller.abortAnimation();
                return true;
            }

            @Override public boolean onScroll(MotionEvent first, MotionEvent current,
                    float distanceX, float distanceY) {
                setHorizontalOffset(horizontalOffset + distanceX);
                return true;
            }

            @Override public boolean onFling(MotionEvent first, MotionEvent current,
                    float velocityX, float velocityY) {
                scroller.fling(Math.round(horizontalOffset), 0, Math.round(-velocityX), 0,
                        0, Math.round(maximumOffset()), 0, 0);
                postInvalidateOnAnimation();
                return true;
            }
        });
        setMinimumHeight(dp(CHART_HEIGHT_DP));
        setFocusable(true);
    }

    void setData(List<SmsStatistics.DailyCount> dailyCounts) {
        setData(dailyCounts, 0);
    }

    void setData(List<SmsStatistics.DailyCount> dailyCounts, int firstVisibleIndex) {
        ariaryValues = false;
        updateData(dailyCounts, firstVisibleIndex);
    }

    void setBonusData(List<SmsStatistics.DailyCount> dailyBonuses) {
        ariaryValues = true;
        updateData(dailyBonuses, 0);
    }

    private void updateData(List<SmsStatistics.DailyCount> dailyCounts, int firstVisibleIndex) {
        data = dailyCounts == null ? new ArrayList<>() : new ArrayList<>(dailyCounts);
        long maximum = 1;
        for (SmsStatistics.DailyCount item : data) maximum = Math.max(maximum, item.count);
        tickStep = readableStep(maximum);
        scaleMaximum = readableMaximum(maximum);
        horizontalOffset = Math.max(0, Math.min(dp(SLOT_WIDTH_DP) * firstVisibleIndex,
                maximumOffset()));
        setContentDescription(buildDescription());
        invalidate();
    }

    /** Selects a 1, 2 or 5 multiplied by a power of ten, aiming for five intervals. */
    static long readableStep(long maximum) {
        if (maximum <= TARGET_TICK_COUNT) return 1;
        double roughStep = maximum / (double) TARGET_TICK_COUNT;
        double magnitude = Math.pow(10, Math.floor(Math.log10(roughStep)));
        double normalized = roughStep / magnitude;
        long multiplier = normalized <= 1 ? 1 : normalized <= 2 ? 2 : 5;
        return Math.max(1, Math.round(multiplier * magnitude));
    }

    static long readableMaximum(long maximum) {
        long step = readableStep(maximum);
        return ((Math.max(1L, maximum) + step - 1) / step) * step;
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(resolveSize(dp(320), widthMeasureSpec),
                resolveSize(dp(CHART_HEIGHT_DP), heightMeasureSpec));
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        horizontalOffset = Math.min(horizontalOffset, maximumOffset());
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        boolean handled = gestureDetector.onTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_UP && !handled) performClick();
        return handled || super.onTouchEvent(event);
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            setHorizontalOffset(scroller.getCurrX());
            postInvalidateOnAnimation();
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data.isEmpty()) return;

        float chartLeft = getPaddingLeft() + dp(54);
        float chartRight = getWidth() - getPaddingRight() - dp(8);
        float chartTop = getPaddingTop() + dp(43);
        float chartBottom = getHeight() - getPaddingBottom() - dp(43);
        float chartHeight = Math.max(1, chartBottom - chartTop);

        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.sms_text_secondary));
        textPaint.setTextSize(11 * getResources().getDisplayMetrics().scaledDensity);
        textPaint.setFakeBoldText(false);
        textPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(ariaryValues ? "Bonus (Ar)" : "Nombre de SMS", chartLeft,
                getPaddingTop() + dp(17), textPaint);
        textPaint.setTextSize(12 * getResources().getDisplayMetrics().scaledDensity);

        int intervals = Math.max(1, (int) (scaleMaximum / tickStep));
        for (int tick = 0; tick <= intervals; tick++) {
            long value = tick * tickStep;
            float y = chartBottom - chartHeight * value / scaleMaximum;
            textPaint.setColor(ContextCompat.getColor(getContext(), R.color.sms_text_secondary));
            textPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(numberFormat.format(value), chartLeft - dp(8), y + dp(4), textPaint);
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint);
        }
        canvas.drawLine(chartLeft, chartTop, chartLeft, chartBottom, axisPaint);
        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, axisPaint);

        float viewportWidth = Math.max(1, chartRight - chartLeft);
        float contentWidth = data.size() * dp(SLOT_WIDTH_DP);
        float leadingSpace = contentWidth < viewportWidth ? (viewportWidth - contentWidth) / 2f : 0;
        int save = canvas.save();
        canvas.clipRect(chartLeft, chartTop - dp(24), chartRight, chartBottom + dp(30));
        for (int index = 0; index < data.size(); index++) {
            SmsStatistics.DailyCount item = data.get(index);
            float centerX = chartLeft + leadingSpace + dp(SLOT_WIDTH_DP) * (index + 0.5f)
                    - horizontalOffset;
            if (item.count > 0) {
                float barTop = chartBottom - chartHeight * item.count / scaleMaximum;
                RectF bar = new RectF(centerX - dp(BAR_WIDTH_DP) / 2f, barTop,
                        centerX + dp(BAR_WIDTH_DP) / 2f, chartBottom);
                canvas.drawRoundRect(bar, dp(4), dp(4), barPaint);
                textPaint.setColor(ContextCompat.getColor(getContext(), R.color.sms_text_primary));
                textPaint.setFakeBoldText(true);
                textPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(formatValue(item.count), centerX, barTop - dp(7), textPaint);
            }
            textPaint.setFakeBoldText(false);
            textPaint.setColor(ContextCompat.getColor(getContext(), R.color.sms_text_secondary));
            canvas.drawText(shortDateFormat.format(new Date(item.localDayTimestamp)), centerX,
                    chartBottom + dp(21), textPaint);
        }
        canvas.restoreToCount(save);

        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.sms_text_primary));
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Date", (chartLeft + chartRight) / 2f, getHeight() - getPaddingBottom() - dp(8),
                textPaint);
    }

    private void setHorizontalOffset(float offset) {
        horizontalOffset = Math.max(0, Math.min(offset, maximumOffset()));
        invalidate();
    }

    private float maximumOffset() {
        float viewportWidth = getWidth() - getPaddingLeft() - getPaddingRight() - dp(62);
        return maximumOffsetDp(data.size(), viewportWidth / density) * density;
    }

    static float contentWidthDp(int dateCount) {
        return Math.max(0, dateCount) * SLOT_WIDTH_DP;
    }

    static float maximumOffsetDp(int dateCount, float viewportWidthDp) {
        return Math.max(0, contentWidthDp(dateCount) - Math.max(0, viewportWidthDp));
    }

    private String buildDescription() {
        StringBuilder result = new StringBuilder(ariaryValues
                ? "Bonus quotidiens. " : "Activité des messages. ");
        for (SmsStatistics.DailyCount item : data) {
            result.append(accessibleDateFormat.format(new Date(item.localDayTimestamp))).append(" : ")
                    .append(formatValue(item.count)).append(ariaryValues ? ". " : " SMS. ");
        }
        return result.toString();
    }

    private String formatValue(long value) {
        return numberFormat.format(value) + (ariaryValues ? " Ar" : "");
    }

    private int dp(int value) { return Math.round(value * density); }
}
