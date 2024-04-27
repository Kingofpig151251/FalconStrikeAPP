package com.example.falconstrikeapp;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

// 定義 GameHandler 類別，繼承自 Handler 類別
public class GameHandler extends Handler {
    private static final String TAG = "GameRenderHandler";

    private static final int MSG_DO_FRAME = 0;  // 定義處理幀的消息
    private static final int MSG_SHUTDOWN = 1;  // 定義關閉的消息

    private final GameThread mGameThread;  // 定義遊戲線程

    // 定義建構子
    public GameHandler(Looper looper, GameThread gameThread) {
        super(looper);  // 調用父類的建構子
        this.mGameThread = gameThread;  // 初始化遊戲線程
    }

    // 定義發送處理幀的消息的方法
    public void sendDoFrame(long frameTimeNanos) {
        sendMessage(obtainMessage(MSG_DO_FRAME, (int) (frameTimeNanos >> 32),
                (int) frameTimeNanos));
    }

    // 定義發送關閉的消息的方法
    public void sendShutdown() {
        sendMessage(obtainMessage(MSG_SHUTDOWN));
    }

    // 覆寫處理消息的方法
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
                mGameThread.doFrame(timestamp);  // 處理幀
                break;
            case MSG_SHUTDOWN:
                mGameThread.shutdown();  // 關閉
                break;
            default:
                throw new RuntimeException("Unknown message " + what);
        }
    }
}