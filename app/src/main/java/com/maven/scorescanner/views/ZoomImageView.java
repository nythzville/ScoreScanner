package com.maven.scorescanner.views;

/**
 * Created by nathan on 5/25/2017.
 */

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class ZoomImageView extends android.support.v7.widget.AppCompatImageView {

// region . Static fields .

    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    static final int CLICK = 3;

// endregion . Static fields .

// region . Fields .

    private int mode = NONE;

    private Matrix mMatrix = new Matrix();

    private PointF mLastTouch = new PointF();
    private PointF mStartTouch = new PointF();
    private float minScale = 0.5f;
    private float maxScale = 4f;
    private float[] mCriticPoints;

    private float mScale = 1f;
    private float mRight;
    private float mBottom;
    private float mOriginalBitmapWidth;
    private float mOriginalBitmapHeight;

    private ScaleGestureDetector mScaleDetector;

//endregion . Fields .

    // region . Ctor .
    public ZoomImageView(Context context) {
        super(context);
        init(context);
    }

    public ZoomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

// endregion . Ctor .

// region . Overrider .

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int bmHeight = getBmHeight();
        int bmWidth = getBmWidth();

        float width = getMeasuredWidth();
        float height = getMeasuredHeight();
        float scale = 1;

        // If image is bigger then display fit it to screen.
        if (width < bmWidth || height < bmHeight) {
            scale = width > height ? height / bmHeight : width / bmWidth;
        }

        mMatrix.setScale(scale, scale);
        mScale = 1f;

        mOriginalBitmapWidth = scale * bmWidth;
        mOriginalBitmapHeight = scale * bmHeight;

        // Center the image
        float redundantYSpace = (height - mOriginalBitmapHeight);
        float redundantXSpace = (width - mOriginalBitmapWidth);

        mMatrix.postTranslate(redundantXSpace / 2, redundantYSpace / 2);

        setImageMatrix(mMatrix);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleDetector.onTouchEvent(event);

        mMatrix.getValues(mCriticPoints);
        float translateX = mCriticPoints[Matrix.MTRANS_X];
        float trnslateY = mCriticPoints[Matrix.MTRANS_Y];
        PointF currentPoint = new PointF(event.getX(), event.getY());

        switch (event.getAction()) {
            //when one finger is touching
            //set the mode to DRAG
            case MotionEvent.ACTION_DOWN:
                mLastTouch.set(event.getX(), event.getY());
                mStartTouch.set(mLastTouch);
                mode = DRAG;
                break;
            //when two fingers are touching
            //set the mode to ZOOM
            case MotionEvent.ACTION_POINTER_DOWN:
                mLastTouch.set(event.getX(), event.getY());
                mStartTouch.set(mLastTouch);
                mode = ZOOM;
                break;
            //when a finger moves
            //If mode is applicable move image
            case MotionEvent.ACTION_MOVE:

                //if the mode is ZOOM or
                //if the mode is DRAG and already zoomed
                if (mode == ZOOM || (mode == DRAG && mScale > minScale)) {

                    // region . Move  image.

                    float deltaX = currentPoint.x - mLastTouch.x;// x difference
                    float deltaY = currentPoint.y - mLastTouch.y;// y difference
                    float scaleWidth = Math.round(mOriginalBitmapWidth * mScale);// width after applying current scale
                    float scaleHeight = Math.round(mOriginalBitmapHeight * mScale);// height after applying current scale

                    // Move image to lef or right if its width is bigger than display width
                    if (scaleWidth > getWidth()) {
                        if (translateX + deltaX > 0) {
                            deltaX = -translateX;
                        } else if (translateX + deltaX < -mRight) {
                            deltaX = -(translateX + mRight);
                        }
                    } else {
                        deltaX = 0;
                    }
                    // Move image to up or bottom if its height is bigger than display height
                    if (scaleHeight > getHeight()) {
                        if (trnslateY + deltaY > 0) {
                            deltaY = -trnslateY;
                        } else if (trnslateY + deltaY < -mBottom) {
                            deltaY = -(trnslateY + mBottom);
                        }
                    } else {
                        deltaY = 0;
                    }

                    //move the image with the matrix
                    mMatrix.postTranslate(deltaX, deltaY);
                    //set the last touch location to the current
                    mLastTouch.set(currentPoint.x, currentPoint.y);

                    // endregion . Move image .
                }
                break;
            //first finger is lifted
            case MotionEvent.ACTION_UP:
                mode = NONE;
                int xDiff = (int) Math.abs(currentPoint.x - mStartTouch.x);
                int yDiff = (int) Math.abs(currentPoint.y - mStartTouch.y);
                if (xDiff < CLICK && yDiff < CLICK)
                    performClick();
                break;
            // second finger is lifted
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
        }
        setImageMatrix(mMatrix);
        invalidate();
        return true;
    }

//endregion . Overrides .

// region . Privates .

    private void init(Context context) {
        super.setClickable(true);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mCriticPoints = new float[9];
        setImageMatrix(mMatrix);
        setScaleType(ScaleType.MATRIX);
    }

    private int getBmWidth() {
        Drawable drawable = getDrawable();
        if (drawable != null) {
            return drawable.getIntrinsicWidth();
        }
        return 0;
    }

    private int getBmHeight() {
        Drawable drawable = getDrawable();
        if (drawable != null) {
            return drawable.getIntrinsicHeight();
        }
        return 0;
    }

//endregion . Privates .

// region . Internal classes .

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mode = ZOOM;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float newScale = mScale * scaleFactor;
            if (newScale < maxScale && newScale > minScale) {
                mScale = newScale;
                float width = getWidth();
                float height = getHeight();
                mRight = (mOriginalBitmapWidth * mScale) - width;
                mBottom = (mOriginalBitmapHeight * mScale) - height;

                float scaledBitmapWidth = mOriginalBitmapWidth * mScale;
                float scaledBitmapHeight = mOriginalBitmapHeight * mScale;

                if (scaledBitmapWidth <= width || scaledBitmapHeight <= height) {
                    mMatrix.postScale(scaleFactor, scaleFactor, width / 2, height / 2);
                } else {
                    mMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
                }
            }
            return true;
        }
    }

// endregion . Internal classes .
}