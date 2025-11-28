package com.example.forhealth.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.content.res.TypedArray;
import com.journeyapps.barcodescanner.ViewfinderView;
import com.journeyapps.barcodescanner.Size;
import com.example.forhealth.R;

public class CustomViewfinderView extends ViewfinderView {

    public static final long INT_ANIMATION_DELAY = 12;

    // 边角线相关属性
    public float mLineRate = 0.1F; // 边角线的比例
    public float mLineDepth = dp2px(4); // 边角线的厚度
    public int mLineColor; // 边角线颜色

    // 扫描线相关属性
    public int mScanLinePosition = 0; // 扫描线的位置
    public float mScanLineDepth = dp2px(4); // 扫描线的厚度
    public float mScanLineDy = dp2px(3); // 扫描线每次移动的距离
    public LinearGradient mLinearGradient; // 渐变线
    public Paint mBitmapPaint; // 绘制图形的 Paint 对象

    // 扫描框宽高
    public float mScanFrameWidth;
    public float mScanFrameHeight;

    // 扫描线颜色数组
    public int[] mScanLineColor = new int[]{0x00000000, Color.YELLOW, 0x00000000};

    public CustomViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 从 XML 获取自定义属性
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CustomViewfinderView);
        mLineColor = typedArray.getColor(R.styleable.CustomViewfinderView_lineColor, Color.YELLOW);
        mScanLineColor[1] = typedArray.getColor(R.styleable.CustomViewfinderView_cornerColor, Color.YELLOW);
        mScanFrameWidth = typedArray.getDimension(R.styleable.CustomViewfinderView_scanFrameWidth, dp2px(160));
        mScanFrameHeight = typedArray.getDimension(R.styleable.CustomViewfinderView_scanFrameHeight, dp2px(160));
        typedArray.recycle();

        mBitmapPaint = new Paint();
        mBitmapPaint.setAntiAlias(true);
    }

    @Override
    public void onDraw(Canvas canvas) {
        refreshSizes();
        if (framingRect == null || previewSize == null) {
            return;
        }

        final Rect frame = framingRect;
        final int width = getWidth();
        final int height = getHeight();

        // 绘制扫描框外部遮罩
        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);

        // 绘制四个角
        paint.setColor(mLineColor);
        canvas.drawRect(frame.left, frame.top, frame.left + frame.width() * mLineRate, frame.top + mLineDepth, paint);
        canvas.drawRect(frame.left, frame.top, frame.left + mLineDepth, frame.top + frame.height() * mLineRate, paint);

        canvas.drawRect(frame.right - frame.width() * mLineRate, frame.top, frame.right, frame.top + mLineDepth, paint);
        canvas.drawRect(frame.right - mLineDepth, frame.top, frame.right, frame.top + frame.height() * mLineRate, paint);

        canvas.drawRect(frame.left, frame.bottom - mLineDepth, frame.left + frame.width() * mLineRate, frame.bottom, paint);
        canvas.drawRect(frame.left, frame.bottom - frame.height() * mLineRate, frame.left + mLineDepth, frame.bottom, paint);

        canvas.drawRect(frame.right - frame.width() * mLineRate, frame.bottom - mLineDepth, frame.right, frame.bottom, paint);
        canvas.drawRect(frame.right - mLineDepth, frame.bottom - frame.height() * mLineRate, frame.right, frame.bottom, paint);

        // 绘制扫描线
        if (resultBitmap != null) {
            paint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(resultBitmap, null, frame, paint);
        } else {
            mScanLinePosition += mScanLineDy;
            if (mScanLinePosition >= frame.height()) {
                mScanLinePosition = 0;
            }
            mLinearGradient = new LinearGradient(frame.left, frame.top + mScanLinePosition, frame.right, frame.top + mScanLinePosition, mScanLineColor, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);
            paint.setShader(mLinearGradient);
            canvas.drawRect(frame.left, frame.top + mScanLinePosition, frame.right, frame.top + mScanLinePosition + mScanLineDepth, paint);
            paint.setShader(null);
        }

        // 定时刷新扫描框
        postInvalidateDelayed(INT_ANIMATION_DELAY,
                frame.left - POINT_SIZE,
                frame.top - POINT_SIZE,
                frame.right + POINT_SIZE,
                frame.bottom + POINT_SIZE);
    }

    protected void refreshSizes() {
        if (cameraPreview == null) {
            return;
        }
        cameraPreview.setFramingRectSize(new Size((int) mScanFrameWidth, (int) mScanFrameHeight));

        Rect framingRect = cameraPreview.getFramingRect();
        Size previewSize = cameraPreview.getPreviewSize();
        if (framingRect != null && previewSize != null) {
            this.framingRect = framingRect;
            this.previewSize = previewSize;
        }
    }

    private int dp2px(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
}
