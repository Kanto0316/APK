package com.netk.mvolatrack;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Compact, touch-selectable line chart containing the intervals of one day. */
public final class HourlyActivityChartView extends View {
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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
        valuePaint.setColor(ContextCompat.getColor(context, R.color.sms_text_primary));
        valuePaint.setTextSize(10 * getResources().getDisplayMetrics().scaledDensity);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setFakeBoldText(true);
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
        float left = getPaddingLeft() + dp(34);
        float right = getWidth() - getPaddingRight() - dp(10);
        // Keep enough headroom for value labels, including labels moved up to avoid a neighbour.
        float top = getPaddingTop() + dp(52);
        float bottom = getHeight() - getPaddingBottom() - dp(35);
        float width = Math.max(1, right - left);
        float height = Math.max(1, bottom - top);

        textPaint.setTextAlign(Paint.Align.RIGHT);
        for (int tick = 0; tick <= maximum; tick += Math.max(1, maximum)) {
            float y = bottom - height * tick / maximum;
            canvas.drawLine(left, y, right, y, gridPaint);
            canvas.drawText(String.valueOf(tick), left - dp(7), y + dp(4), textPaint);
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
        drawValueLabels(canvas, left, width, bottom, height);
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

    /** Draws every meaningful value and moves close labels into rows above their points. */
    private void drawValueLabels(Canvas canvas, float left, float width, float chartBottom,
                                 float chartHeight) {
        List<RectF> occupied = new ArrayList<>();
        Paint.FontMetrics metrics = valuePaint.getFontMetrics();
        float labelHeight = metrics.descent - metrics.ascent;
        float rowHeight = labelHeight + dp(2);
        float highestBaseline = getPaddingTop() - metrics.ascent;

        for (int index = 0; index < values.length; index++) {
            int value = values[index];
            if (!shouldDrawValueLabel(value)) continue;

            String label = numberFormat.format(value);
            float x = pointX(left, width, index);
            float pointY = chartBottom - chartHeight * value / maximum;
            float preferredBaseline = pointY - dp(7);
            float baseline = preferredBaseline;
            RectF bounds = null;

            // Prefer the position immediately above the point. Only move farther upward when
            // another visible value would overlap it.
            int availableRows = Math.max(1,
                    (int) ((preferredBaseline - highestBaseline) / rowHeight) + 1);
            for (int row = 0; row < availableRows; row++) {
                float candidateBaseline = preferredBaseline - row * rowHeight;
                RectF candidate = labelBounds(label, x, candidateBaseline, metrics);
                if (!intersectsAny(candidate, occupied)) {
                    baseline = candidateBaseline;
                    bounds = candidate;
                    break;
                }
            }
            if (bounds == null) {
                // Extremely dense data can exhaust the vertical rows; retain every label and
                // use a deterministic stagger rather than silently dropping values.
                int staggerRows = intervalMinutes == 15 ? 4 : intervalMinutes == 30 ? 2 : 1;
                baseline = Math.max(highestBaseline,
                        preferredBaseline - (index % staggerRows) * rowHeight);
                bounds = labelBounds(label, x, baseline, metrics);
            }
            canvas.drawText(label, x, baseline, valuePaint);
            occupied.add(bounds);
        }
    }

    private RectF labelBounds(String label, float centerX, float baseline,
                              Paint.FontMetrics metrics) {
        float halfWidth = valuePaint.measureText(label) / 2f + dp(2);
        return new RectF(centerX - halfWidth, baseline + metrics.ascent - dp(1),
                centerX + halfWidth, baseline + metrics.descent + dp(1));
    }

    private static boolean intersectsAny(RectF candidate, List<RectF> occupied) {
        for (RectF bounds : occupied) {
            if (RectF.intersects(candidate, bounds)) return true;
        }
        return false;
    }

    static boolean shouldDrawValueLabel(int value) {
        return value > 0;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            float left = getPaddingLeft() + dp(34);
            float width = Math.max(1, getWidth() - getPaddingRight() - dp(10) - left);
            selectedInterval = Math.max(0, Math.min(values.length - 1,
                    Math.round((event.getX() - left) * (values.length - 1) / width)));
            invalidate();
            performClick();
        }
        return true;
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
