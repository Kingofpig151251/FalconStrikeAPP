package com.example.falconstrikeapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.Choreographer;
import android.view.SurfaceHolder;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

// 定義 MainActivity 類別，繼承自 AppCompatActivity 類別，並實現 SurfaceHolder.Callback 和 Choreographer.FrameCallback 接口
public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback,
        Choreographer.FrameCallback {

    private GamePanel mGamePanel;  // 定義遊戲面板
    private GameThread mGameThread;  // 定義遊戲線程

    // 覆寫 onCreate 方法
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGamePanel = findViewById(R.id.gamePanel);  // 初始化遊戲面板
        mGamePanel.getHolder().addCallback(this);  // 為遊戲面板的 Holder 添加回調
    }

    // 覆寫 onResume 方法
    @Override
    protected void onResume() {
        super.onResume();
        if (mGameThread != null) {
            Choreographer.getInstance().postFrameCallback(this);  // 為 Choreographer 添加幀回調
        }
    }

    // 覆寫 onPause 方法
    @Override
    protected void onPause() {
        super.onPause();
        Choreographer.getInstance().removeFrameCallback(this);  // 為 Choreographer 移除幀回調
    }

    // 實現 SurfaceHolder.Callback 接口的 surfaceCreated 方法
    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        mGamePanel.start();  // 開始遊戲

        mGameThread = new GameThread(mGamePanel);  // 初始化遊戲線程
        mGameThread.start();  // 開始遊戲線程
        mGameThread.waitUntilReady();  // 等待遊戲線程準備好

        Choreographer.getInstance().postFrameCallback(this);  // 為 Choreographer 添加幀回調
    }

    // 實現 SurfaceHolder.Callback 接口的 surfaceChanged 方法
    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
    }

    // 實現 SurfaceHolder.Callback 接口的 surfaceDestroyed 方法
    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        GameHandler handler = mGameThread.getHandler();  // 獲取遊戲線程的 Handler
        if (handler != null) {
            handler.sendShutdown();  // 發送關閉消息
            try {
                mGameThread.join();  // 等待遊戲線程結束
            } catch (InterruptedException ie) {
                throw new RuntimeException("GameThread join() interrupted", ie);
            }
        }
        mGameThread = null;  // 將遊戲線程設為 null
    }

    // 實現 Choreographer.FrameCallback 接口的 doFrame 方法
    @Override
    public void doFrame(long frameTimeNanos) {
        GameHandler handler = mGameThread.getHandler();  // 獲取遊戲線程的 Handler
        if (handler != null) {
            Choreographer.getInstance().postFrameCallback(this);  // 為 Choreographer 添加幀回調
            handler.sendDoFrame(frameTimeNanos);  // 發送處理幀的消息
        }
    }
}