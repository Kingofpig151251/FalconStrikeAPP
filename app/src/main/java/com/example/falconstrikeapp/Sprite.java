package com.example.falconstrikeapp;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

public class Sprite {

    // 定義成員變量
    protected final Bitmap mBitmap;  // 用於儲存精靈的圖像
    protected final Paint mPaint;  // 用於繪製精靈的畫筆

    protected float mX, mY;  // 精靈的位置
    protected float mXSpeed, mYSpeed;  // 精靈的速度

    protected float mScale;  // 精靈的縮放比例
    protected boolean mDraggable, mDragging;  // 精靈是否可以拖動，以及是否正在被拖動


    // 定義建構子
    public Sprite(Bitmap bitmap) {
        mBitmap = bitmap;  // 初始化圖像
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);  // 初始化畫筆，並開啟抗鋸齒

        mX = mY = mXSpeed = mYSpeed = 0f;  // 初始化位置和速度

        mScale = 1;  // 初始化縮放比例
        mDraggable = mDragging = false;  // 初始化拖動狀態
    }

    // 定義移動方法
    public void move(float deltaTime) {
        mX += mXSpeed * deltaTime;  // 更新 X 位置
        mY += mYSpeed * deltaTime;  // 更新 Y 位置
    }

    // 定義碰撞處理方法
    public void handleBounce(int left, int top, int right, int bottom) {
        int halfWidth = (int) (getBounds().width() / 2f);
        if (mX < left + halfWidth || mX > right - halfWidth)
            mXSpeed *= -1;  // 如果碰到左右邊界，反轉 X 方向速度

        int halfHeight = (int) (getBounds().height() / 2f);
        if (mY < top + halfHeight || mY > bottom - halfHeight)
            mYSpeed *= -1;  // 如果碰到上下邊界，反轉 Y 方向速度
    }

    // 定義繪製方法
    public void render(Canvas canvas) {
        canvas.drawBitmap(mBitmap, null, getBounds(), mPaint);  // 在畫布上繪製精靈
    }

    // 定義碰撞檢測方法
    public boolean collideWith(Sprite other) {
        return RectF.intersects(this.getBounds(), other.getBounds());  // 檢查此精靈是否與其他精靈碰撞
    }

    // 定義觸摸檢測方法
    public boolean isTouched(MotionEvent event) {
        return mDraggable && getBounds().contains(event.getX(), event.getY());  // 檢查此精靈是否被觸摸
    }

    // 定義獲取邊界方法
    public RectF getBounds() {
        int bitmapWidth = mBitmap.getWidth();
        int bitmapHeight = mBitmap.getHeight();

        RectF bounds = new RectF(
                mX - bitmapWidth / 2f,
                mY - bitmapHeight / 2f,
                mX + bitmapWidth / 2f,
                mY + bitmapHeight / 2f);  // 計算精靈的邊界

        // 如果有縮放，則調整邊界
        if (mScale != 1) {
            float diffHorizontal = (bounds.right - bounds.left) * (mScale - 1f);
            float diffVertical = (bounds.bottom - bounds.top) * (mScale - 1f);

            bounds.top -= diffVertical / 2f;
            bounds.left -= diffHorizontal / 2f;
            bounds.right += diffHorizontal / 2f;
            bounds.bottom += diffVertical / 2f;
        }

        return bounds;  // 返回邊界
    }

    // 定義獲取和設置拖動狀態的方法
    public boolean isDraggable() {
        return mDraggable;
    }

    public boolean isDragging() {
        return mDraggable && mDragging;
    }

    public void setDraggable(boolean state) {
        mDraggable = state;
    }

    public void setDragging(boolean state) {
        mDragging = state;
    }

    // 定義設置位置的方法
    public void setPosition(float x, float y) {
        // 獲取畫面的寬度和高度
        int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;

        // 獲取精靈的寬度和高度
        float spriteWidth = getBounds().width();
        float spriteHeight = getBounds().height();

        // 檢查並調整 x 和 y 的值，以確保精靈不會超出畫面邊界
        if (x - spriteWidth / 2 < 0) {
            x = spriteWidth / 2;
        } else if (x + spriteWidth / 2 > screenWidth) {
            x = screenWidth - spriteWidth / 2;
        }

        if (y - spriteHeight / 2 < 0) {
            y = spriteHeight / 2;
        } else if (y + spriteHeight / 2 > screenHeight) {
            y = screenHeight - spriteHeight / 2;
        }

        // 更新精靈的位置
        mX = x;
        mY = y;
    }

    // 定義設置縮放比例的方法
    public void setScale(float scale) {
        mScale = scale;
    }

    // 定義設置速度的方法
    public void setSpeed(float xSpeed, float ySpeed) {
        mXSpeed = xSpeed;
        mYSpeed = ySpeed;
    }

    public float getX() {
        return mX;
    }

    public float getY() {
        return mY;
    }
}