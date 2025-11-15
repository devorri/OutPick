package com.example.outpick.utils;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * ResizableImageView is a custom ImageView that allows the user to
 * move (drag), scale (pinch-to-zoom), and rotate a clothing item
 * on the screen using multi-touch gestures.
 */
public class ResizableImageView extends AppCompatImageView {

    // Matrix to transform the image: move, scale, rotate
    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();

    // Three modes for touch events
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    // Last touch point position for DRAG mode
    private PointF lastPoint = new PointF();
    // Center point for ZOOM mode (pivot for scale/rotate)
    private PointF midPoint = new PointF();
    // Initial distance between two fingers for ZOOM scaling
    private float oldDist = 1f;
    // Initial rotation angle for ZOOM rotation
    private float oldAngle = 0f;

    // Constructors
    public ResizableImageView(Context context) {
        super(context);
        init();
    }

    public ResizableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ResizableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Set the image scale type to MATRIX so we can control the transformations
        setScaleType(ScaleType.MATRIX);
        // Ensure the view can receive touch events
        setClickable(true);
        // Use an inner class for touch event handling
        setOnTouchListener(new TouchListener());
    }

    /**
     * Resets the view's transformation matrix to identity.
     * Called when a new item is added or when an item is removed.
     */
    public void resetTransformation() {
        matrix.reset();
        savedMatrix.reset();
        setImageMatrix(matrix);
    }

    /**
     * Calculates the distance between two points.
     */
    private float calculateDistance(MotionEvent event) {
        if (event.getPointerCount() < 2) return 1f; // Cannot calculate distance with one finger
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * Calculates the midpoint between two points.
     */
    private void calculateMidPoint(PointF point, MotionEvent event) {
        if (event.getPointerCount() < 2) return;
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    /**
     * Calculates the angle between two points for rotation.
     */
    private float calculateRotation(MotionEvent event) {
        if (event.getPointerCount() < 2) return 0f;
        double delta_x = (event.getX(0) - event.getX(1));
        double delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    /**
     * Custom Touch Listener to handle multi-touch gestures.
     */
    private class TouchListener implements OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // Get the current touch action
            int action = event.getActionMasked();

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    // Primary finger down (Start DRAG mode)
                    savedMatrix.set(matrix);
                    lastPoint.set(event.getX(), event.getY());
                    mode = DRAG;
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:
                    // Secondary finger down (Start ZOOM mode)
                    oldDist = calculateDistance(event);
                    if (oldDist > 10f) { // Check if fingers are far enough apart to initiate zoom
                        savedMatrix.set(matrix);
                        calculateMidPoint(midPoint, event);
                        oldAngle = calculateRotation(event);
                        mode = ZOOM;
                        bringToFront(); // Bring selected item to the top when interacting
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG) {
                        // DRAG mode: Translate the image
                        matrix.set(savedMatrix);
                        float dx = event.getX() - lastPoint.x;
                        float dy = event.getY() - lastPoint.y;
                        matrix.postTranslate(dx, dy);

                    } else if (mode == ZOOM) {
                        // ZOOM mode: Scale and Rotate the image
                        float newDist = calculateDistance(event);
                        float newAngle = calculateRotation(event);

                        matrix.set(savedMatrix);

                        // 1. Scale
                        if (newDist > 10f) {
                            float scale = newDist / oldDist;
                            // Post-scale around the midpoint of the fingers
                            matrix.postScale(scale, scale, midPoint.x, midPoint.y);
                        }

                        // 2. Rotate
                        float rotation = newAngle - oldAngle;
                        // Post-rotate around the midpoint of the fingers
                        matrix.postRotate(rotation, midPoint.x, midPoint.y);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    // Finger lifted (End of drag or zoom)
                    mode = NONE;
                    // Note: No selection border logic is included in this version.
                    break;
            }

            // Apply the new matrix to the ImageView
            setImageMatrix(matrix);
            // Returning true consumes the event, preventing it from propagating to the parent
            return true;
        }
    }
}
