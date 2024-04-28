package com.example.falcon_strike_app;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

// Define GameHandler class, extends from Handler class
public class GameHandler extends Handler {
    private static final String TAG = "GameRenderHandler";

    private static final int MSG_DO_FRAME = 0;  // Define the message for handling frames
    private static final int MSG_SHUTDOWN = 1;  // Define the message for shutdown

    private final GameThread mGameThread;  // Define game thread

    // Define constructor
    public GameHandler(Looper looper, GameThread gameThread) {
        super(looper);  // Call parent's constructor
        this.mGameThread = gameThread;  // Initialize game thread
    }

    // Define method for sending frame handling message
    public void sendDoFrame(long frameTimeNanos) {
        sendMessage(obtainMessage(MSG_DO_FRAME, (int) (frameTimeNanos >> 32),
                (int) frameTimeNanos));
    }

    // Define method for sending shutdown message
    public void sendShutdown() {
        sendMessage(obtainMessage(MSG_SHUTDOWN));
    }

    // Override method for handling messages
    @Override
    public void handleMessage(@NonNull Message msg) {
        if (mGameThread == null) {
            Log.w(TAG, "Unable to process message: mRenderThread is null!");
            return;
        }

        int what = msg.what;
        switch (what) {
            case MSG_DO_FRAME:
                long timestamp = (((long) msg.arg1) << 32) | (((long) msg.arg2) & 0xffffffffL);
                mGameThread.doFrame(timestamp);  // Handle frame
                break;
            case MSG_SHUTDOWN:
                mGameThread.shutdown();  // Shutdown
                break;
            default:
                throw new RuntimeException("Unknown message " + what);
        }
    }
}