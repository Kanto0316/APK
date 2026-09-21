package com.example.testapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/** Draws the bottom bar and its smooth cradle without requiring Material Components. */
public class NotchedBottomBarView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float density;

    public NotchedBottomBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        paint.setColor(ContextCompat.getColor(context, R.color.sms_bottom_bar));
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        paint.setShadowLayer(6 * density, 0, -2 * density, 0x30000000);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float top = 24 * density;
        float center = width / 2f;
        float corner = 18 * density;
        float cradleHalfWidth = 48 * density;
        float cradleDepth = 28 * density;

        path.reset();
        path.moveTo(0, getHeight());
        path.lineTo(0, top + corner);
        path.quadTo(0, top, corner, top);
        path.lineTo(center - cradleHalfWidth, top);
        path.cubicTo(center - 36 * density, top,
                center - 38 * density, top + cradleDepth,
                center, top + cradleDepth);
        path.cubicTo(center + 38 * density, top + cradleDepth,
                center + 36 * density, top,
                center + cradleHalfWidth, top);
        path.lineTo(width - corner, top);
        path.quadTo(width, top, width, top + corner);
        path.lineTo(width, getHeight());
        path.close();
        canvas.drawPath(path, paint);
    }
}
