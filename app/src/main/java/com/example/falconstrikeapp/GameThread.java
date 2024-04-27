package com.example.falconstrikeapp;

import android.graphics.Canvas;
import android.os.Looper;
import android.util.Log;

public class GameThread extends Thread {
    private static final String TAG = "GameThread";
    private static final long ONE_SECOND_NANOS = 1000000000L;
    private static final float ONE_BILLION_FLOAT = 1000000000.0f;
    private static final long TARGET_FRAME_RATE = ONE_SECOND_NANOS / 60 * ONE_SECOND_NANOS;

    private final GamePanel mGamePanel;
    private final Object mStartLock;

    private volatile GameHandler mHandler;
    private boolean mReady;

    private long mPrevTimeNanos;

    public GameThread(GamePanel gamePanel) {
        this.mGamePanel = gamePanel;
        this.mStartLock = new Object();
    }

    public void waitUntilReady() {
        synchronized (mStartLock) {
            while (!mReady) {
                try {
                    mStartLock.wait();
                } catch (InterruptedException ie) { /* not expected */ }
            }
        }
    }

    public void doFrame(long timeStampNanos) {
        // Figure out time delta based on the timestamps
        long intervalNanos;
        if (mPrevTimeNanos == 0) {
            intervalNanos = 0;
        } else {
            intervalNanos = timeStampNanos - mPrevTimeNanos;
            if (intervalNanos > ONE_SECOND_NANOS) {
                intervalNanos = 0;
            }
        }
        mPrevTimeNanos = timeStampNanos;
        mGamePanel.update(intervalNanos / ONE_BILLION_FLOAT);

        // If we spent too much time updating, skip a frame
        long deltaTimeStamp = System.nanoTime() - timeStampNanos;
        if (deltaTimeStamp > TARGET_FRAME_RATE) {
            Log.d(TAG, "Last frame spent " + deltaTimeStamp + "us, skipping render");
            return;
        }

        // Lock the canvas and call render(Canvas)
        Canvas canvas = mGamePanel.getHolder().lockCanvas();
        if (canvas == null) {
            Log.d(TAG, "Unable to lock canvas, skipping render");
            return;
        }
        try {
            synchronized (mGamePanel.getHolder()) {
                mGamePanel.render(canvas);
            }
        } finally {
            mGamePanel.getHolder().unlockCanvasAndPost(canvas);
        }
    }

    public void shutdown() {
        Looper.myLooper().quit();
    }

    public GameHandler getHandler() {
        return mHandler;
    }

    @Override
    public void run() {
        Looper.prepare();
        mHandler = new GameHandler(Looper.myLooper(), this);
        synchronized (mStartLock) {
            mReady = true;
            mStartLock.notify();
        }

        Looper.loop();

        synchronized (mStartLock) {
            mReady = false;
        }
    }
}
