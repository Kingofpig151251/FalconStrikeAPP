package com.example.falcon_strike_app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.falconstrikeapp.R;

import java.util.concurrent.CopyOnWriteArrayList;

public class GamePanel extends SurfaceView implements CollisionListener {

    public enum BitmapType {
        PLAYER,
        ENEMY_BLUE,
        ENEMY_GREEN,
        ENEMY_RED,
        BULLET,
        EXPLOSION,
        BACKGROUND_WATER,
        BACKGROUND_DESERT,
        BACKGROUND_BASE,
    }

    private Bitmap[] mBitmaps;
    private int mNextLevelScore;

    private AnimatedSprite mDraggingSprite;  // The sprite currently being dragged

    private static final int MAX_ENEMIES = 10;  // Maximum number of enemies
    private static final float BULLET_SPEED = -300f;  // Bullet speed
    private static final long BULLET_INTERVAL = 500;  // Bullet interval
    private static final long PLAYER_INVINCIBLE_TIME = 3000;  // Player invincible time
    private long mLastHitTime;  // Last hit time
    private final Paint mPaint;  // Paint object
    private final CopyOnWriteArrayList<AnimatedSprite> mSprites;  // List of sprites
    private final AnimatedSprite mPlayer;  // Player sprite
    private final CopyOnWriteArrayList<AnimatedSprite> mEnemy;  // List of enemy sprites
    private final CopyOnWriteArrayList<AnimatedSprite> mBullets;  // List of bullet sprites

    private CollisionThread collisionThread;  // Collision thread

    private Bitmap mBackgroundBitmap;  // Background bitmap
    private float mDisplayDensity;  // Display density
    private int mLevel;  // Level
    private float mBackgroundY;  // Background Y position
    private float mBackgroundSpeed;  // Background speed
    private long mLastBulletTime;  // Last bullet time

    private int mPlayerHP = 3;  // Player HP

    private int mScore = 0;  // Score
    private boolean mIsGameOver = false;  // Is game over flag
    private boolean mIsGameWin = false;  // Is game win flag
    private boolean mIsGameStart = false;  // Is game start flag

    private final SoundPool mSoundPool;  // Sound pool
    private final int mExplosionSoundId;  // Explosion sound ID

    private final Handler mHandler = new Handler();  // Handler
    // Activate an enemy every 1 second
    private final Runnable mActivateEnemyTask = new Runnable() {
        @Override
        public void run() {
            if (mIsGameOver || mIsGameWin) return;
            if (mEnemy.size() < MAX_ENEMIES) {
                if (mIsGameStart) spawnEnemy();
            }
            mHandler.postDelayed(this, 2000 / mLevel);
        }
    };

    public GamePanel(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        mBitmaps = new Bitmap[BitmapType.values().length];
        mBitmaps[BitmapType.PLAYER.ordinal()] = BitmapFactory.decodeResource(getResources(), R.drawable.player);
        mBitmaps[BitmapType.ENEMY_BLUE.ordinal()] = BitmapFactory.decodeResource(getResources(), R.drawable.enemy_blue);
        mBitmaps[BitmapType.ENEMY_GREEN.ordinal()] = BitmapFactory.decodeResource(getResources(), R.drawable.enemy_green);
        mBitmaps[BitmapType.ENEMY_RED.ordinal()] = BitmapFactory.decodeResource(getResources(), R.drawable.enemy_red);
        mBitmaps[BitmapType.BULLET.ordinal()] = BitmapFactory.decodeResource(getResources(), R.drawable.bullet);
        mBitmaps[BitmapType.EXPLOSION.ordinal()] = BitmapFactory.decodeResource(getResources(), R.drawable.explosion);
        mBitmaps[BitmapType.BACKGROUND_WATER.ordinal()] = BitmapFactory.decodeResource(getResources(), R.drawable.water);
        mBitmaps[BitmapType.BACKGROUND_DESERT.ordinal()] = BitmapFactory.decodeResource(getResources(), R.drawable.desert);
        mBitmaps[BitmapType.BACKGROUND_BASE.ordinal()] = BitmapFactory.decodeResource(getResources(), R.drawable.base);

        mPaint = new Paint();
        mPlayer = new AnimatedSprite(mBitmaps[BitmapType.PLAYER.ordinal()], 3, 6, SpriteType.PLAYER);
        mSprites = new CopyOnWriteArrayList<>();
        mEnemy = new CopyOnWriteArrayList<>();
        mBullets = new CopyOnWriteArrayList<>();

        mLevel = 1;
        mBackgroundY = 0;

        // Create SoundPool
        AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        mSoundPool = new SoundPool.Builder().setAudioAttributes(audioAttributes).setMaxStreams(2).build();

        // Load sound effect
        mExplosionSoundId = mSoundPool.load(context, R.raw.explosion, 1);

        // Load all bitmaps at the start of the game
    }

    protected void start() {
        mDisplayDensity = getResources().getDisplayMetrics().density;
        mBackgroundSpeed = 50f * mDisplayDensity;
        mPlayer.setPosition(getWidth() / 2, getHeight() / 2);
        mPlayer.setScale(mDisplayDensity / 2);
        mPlayer.setDraggable(true);
        mSprites.add(mPlayer);
        mHandler.postDelayed(mActivateEnemyTask, 0);
        collisionThread = new CollisionThread(mSprites, 100, getWidth(), getHeight(), this);
        collisionThread.start();
    }

    protected void update(float deltaTime) {
        if (mIsGameOver || mIsGameWin) {
            deltaTime = 0;
            mPlayer.setDragging(false);
            mHandler.removeCallbacks(mActivateEnemyTask);
            collisionThread.shutDown();
            mHandler.postDelayed(() -> {
                if (getContext() instanceof MainActivity) {
                    ((MainActivity) getContext()).gameOver(mScore);
                }
            }, 3000);
        }
        // Check if the score has reached the threshold for the next level
        if (mScore >= mNextLevelScore) {
            mLevel++;
            mNextLevelScore += 100;  // Update the score threshold for the next level
        }
        if (mLevel > 4) {
            mIsGameWin = true;
        }
        updateBackground(deltaTime);
        fireBullets();
        updateEnemies(deltaTime);
        updateBullets(deltaTime);
    }

    private void updateBackground(float deltaTime) {
        mBackgroundY += mBackgroundSpeed * deltaTime;
        if (mBackgroundY > mBitmaps[BitmapType.BACKGROUND_WATER.ordinal()].getHeight()) {
            mBackgroundY -= mBitmaps[BitmapType.BACKGROUND_WATER.ordinal()].getHeight();
        }
    }

    protected void render(@NonNull Canvas canvas) {
        drawBackgroundBitmap(mLevel, canvas);
        mPaint.setColor(Color.RED);
        mPaint.setTextSize(24f * mDisplayDensity);

        drawMultilineText(canvas, "Score :" + mScore, 16f, 48f, mPaint, false);
        drawMultilineText(canvas, "HP :" + mPlayerHP, 16f, 48f * mDisplayDensity, mPaint, false);

        for (Sprite sprite : mSprites) {
            if (sprite instanceof AnimatedSprite) {
                ((AnimatedSprite) sprite).handleAnimation();
            }
            sprite.render(canvas);
        }

        if (mIsGameWin) {
            mPaint.setColor(Color.RED);
            mPaint.setTextSize(32f * mDisplayDensity);
            drawMultilineText(canvas, "You Win", getWidth() / 2, getHeight() / 2, mPaint, true);
            drawMultilineText(canvas, "Backing to Menu", getWidth() / 2, getHeight() / 2 + 48f * mDisplayDensity, mPaint, true);
        } else if (mIsGameOver) {
            mPaint.setColor(Color.RED);
            mPaint.setTextSize(32f * mDisplayDensity);
            drawMultilineText(canvas, "Game Over", getWidth() / 2, getHeight() / 2, mPaint, true);
            drawMultilineText(canvas, "Backing to Menu", getWidth() / 2, getHeight() / 2 + 48f * mDisplayDensity, mPaint, true);
        } else if (!mIsGameStart) {
            mPaint.setColor(Color.RED);
            mPaint.setTextSize(48f * mDisplayDensity);
            drawMultilineText(canvas, "Falcon Strike", getWidth() / 2, getHeight() / 2 / 2, mPaint, true);
            mPaint.setTextSize(32f * mDisplayDensity);
            drawMultilineText(canvas, "Touch and drag the\nplayer to start", getWidth() / 2, getHeight() / 2, mPaint, true);
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                for (Sprite sprite : mSprites) {
                    if (sprite.isDraggable() && sprite.isTouched(event)) {
                        mDraggingSprite = (AnimatedSprite) sprite;
                        mDraggingSprite.setDragging(true);
                        return true;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mDraggingSprite != null) {
                    if (!mIsGameStart) {
                        mIsGameStart = true;
                    }
                    mDraggingSprite.setPosition(event.getX(), event.getY());
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mDraggingSprite != null) {
                    mDraggingSprite.setDragging(false);
                    mDraggingSprite = null;
                }
                break;
        }
        return true;
    }

    public void spawnEnemy() {
        AnimatedSprite enemy;
        float speed;
        int enemyType = (int) (Math.random() * mLevel) + 1;
        switch (enemyType) {
            case 2:
                // Use the preloaded bitmap instead of decoding it from resources
                enemy = new AnimatedSprite(mBitmaps[BitmapType.ENEMY_BLUE.ordinal()], 3, 6, SpriteType.ENEMY);
                speed = 150 * mDisplayDensity * mLevel;
                break;
            case 3:
                // Use the preloaded bitmap instead of decoding it from resources
                enemy = new AnimatedSprite(mBitmaps[BitmapType.ENEMY_GREEN.ordinal()], 3, 6, SpriteType.ENEMY);
                speed = 200 * mDisplayDensity * mLevel;
                break;
            default:
                // Use the preloaded bitmap instead of decoding it from resources
                enemy = new AnimatedSprite(mBitmaps[BitmapType.ENEMY_RED.ordinal()], 3, 6, SpriteType.ENEMY);
                speed = 100 * mDisplayDensity * mLevel;
                break;
        }
        enemy.setSpeed(0, speed);
        enemy.setPosition((float) Math.random() * (getWidth() - enemy.getBounds().width()), -enemy.getBounds().height());
        mEnemy.add(enemy);
        mSprites.add(enemy);
    }

    @Override
    public void onPlayerEnemyCollision(AnimatedSprite player, AnimatedSprite enemy) {
        if (System.currentTimeMillis() - mLastHitTime > PLAYER_INVINCIBLE_TIME) {
            player.startBlinking(PLAYER_INVINCIBLE_TIME, 200);
            spawnExplosion(player);
            mPlayerHP--;
            mLastHitTime = System.currentTimeMillis();
            if (mPlayerHP <= 0) {
                mIsGameOver = true;
            }
            mEnemy.remove(enemy);
            mSprites.remove(enemy);
        }
    }

    @Override
    public void onBulletEnemyCollision(AnimatedSprite bullet, AnimatedSprite enemy) {
        spawnExplosion(enemy);
        mScore += 10;
        mBullets.remove(bullet);
        mSprites.remove(bullet);
        mEnemy.remove(enemy);
        mSprites.remove(enemy);
    }

    private void spawnExplosion(AnimatedSprite sprite) {
        AnimatedSprite explosion = new AnimatedSprite(mBitmaps[BitmapType.EXPLOSION.ordinal()], 3, 6, SpriteType.EXPLOSION);
        explosion.setPosition(sprite.getX(), sprite.getY());
        explosion.setSpeed(0, 0);
        mSprites.add(explosion);
        mSoundPool.play(mExplosionSoundId, 1, 1, 1, 0, 1);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mSprites.remove(explosion);
            }
        }, 3000);
    }


    private void updateEnemies(float deltaTime) {
        for (Sprite enemy : mEnemy) {
            enemy.move(deltaTime);
            if (enemy.getY() > getHeight()) {
                enemy.setPosition((float) Math.random() * (getWidth() - enemy.getBounds().width()), -enemy.getBounds().height());
            }
        }
    }

    private void fireBullets() {
        if (System.currentTimeMillis() - mLastBulletTime > BULLET_INTERVAL && mPlayer.isDragging()) {
            mLastBulletTime = System.currentTimeMillis();
            AnimatedSprite bullet = new AnimatedSprite(mBitmaps[BitmapType.BULLET.ordinal()], 3, 6, SpriteType.BULLET);
            bullet.setPosition(mPlayer.getX(), mPlayer.getY() - mPlayer.getBounds().height());
            bullet.setSpeed(0, BULLET_SPEED * mDisplayDensity);
            mBullets.add(bullet);
            mSprites.add(bullet);
        }
    }


    private void updateBullets(float deltaTime) {
        //更新子彈位置
        for (Sprite bullet : mBullets) {
            bullet.move(deltaTime);
            //子彈超出螢幕
            if (bullet.getY() < 0) {
                mBullets.remove(bullet);
                mSprites.remove(bullet);
            }
        }
    }


    protected void drawBackgroundBitmap(int mLevel, Canvas canvas) {
        switch (mLevel - 1) {
            case 1:
                mBackgroundBitmap = mBitmaps[BitmapType.BACKGROUND_WATER.ordinal()];
                break;
            case 2:
                mBackgroundBitmap = mBitmaps[BitmapType.BACKGROUND_DESERT.ordinal()];
                break;
            case 3:
                mBackgroundBitmap = mBitmaps[BitmapType.BACKGROUND_BASE.ordinal()];
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

    protected void drawMultilineText(Canvas canvas, String text, float x, float y, Paint paint, boolean centerText) {
        Rect textBounds = new Rect();
        float yOffset = 0f;
        for (String line : text.split("\n")) {
            paint.getTextBounds(line, 0, line.length(), textBounds);
            yOffset += textBounds.height();
            float adjustedX = centerText ? x - textBounds.width() / 2 : x;
            canvas.drawText(line, adjustedX, y + yOffset, mPaint);
        }
    }
}