package com.example.imageviewbigsmall.zoomImageView;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * Created by Administrator on 2016/11/26.
 */

public class ZoomImageView extends ImageView implements ViewTreeObserver.OnGlobalLayoutListener, ScaleGestureDetector.OnScaleGestureListener, View.OnTouchListener {
    private boolean mOnce = false;
    //初始化时缩放的值
    private float mInitScale;
    //双击放大时缩放的值
    private float mMidScale;
    //放大的极限
    private float mMaxScale;
    private Matrix mScaleMatrix;

    //多点触控
    private ScaleGestureDetector mScaleGestureDetector;
    //-------------自由移动
    private int mLastPointerCount;
    private float mLastX;
    private float mLastY;
    private int mTouchSlop;
    private boolean isCanDrag;
    private boolean isCheckLeftAndRight;
    private boolean isCheckTopAndBottom;


    public ZoomImageView(Context context) {
        this(context, null);
    }

    public ZoomImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScaleMatrix = new Matrix();
        setScaleType(ScaleType.MATRIX);
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        setOnTouchListener(this);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeGlobalOnLayoutListener(this);
    }

    @Override
    public void onGlobalLayout() {
        Log.i("图片", "chuxian");
        if (!mOnce) {
            //得到控件的宽和高
            int width = getWidth();

            int height = getHeight();
            Log.i("图片", width + "控件宽，高" + height);
            //得到我们的图片以及宽和高
            Drawable d = getDrawable();
            if (d == null)
                return;
            int dw = d.getIntrinsicWidth();
            int dh = d.getIntrinsicHeight();
            Log.i("图片", dw + "宽，高" + dh);
            float scale = 1.0f;
            if (dw > width && dh < height) {//图片很宽但是高度很小
                scale = width * 1.0f / dw;
            }
            if (dh > height && dw < width) {
                scale = height * 1.0f / dh;
            }
            if (dw > width && dh > height) {
                scale = Math.max(width * 1.0f / dw, height * 1.0f / dh);
            }
            if (dw < width && dh < height) {//图片的宽高都小
                scale = Math.min(width * 1.0f / dw, height * 1.0f / dh);
            }
            mInitScale = scale;//初始时缩放的值
            mMaxScale = mInitScale * 4;//缩放极限值
            mMidScale = mInitScale * 2;

            int dx = getWidth() / 2 - dw / 2;
            int dy = getHeight() / 2 - dh / 2;
            mScaleMatrix.postTranslate(dx, dy);//平移
            mScaleMatrix.postScale(mInitScale, mInitScale, width / 2, height / 2);
            setImageMatrix(mScaleMatrix);
            mOnce = true;
        }
    }

    /**
     * 获取当前图片的缩放值
     *
     * @return
     */

    public float getScale() {
        float[] values = new float[9];
        mScaleMatrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scale = getScale();
        Log.i("图片",scale+"scale");
        float scaleFactor = detector.getScaleFactor();//从前一个伸缩事件至当前伸缩事件的伸缩比率。
        Log.i("图片",scaleFactor+"scaleFactor");
        if (getDrawable() == null)
            return true;
        if ((scale < mMaxScale && scaleFactor > 1.0f) || (scale > mInitScale && scaleFactor < 1.0f)) {
            if (scale * scaleFactor < mInitScale) {
                scaleFactor = mInitScale / scale;
                Log.i("图片",scaleFactor+"scaleFactor最小");
            }
            if (scale * scaleFactor > mMaxScale) {
                scaleFactor = mMaxScale / scale;
                Log.i("图片",scaleFactor+"scaleFactor最大");
            }
            Log.i("图片",scaleFactor+"scaleFactor取值");
            //缩放
            mScaleMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            checkBoardAndCenterWhenScale();
            setImageMatrix(mScaleMatrix);
        }


        return true;
    }

    /**
     * 获得图片放大缩小后的宽和高
     *
     * @return
     */
    private RectF getMatrixRectF() {
        Matrix mMatrix = mScaleMatrix;
        RectF rectF = new RectF();
        Drawable d = getDrawable();
        if (d != null) {
            rectF.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            mMatrix.mapRect(rectF);
        }
        return rectF;
    }

    /**
     * 当缩放时检测
     */
    private void checkBoardAndCenterWhenScale() {
        RectF rect = getMatrixRectF();
        float delatX = 0;
        float delatY = 0;

        int width = getWidth();
        int height = getHeight();
        if (rect.width() >= width) {
            if (rect.left > 0) {
                delatX = -rect.left;
            }
            if (rect.right < width) {
                delatX = width - rect.right;
            }
        }
        if (rect.height() >= height) {
            if (rect.top > 0) {
                delatY = -rect.top;
            }
            if (rect.bottom < height) {
                delatY = height - rect.bottom;
            }
        }
        //如果图片的宽度或者高度小于空间的 宽高，让其居中
        if (rect.width() < width) {
            delatX = width / 2f - rect.right + rect.width() / 2f;
        }
        if (rect.height() < height) {
            delatY = height / 2f - rect.bottom + rect.height() / 2f;
        }
        mScaleMatrix.postTranslate(delatX, delatY);
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mScaleGestureDetector.onTouchEvent(event);
        float x = 0;
        float y = 0;
        int pointCount = event.getPointerCount();
        for (int i = 0; i < pointCount; i++) {
            x += event.getX(i);
            y += event.getY(i);
        }

        x /= pointCount;
        y /= pointCount;

        if (mLastPointerCount != pointCount) {
            isCanDrag = false;
            mLastX = x;
            mLastY = y;
        }
        mLastPointerCount = pointCount;
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float dx = x - mLastX;
                float dy = y - mLastY;
                if (!isCanDrag) {
                    isCanDrag = isMoveAction(dx, dy);
                }
                if (isCanDrag) {
                    RectF rectF = getMatrixRectF();
                    if (getDrawable() != null) {
                        isCheckLeftAndRight = isCheckTopAndBottom = true;
                        //如果宽度小于控件宽度不允许横向移动
                        if (rectF.width() < getWidth()) {
                            isCheckLeftAndRight = false;
                            dx = 0;
                        }
                        if (rectF.height() < getHeight()) {
                            isCheckTopAndBottom = false;
                            dy = 0;
                        }
                        mScaleMatrix.postTranslate(dx, dy);
                        checkBoardWhenTranslate();
                        setImageMatrix(mScaleMatrix);
                    }
                }
                mLastX = x;
                mLastY = y;
                break;

            case MotionEvent.ACTION_UP:
                mLastPointerCount = 0;
                break;
        }
        return true;
    }

    /**
     * 当移动时进行边界检查
     */
    private void checkBoardWhenTranslate() {
        RectF rectF = getMatrixRectF();
        float deltaX = 0;
        float deltaY = 0;
        int width = getWidth();
        int height = getHeight();
        if (rectF.top > 0 && isCheckTopAndBottom) {
            deltaY = -rectF.top;
        }
        if (rectF.bottom < height && isCheckTopAndBottom) {
            deltaY = height - rectF.bottom;
        }
        if (rectF.left > 0 && isCheckLeftAndRight) {
            deltaX = -rectF.left;
        }
        if (rectF.right < width && isCheckLeftAndRight) {
            deltaX = width - rectF.right;
        }
        mScaleMatrix.postTranslate(deltaX, deltaY);


    }

    private boolean isMoveAction(float dx, float dy) {

        return Math.sqrt(dx * dx + dy * dy) > mTouchSlop;
    }
}
