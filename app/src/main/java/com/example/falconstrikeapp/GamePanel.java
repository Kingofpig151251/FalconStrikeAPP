package com.example.falconstrikeapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GamePanel extends SurfaceView {

    private final Paint mPaint;
    private final ArrayList<Sprite> mSprites;
    private final Sprite mPlayer;
    private final ArrayList<Sprite> mEnemys;
    private final ArrayList<Sprite> mBullets;
    private Bitmap mBackgroundBitmap;
    private float mDisplayDensity;
    private int mState;
    private float mBackgroundY;
    private float mBackgroundSpeed;

    private Handler mHandler = new Handler();
    private Runnable mEnemySpawner = new Runnable() {
        @Override
        public void run() {
            if (mEnemys.size() < 10) {
                spawnEnemy(R.drawable.enemy_red, 150 + (float) (Math.random() * 50));
                int delay = 2000 + (int) (Math.random() * 2000);  // Generate a random delay between 2000 and 4000 milliseconds
                mHandler.postDelayed(this, delay);  // Schedule the next spawn with the random delay
            }
        }
    };

    public GamePanel(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);

        mPaint = new Paint();
        mPlayer = new AnimatedSprite(BitmapFactory.decodeResource(getResources(), R.drawable.player), 3, 6);
        mSprites = new ArrayList<>();
        mEnemys = new ArrayList<>();
        mBullets = new ArrayList<>();

        mState = 0;
        mBackgroundY = 0;
    }

    protected void start() {
        mDisplayDensity = getResources().getDisplayMetrics().density;
        mBackgroundSpeed = 50f * mDisplayDensity;

        mPlayer.setPosition(100f * mDisplayDensity, 100f * mDisplayDensity);
        mPlayer.setScale(mDisplayDensity / 2);
        mPlayer.setDraggable(true);
        mSprites.add(mPlayer);
        mHandler.post(mEnemySpawner);  // Start spawning enemies
    }

    protected void update(float deltaTime) {
        if (mBackgroundBitmap != null) {
            mBackgroundY += mBackgroundSpeed * deltaTime;
            if (mBackgroundY > mBackgroundBitmap.getHeight()) {
                mBackgroundY -= mBackgroundBitmap.getHeight();
            }
        }
        // 更新所有敵人的位置
        for (Sprite enemy : mEnemys) {
            enemy.move(deltaTime);

            // 如果敵人的位置超出了畫面的底部，將其位置重置到畫面的頂部
            if (enemy.getBounds().top > getHeight()) {
                float x = (float) Math.random() * (getWidth() - enemy.getBounds().width());
                float y = -enemy.getBounds().height();
                enemy.setPosition(x, y);
            }
        }
    }


    protected void render(@NonNull Canvas canvas) {
        drawBackgroundBitmap(mState, canvas);
        mPaint.setColor(Color.RED);
        mPaint.setTextSize(16f * mDisplayDensity);
        drawMultilineText(canvas, "Animated Sprite Sample", 16f * mDisplayDensity, 16f * mDisplayDensity, mPaint);

        for (Sprite sprite : mSprites) {
            if (sprite instanceof AnimatedSprite) {
                ((AnimatedSprite) sprite).handleAnimation();
            }
            sprite.render(canvas);
        }

    }

    protected void drawBackgroundBitmap(int mState, Canvas canvas) {
        switch (mState) {
            case 0:
                mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.water);
                break;
            case 1:
                mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.desert);
                break;
            case 2:
                mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.base);
                break;
        }

        if (mBackgroundBitmap != null) {
            int width = getWidth();
            int height = getHeight();
            for (int y = (int) mBackgroundY - mBackgroundBitmap.getHeight(); y < height; y += mBackgroundBitmap.getHeight()) {
                for (int x = 0; x < width; x += mBackgroundBitmap.getWidth()) {
                    canvas.drawBitmap(mBackgroundBitmap, x, y, null);
                }
            }
        }
    }

    protected void drawMultilineText(Canvas canvas, String text, float x, float y, Paint paint) {
        Rect textBounds = new Rect();
        float yOffset = 0f;

        for (String line : text.split("\n")) {
            paint.getTextBounds(line, 0, line.length(), textBounds);
            yOffset += textBounds.height();
            canvas.drawText(line, x, y + yOffset, mPaint);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        for (Sprite sprite : mSprites) {
            if (!sprite.isDraggable())
                continue;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (sprite.isTouched(event)) {
                        sprite.setDragging(true);
                        return true;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (sprite.isDragging()) {
                        sprite.setPosition(event.getX(), event.getY());
                    }
                    spawnBullet();
                    break;
                case MotionEvent.ACTION_UP:
                    if (sprite.isDragging())
                        sprite.setDragging(false);
                    break;
            }
        }
        return true;
    }

    public void spawnEnemy(int enemyDrawableId, float ySpeed) {
        // 創建敵人精靈
        AnimatedSprite enemySprite = new AnimatedSprite(BitmapFactory.decodeResource(getResources(), enemyDrawableId), 3, 6);

        // 計算敵人的初始位置
        float x = (float) Math.random() * (getWidth() - enemySprite.getBounds().width());
        float y = -enemySprite.getBounds().height();

        // 設置敵人的位置和速度
        enemySprite.setPosition(x, y);
        enemySprite.setSpeed(0, ySpeed * mDisplayDensity);

        // 將敵人添加到列表中
        mEnemys.add(enemySprite);
        mSprites.add(enemySprite);
    }

    public void spawnBullet() {
        // 創建子彈精靈
        AnimatedSprite bulletSprite = new AnimatedSprite(BitmapFactory.decodeResource(getResources(), R.drawable.bullet), 3, 6);

        // 計算子彈的初始位置
        float x = mPlayer.getBounds().centerX();
        float y = mPlayer.getBounds().top;

        // 設置子彈的位置和速度
        bulletSprite.setPosition(x, y);
        bulletSprite.setSpeed(0, -50f * mDisplayDensity);  // 假設子彈的速度為50dp/s

        // 將子彈添加到列表中
        mBullets.add(bulletSprite);
        mSprites.add(bulletSprite);
    }
}