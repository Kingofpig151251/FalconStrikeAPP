package com.example.falconstrikeapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

// 定義 GamePanel 類別，繼承自 SurfaceView 類別
public class GamePanel extends SurfaceView {

    private final Paint mPaint;  // 定義畫筆
    private final ArrayList<Sprite> mSprites;  // 定義精靈列表
    private Bitmap mBackgroundBitmap;  // 定義背景圖片
    private float mDisplayDensity;  // 定義顯示密度
    private int mState;  // 定義狀態
    private float mBackgroundY;  // 定義背景圖片的垂直位置
    private float mBackgroundSpeed;  // 定義背景圖片的滾動速度


    // 定義建構子
    public GamePanel(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);  // 調用父類的建構子
        setFocusable(true);  // 設置為可獲取焦點

        mPaint = new Paint();  // 初始化畫筆
        mSprites = new ArrayList<>();  // 初始化精靈列表

        mState = 0;  //

        mBackgroundY = 0;  // 初始化背景圖片的垂直位置
        mBackgroundSpeed = 50f * mDisplayDensity;  // 初始化背景圖片的滾動速度
    }


    // 覆寫觸摸事件方法
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
                    if (sprite.isDragging())
                        sprite.setPosition(event.getX(), event.getY());
                    break;
                case MotionEvent.ACTION_UP:
                    if (sprite.isDragging())
                        sprite.setDragging(false);
                    break;
            }
        }

        // 抑制默認行為
        return true;
    }

    // 定義開始遊戲的方法
    protected void start() {
        mDisplayDensity = getResources().getDisplayMetrics().density;

        for (int i = 1; i <= 2; i++) {
            AnimatedSprite playerSprite = new AnimatedSprite(BitmapFactory.decodeResource(getResources(), R.drawable.player), 3, 3);
            playerSprite.setPosition(100f * i * mDisplayDensity, 100f * i * mDisplayDensity);
            playerSprite.setScale(mDisplayDensity / 2);
            mSprites.add(playerSprite);
        }

        // 速度是基於時間的 (px/s)
        mSprites.get(0).setDraggable(true);
        mSprites.get(1).setSpeed(25f * mDisplayDensity, 50f * mDisplayDensity);

    }

    // 定義更新遊戲的方法
    protected void update(float deltaTime) {
        mBackgroundY += mBackgroundSpeed * deltaTime;
        if (mBackgroundY > getHeight()) {
            mBackgroundY -= mBackgroundBitmap.getHeight();
        }
        mSprites.get(1).move(deltaTime);
        mSprites.get(1).handleBounce(0, 0, getWidth(), getHeight());
    }

    // 定義渲染遊戲的方法
    protected void render(@NonNull Canvas canvas) {
        // 背景
        drawBackgroundBitmap(mState, canvas);

        // 文本
        mPaint.setColor(Color.RED);
        mPaint.setTextSize(16f * mDisplayDensity);
        drawMultilineText(canvas, "Animated Sprite Sample", 16f * mDisplayDensity, 16f * mDisplayDensity, mPaint);

        // 精靈
        for (Sprite sprite : mSprites) {
            if (sprite instanceof AnimatedSprite) {
                ((AnimatedSprite) sprite).handleAnimation();
            }
            sprite.render(canvas);
        }
    }

    // 定義繪製多行文本的方法
    protected void drawMultilineText(Canvas canvas, String text, float x, float y, Paint paint) {
        Rect textBounds = new Rect();
        float yOffset = 0f;

        for (String line : text.split("\n")) {
            paint.getTextBounds(line, 0, line.length(), textBounds);
            yOffset += textBounds.height();
            canvas.drawText(line, x, y + yOffset, mPaint);
        }
    }

    // 定義設置背景圖片的方法
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
            for (int y = (int) mBackgroundY; y < height; y += mBackgroundBitmap.getHeight()) {
                for (int x = 0; x < width; x += mBackgroundBitmap.getWidth()) {
                    canvas.drawBitmap(mBackgroundBitmap, x, y, null);
                }
            }
        }
    }
}