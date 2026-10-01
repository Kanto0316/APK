package com.netk.mvolatrack.invoice;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/** Clips the invoice itself, so both the modal and exported bitmap have ticket perforations. */
public class PerforatedTicketLayout extends LinearLayout {
    private final Path ticket = new Path();
    private final Path holes = new Path();

    public PerforatedTicketLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
    }

    @Override protected void dispatchDraw(Canvas canvas) {
        float radius = getResources().getDisplayMetrics().density * 7f;
        ticket.reset();
        ticket.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
        holes.reset();
        for (float x = radius; x < getWidth(); x += radius * 2.35f) {
            holes.addCircle(x, 0, radius, Path.Direction.CW);
            holes.addCircle(x, getHeight(), radius, Path.Direction.CW);
        }
        ticket.op(holes, Path.Op.DIFFERENCE);
        int checkpoint = canvas.save();
        canvas.clipPath(ticket);
        canvas.drawColor(0xffffffff);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(checkpoint);
    }
}
