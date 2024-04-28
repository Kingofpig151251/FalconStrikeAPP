package com.example.falcon_strike_app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

public class AnimatedSprite extends Sprite {

    protected final int mNumFrames;  // Number of frames in the animation
    protected final long mFPS;  // Frame rate of the animation
    protected final List<Rect> mFrames;  // Stores the boundaries of the animation frames
    protected int mCurFrame;  // Current frame
    protected long mNextFrameTime;  // Time of the next frame

    private boolean isVisible = true;
    private Handler mHandler = new Handler(Looper.getMainLooper());

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
        if (!isVisible) {
            return;
        }
        Rect frame = mFrames.get(mCurFrame);
        canvas.drawBitmap(mBitmap, frame, getBounds(), mPaint);
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

    public void startBlinking(long blinkDuration, long blinkInterval) {
        mHandler.postDelayed(new Runnable() {
            long endTime = System.currentTimeMillis() + blinkDuration;

            @Override
            public void run() {
                isVisible = !isVisible;
                if (System.currentTimeMillis() < endTime) {
                    mHandler.postDelayed(this, blinkInterval);
                } else {
                    isVisible = true;
                }
            }
        }, blinkInterval);
    }
}