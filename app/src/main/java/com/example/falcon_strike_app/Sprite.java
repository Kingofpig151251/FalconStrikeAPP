package com.example.falcon_strike_app;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

public class Sprite {

    // Member variables
    protected final Bitmap mBitmap;  // Bitmap for storing sprite image
    protected final Paint mPaint;  // Paint for drawing sprite

    protected float mX, mY;  // Sprite's position
    protected float mXSpeed, mYSpeed;  // Sprite's speed

    protected float mScale;  // Sprite's scale ratio
    protected boolean mDraggable, mDragging;  // Whether the sprite can be dragged, and whether it is being dragged


    // Constructor
    public Sprite(Bitmap bitmap) {
        mBitmap = bitmap;  // Initialize image
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);  // Initialize paint and enable anti-aliasing

        mX = mY = mXSpeed = mYSpeed = 0f;  // Initialize position and speed

        mScale = 1;  // Initialize scale ratio
        mDraggable = mDragging = false;  // Initialize dragging state
    }

    // Move method
    public void move(float deltaTime) {
        mX += mXSpeed * deltaTime;  // Update X position
        mY += mYSpeed * deltaTime;  // Update Y position
    }

    // Collision handling method
    public void handleBounce(int left, int top, int right, int bottom) {
        int halfWidth = (int) (getBounds().width() / 2f);
        if (mX < left + halfWidth || mX > right - halfWidth)
            mXSpeed *= -1;  // If it hits the left or right boundary, reverse the X direction speed

        int halfHeight = (int) (getBounds().height() / 2f);
        if (mY < top + halfHeight || mY > bottom - halfHeight)
            mYSpeed *= -1;  // If it hits the top or bottom boundary, reverse the Y direction speed
    }

    // Render method
    public void render(Canvas canvas) {
        canvas.drawBitmap(mBitmap, null, getBounds(), mPaint);  // Draw sprite on canvas
    }

    // Collision detection method
    public boolean collideWith(Sprite other) {
        return RectF.intersects(this.getBounds(), other.getBounds());  // Check if this sprite collides with other sprite
    }

    // Touch detection method
    public boolean isTouched(MotionEvent event) {
        return mDraggable && getBounds().contains(event.getX(), event.getY());  // Check if this sprite is touched
    }

    // Get bounds method
    public RectF getBounds() {
        int bitmapWidth = mBitmap.getWidth();
        int bitmapHeight = mBitmap.getHeight();

        RectF bounds = new RectF(
                mX - bitmapWidth / 2f,
                mY - bitmapHeight / 2f,
                mX + bitmapWidth / 2f,
                mY + bitmapHeight / 2f);  // Calculate sprite's bounds

        // If there is a scale, adjust the bounds
        if (mScale != 1) {
            float diffHorizontal = (bounds.right - bounds.left) * (mScale - 1f);
            float diffVertical = (bounds.bottom - bounds.top) * (mScale - 1f);

            bounds.top -= diffVertical / 2f;
            bounds.left -= diffHorizontal / 2f;
            bounds.right += diffHorizontal / 2f;
            bounds.bottom += diffVertical / 2f;
        }

        return bounds;  // Return bounds
    }

    // Get and set dragging state methods
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

    // Set position method
    public void setPosition(float x, float y) {
        // Get screen width and height
        int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;

        // Get sprite width and height
        float spriteWidth = getBounds().width();
        float spriteHeight = getBounds().height();

        // Check and adjust x and y values to ensure sprite does not exceed screen boundaries
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

        // Update sprite's position
        mX = x;
        mY = y;
    }

    // Set scale ratio method
    public void setScale(float scale) {
        mScale = scale;
    }

    // Set speed method
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