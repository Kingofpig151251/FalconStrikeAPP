package com.example.falcon_strike_app;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Choreographer;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.falconstrikeapp.R;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback,
        Choreographer.FrameCallback {

    private GamePanel mGamePanel;  // Define game panel
    private GameThread mGameThread;  // Define game thread

    private MediaPlayer mMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGamePanel = findViewById(R.id.gamePanel);  // Initialize game panel
        mGamePanel.getHolder().addCallback(this);  // Add callback to the holder of the game panel

        // Create MediaPlayer and start playing music
        mMediaPlayer = MediaPlayer.create(this, R.raw.bgm);
        mMediaPlayer.setLooping(true);  // Set music to loop
        mMediaPlayer.start();
    }

    // Override onResume method
    @Override
    protected void onResume() {
        super.onResume();
        if (mMediaPlayer != null) {
            mMediaPlayer.start();  // Continue playing music
        }
        if (mGameThread != null) {
            Choreographer.getInstance().postFrameCallback(this);  // Add frame callback to Choreographer
        }
    }

    // Override onPause method
    @Override
    protected void onPause() {
        super.onPause();
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();  // Pause music
        }
        Choreographer.getInstance().removeFrameCallback(this);  // Remove frame callback from Choreographer
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            mMediaPlayer.release();  // Release MediaPlayer
            mMediaPlayer = null;
        }
    }

    // Implement surfaceCreated method of SurfaceHolder.Callback interface
    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        mGamePanel.start();  // Start game

        mGameThread = new GameThread(mGamePanel);  // Initialize game thread
        mGameThread.start();  // Start game thread
        mGameThread.waitUntilReady();  // Wait until game thread is ready

        Choreographer.getInstance().postFrameCallback(this);  // Add frame callback to Choreographer
    }

    // Implement surfaceChanged method of SurfaceHolder.Callback interface
    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
    }

    // Implement surfaceDestroyed method of SurfaceHolder.Callback interface
    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        GameHandler handler = mGameThread.getHandler();  // Get handler of game thread
        if (handler != null) {
            handler.sendShutdown();  // Send shutdown message
            try {
                mGameThread.join();  // Wait for game thread to end
            } catch (InterruptedException ie) {
                throw new RuntimeException("GameThread join() interrupted", ie);
            }
        }
        mGameThread = null;  // Set game thread to null
    }

    // Implement doFrame method of Choreographer.FrameCallback interface
    @Override
    public void doFrame(long frameTimeNanos) {
        GameHandler handler = mGameThread.getHandler();  // Get handler of game thread
        if (handler != null) {
            Choreographer.getInstance().postFrameCallback(this);  // Add frame callback to Choreographer
            handler.sendDoFrame(frameTimeNanos);  // Send message to handle frame
        }
    }

    public void gameOver(int score) {
        Intent extra = new Intent();
        extra.putExtra("score", score);
        setResult(RESULT_OK, extra);
        finish();
    }
}