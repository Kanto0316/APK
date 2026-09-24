package com.example.testapp;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.HorizontalScrollView;

/** Horizontal table scroller which reserves two-finger gestures for semantic table zoom. */
public class ZoomableTableScrollView extends HorizontalScrollView {
    public interface ZoomListener {
        void onZoom(float scaleFactor);
        void onResetZoom();
    }

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;
    private ZoomListener zoomListener;

    public ZoomableTableScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        scaleDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override public boolean onScale(ScaleGestureDetector detector) {
                        if (zoomListener != null) zoomListener.onZoom(detector.getScaleFactor());
                        return true;
                    }
                });
        gestureDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onDoubleTap(MotionEvent event) {
                        if (zoomListener != null) zoomListener.onResetZoom();
                        return true;
                    }
                });
    }

    public void setZoomListener(ZoomListener listener) {
        zoomListener = listener;
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        // Observe the complete stream even while the RecyclerView owns vertical drags.
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        // A second pointer switches ownership from horizontal/vertical scrolling to zoom.
        return event.getPointerCount() > 1 || super.onInterceptTouchEvent(event);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() > 1 || scaleDetector.isInProgress()) return true;
        return super.onTouchEvent(event);
    }
}
