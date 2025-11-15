package com.example.outpick.utils;

import android.view.MotionEvent;
import android.view.View;

public class TouchResizeDragListener implements View.OnTouchListener {

    private float dX, dY;
    private float initialDistance = 0f;
    private float initialScale = 1f;

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:
                // First finger touches – record position
                dX = view.getX() - event.getRawX();
                dY = view.getY() - event.getRawY();
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                // Second finger touches – prepare for pinch zoom
                initialDistance = calculateSpacing(event);
                initialScale = view.getScaleX(); // Assuming uniform scaling
                return true;

            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() == 1) {
                    // Dragging with one finger
                    view.setX(event.getRawX() + dX);
                    view.setY(event.getRawY() + dY);

                } else if (event.getPointerCount() == 2) {
                    // Pinch zoom with two fingers
                    float newDistance = calculateSpacing(event);
                    if (initialDistance > 0f) {
                        float scaleFactor = newDistance / initialDistance;
                        view.setScaleX(initialScale * scaleFactor);
                        view.setScaleY(initialScale * scaleFactor);
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                // One or more fingers lifted – reset and allow click
                initialDistance = 0f;
                initialScale = view.getScaleX();  // Save the latest scale
                view.performClick();              // Important: enables click events
                return true;

            default:
                return false;
        }
    }

    // Calculates distance between two touch points
    private float calculateSpacing(MotionEvent event) {
        if (event.getPointerCount() < 2) return 0f;
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }
}
