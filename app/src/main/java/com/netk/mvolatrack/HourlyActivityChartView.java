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
import java.util.Locale;

/** Compact, touch-selectable line chart containing all 24 hours of one day. */
public final class HourlyActivityChartView extends View {
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private final NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.FRENCH);
    private int[] values = new int[24];
    private int maximum = 1;
    private int selectedHour = -1;

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

    void setData(int[] hourlyValues) {
        values = hourlyValues == null ? new int[24] : hourlyValues.clone();
        if (values.length != 24) throw new IllegalArgumentException("24 hourly values required");
        maximum = 1;
        for (int value : values) maximum = Math.max(maximum, value);
        selectedHour = -1;
        setContentDescription(buildDescription());
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left = getPaddingLeft() + dp(34);
        float right = getWidth() - getPaddingRight() - dp(10);
        float top = getPaddingTop() + dp(30);
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
        for (int hour = 0; hour < 24; hour++) {
            float x = left + width * hour / 23f;
            float y = bottom - height * values[hour] / maximum;
            if (hour == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        canvas.drawPath(path, linePaint);
        for (int hour = 0; hour < 24; hour++) {
            float x = left + width * hour / 23f;
            float y = bottom - height * values[hour] / maximum;
            canvas.drawCircle(x, y, hour == selectedHour ? dp(5) : dp(3), pointPaint);
        }
        textPaint.setTextAlign(Paint.Align.CENTER);
        for (int hour = 0; hour < 24; hour++) {
            if (hour % 3 == 0 || hour == 23) {
                float x = left + width * hour / 23f;
                canvas.drawText(String.format(Locale.FRENCH, "%02dh", hour), x,
                        bottom + dp(20), textPaint);
            }
        }
        if (selectedHour >= 0) {
            String suffix = values[selectedHour] == 1 ? " transaction" : " transactions";
            textPaint.setColor(ContextCompat.getColor(getContext(), R.color.sms_text_primary));
            textPaint.setFakeBoldText(true);
            canvas.drawText(String.format(Locale.FRENCH, "%02dh · %s%s", selectedHour,
                    numberFormat.format(values[selectedHour]), suffix), (left + right) / 2,
                    getPaddingTop() + dp(15), textPaint);
            textPaint.setFakeBoldText(false);
            textPaint.setColor(ContextCompat.getColor(getContext(), R.color.sms_text_secondary));
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            float left = getPaddingLeft() + dp(34);
            float width = Math.max(1, getWidth() - getPaddingRight() - dp(10) - left);
            selectedHour = Math.max(0, Math.min(23,
                    Math.round((event.getX() - left) * 23f / width)));
            invalidate();
            performClick();
        }
        return true;
    }

    @Override public boolean performClick() { super.performClick(); return true; }

    private String buildDescription() {
        StringBuilder description = new StringBuilder("Activité par heure. ");
        for (int hour = 0; hour < 24; hour++) {
            description.append(String.format(Locale.FRENCH, "%02dh : %d transaction%s. ", hour,
                    values[hour], values[hour] == 1 ? "" : "s"));
        }
        return description.toString();
    }

    private float dp(int value) { return value * density; }
}
