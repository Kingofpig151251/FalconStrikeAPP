package com.example.falconstrikeapp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;

public class AnimatedSprite extends Sprite {

    protected final int mNumFrames;  // 動畫的幀數
    protected final long mFPS;  // 動畫的幀率
    protected final List<Rect> mFrames;  // 儲存動畫幀的邊界
    protected int mCurFrame;  // 當前的幀
    protected long mNextFrameTime;  // 下一幀的時間

    protected SpriteType mType;

    public AnimatedSprite(Bitmap bitmap, int numFrames, int framesPerSecond, SpriteType type) {
        super(bitmap);
        mNumFrames = numFrames;
        mFPS = (long) (1 / (double) framesPerSecond * 1000);
        mFrames = new ArrayList<>();
        mType = type;
        int bitmapWidth = mBitmap.getWidth() / mNumFrames;
        int bitmapHeight = mBitmap.getHeight();

        for (int i = 0; i < mNumFrames; i++) {
            mFrames.add(new Rect(
                    i * bitmapWidth,
                    0,
                    (i + 1) * bitmapWidth,
                    bitmapHeight
            ));
        }
    }


    public SpriteType getType() {
        return mType;
    }

    public void handleAnimation() {
        if (mNextFrameTime > System.currentTimeMillis())
            return;

        mCurFrame = (mCurFrame + 1) % mNumFrames;
        mNextFrameTime = System.currentTimeMillis() + mFPS;
    }

    @Override
    public void render(Canvas canvas) {
        Rect frame = mFrames.get(mCurFrame);
        canvas.drawBitmap(mBitmap, frame, getBounds(), mPaint);

        // 繪製碰撞體積的邊界
        Paint boundsPaint = new Paint();
        boundsPaint.setColor(Color.RED);
        boundsPaint.setStyle(Paint.Style.STROKE);
        boundsPaint.setStrokeWidth(2);
        canvas.drawRect(getBounds(), boundsPaint);
    }

    @Override
    public RectF getBounds() {
        int bitmapWidth = mBitmap.getWidth() / mNumFrames;
        int bitmapHeight = mBitmap.getHeight();

        RectF bounds = new RectF(
                mX - bitmapWidth / 2f,
                mY - bitmapHeight / 2f,
                mX + bitmapWidth / 2f,
                mY + bitmapHeight / 2f);

        if (mScale != 1) {
            float diffHorizontal = (bounds.right - bounds.left) * (mScale - 1f);
            float diffVertical = (bounds.bottom - bounds.top) * (mScale - 1f);

            bounds.top -= diffVertical / 2f;
            bounds.left -= diffHorizontal / 2f;
            bounds.right += diffHorizontal / 2f;
            bounds.bottom += diffVertical / 2f;
        }

        return bounds;
    }
}