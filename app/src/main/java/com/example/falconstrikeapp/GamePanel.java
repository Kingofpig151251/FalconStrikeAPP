package com.example.falconstrikeapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
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

import java.util.concurrent.CopyOnWriteArrayList;

public class GamePanel extends SurfaceView implements CollisionListener {
    private static final int MAX_ENEMIES = 10;
    private static final float BULLET_SPEED = -300f;
    private static final long BULLET_INTERVAL = 1000;
    private static final long PLAYER_INVINCIBLE_TIME = 3000;  // 玩家的無敵時間（毫秒）

    private long mLastHitTime;  // 玩家上次被撞的時間
    private final Paint mPaint;
    private final CopyOnWriteArrayList<AnimatedSprite> mSprites;
    private final AnimatedSprite mPlayer;
    private final CopyOnWriteArrayList<AnimatedSprite> mEnemy;
    private final CopyOnWriteArrayList<AnimatedSprite> mBullets;

    private CollisionThread collisionThread;

    private Bitmap mBackgroundBitmap;
    private float mDisplayDensity;
    private int mLevel;
    private float mBackgroundY;
    private float mBackgroundSpeed;
    private long mLastBulletTime;

    private int mPlayerHP = 3;

    private int mScore = 0;
    private float centerX, centerY;
    private boolean mIsGameOver = false;
    private boolean mIsGameWin = false;
    private boolean mIsGameStart = false;

    private final SoundPool mSoundPool;
    private final int mExplosionSoundId;

    private final Handler mHandler = new Handler();
    //每1秒激活一个敌人
    private final Runnable mActivateEnemyTask = new Runnable() {
        @Override
        public void run() {
            if (mIsGameOver || mIsGameWin) return;
            if (mEnemy.size() < MAX_ENEMIES) {
                if (mIsGameStart)
                    spawnEnemy();
            }
            mHandler.postDelayed(this, 2000);
        }
    };

    public GamePanel(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        mPaint = new Paint();
        mPlayer = new AnimatedSprite(BitmapFactory.decodeResource(getResources(), R.drawable.player), 3, 6, SpriteType.PLAYER);
        mSprites = new CopyOnWriteArrayList<>();
        mEnemy = new CopyOnWriteArrayList<>();
        mBullets = new CopyOnWriteArrayList<>();

        mLevel = 1;
        mBackgroundY = 0;

        // 創建 SoundPool
        AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        mSoundPool = new SoundPool.Builder().setAudioAttributes(audioAttributes).setMaxStreams(2).build();

        // 加載音效
        mExplosionSoundId = mSoundPool.load(context, R.raw.explosion, 1);

    }

    protected void start() {
        mDisplayDensity = getResources().getDisplayMetrics().density;
        mBackgroundSpeed = 50f * mDisplayDensity;
        centerX = (getWidth() - mPlayer.getBounds().width() * mDisplayDensity);
        centerY = (getHeight() - mPlayer.getBounds().height() * mDisplayDensity);
        mPlayer.setPosition(centerX, centerY);
        mPlayer.setScale(mDisplayDensity / 2);
        mPlayer.setDraggable(true);
        mSprites.add(mPlayer);
        mHandler.postDelayed(mActivateEnemyTask, 0);
        collisionThread = new CollisionThread(mSprites, 100, getWidth(), getHeight(), this);
        collisionThread.start();
    }

    protected void update(float deltaTime) {
        if (mIsGameOver || mIsGameWin) {
            mHandler.postDelayed(() -> ((Activity) getContext()).finish(), 5000);
            return;
        }
        switch (mScore) {
            case 100:
                mLevel = 2;
                break;
            case 200:
                mLevel = 3;
                break;
            case 300:
                mIsGameWin = true;
        }
        updateBackground(deltaTime);
        fireBullets();
        updateEnemies(deltaTime);
        updateBullets(deltaTime);
    }

    private void updateBackground(float deltaTime) {
        if (mBackgroundBitmap != null) {
            mBackgroundY += mBackgroundSpeed * deltaTime;
            if (mBackgroundY > mBackgroundBitmap.getHeight()) {
                mBackgroundY -= mBackgroundBitmap.getHeight();
            }
        }
    }

    private void fireBullets() {
        if (System.currentTimeMillis() - mLastBulletTime > BULLET_INTERVAL && mPlayer.isDragging()) {
            mLastBulletTime = System.currentTimeMillis();
            AnimatedSprite bullet = new AnimatedSprite(BitmapFactory.decodeResource(getResources(), R.drawable.bullet), 3, 6, SpriteType.BULLET);
            bullet.setPosition(mPlayer.getX(), mPlayer.getY() - mPlayer.getBounds().height());
            bullet.setSpeed(0, BULLET_SPEED * mDisplayDensity);
            mBullets.add(bullet);
            mSprites.add(bullet);
        }
    }

    private void updateEnemies(float deltaTime) {
        for (Sprite enemy : mEnemy) {
            enemy.move(deltaTime);
            if (enemy.getY() > getHeight()) {
                enemy.setPosition((float) Math.random() * (getWidth() - enemy.getBounds().width()), -enemy.getBounds().height());
            }
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


    protected void render(@NonNull Canvas canvas) {
        drawBackgroundBitmap(mLevel, canvas);
        mPaint.setColor(Color.RED);
        mPaint.setTextSize(24f * mDisplayDensity);

        drawMultilineText(canvas, "Score :" + mScore, 16f, 48f, mPaint, false);
        drawMultilineText(canvas, "HP :" + mPlayerHP, 16f, 48f * mDisplayDensity, mPaint, false);

        if (mIsGameOver) {
            mPaint.setColor(Color.RED);
            mPaint.setTextSize(32f * mDisplayDensity);
            drawMultilineText(canvas, "Game Over", centerX, centerY, mPaint, true);
        }
        if (mIsGameWin) {
            mPaint.setColor(Color.RED);
            mPaint.setTextSize(32f * mDisplayDensity);
            drawMultilineText(canvas, "You Win", centerX, centerY * 2f, mPaint, true);
        }

        if (!mIsGameStart) {
            mPaint.setColor(Color.RED);
            mPaint.setTextSize(48f * mDisplayDensity);
            drawMultilineText(canvas, "Falcon Strike", centerX, centerY / 2, mPaint, true);
            mPaint.setTextSize(32f * mDisplayDensity);
            drawMultilineText(canvas, "Touch and drag the\nplayer to start", centerX, centerY, mPaint, true);
        }

        for (Sprite sprite : mSprites) {
            if (sprite instanceof AnimatedSprite) {
                ((AnimatedSprite) sprite).handleAnimation();
            }
            sprite.render(canvas);
        }
    }


    protected void drawBackgroundBitmap(int mState, Canvas canvas) {
        switch (mState) {
            case 1:
                mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.water);
                break;
            case 2:
                mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.desert);
                break;
            case 3:
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

    protected void drawMultilineText(Canvas canvas, String text, float x, float y, Paint paint, boolean centerText) {
        Rect textBounds = new Rect();
        float yOffset = 0f;

        for (String line : text.split("\n")) {
            paint.getTextBounds(line, 0, line.length(), textBounds);
            yOffset += textBounds.height();
            float adjustedX = centerText ? x - textBounds.width() / 2 : x;  // 根據是否需要置中來調整 x 的位置
            canvas.drawText(line, adjustedX, y + yOffset, mPaint);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        for (Sprite sprite : mSprites) {
            if (!sprite.isDraggable()) continue;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (sprite.isTouched(event)) {
                        sprite.setDragging(true);
                        return true;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (sprite.isDragging()) {
                        if (!mIsGameStart) {
                            mIsGameStart = true;
                        }
                        sprite.setPosition(event.getX(), event.getY());
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (sprite.isDragging()) sprite.setDragging(false);
                    break;
            }
        }
        return true;
    }

    public void spawnEnemy() {
        AnimatedSprite enemy;
        float speed;
        int enemyType = (int) (Math.random() * mLevel) + 1;
        switch (enemyType) {
            case 1:
                enemy = new AnimatedSprite(BitmapFactory.decodeResource(getResources(), R.drawable.enemy_red), 3, 6, SpriteType.ENEMY);
                speed = 100 * mDisplayDensity * mLevel;
                break;
            case 2:
                enemy = new AnimatedSprite(BitmapFactory.decodeResource(getResources(), R.drawable.enemy_blue), 3, 6, SpriteType.ENEMY);
                speed = 150 * mDisplayDensity * mLevel;
                break;
            case 3:
                enemy = new AnimatedSprite(BitmapFactory.decodeResource(getResources(), R.drawable.enemy_green), 3, 6, SpriteType.ENEMY);
                speed = 200 * mDisplayDensity * mLevel;
                break;
            default:
                enemy = new AnimatedSprite(BitmapFactory.decodeResource(getResources(), R.drawable.enemy_red), 3, 6, SpriteType.ENEMY);
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
        // 在這裡處理玩家與敵人的碰撞
        // 檢查玩家是否在無敵時間內
        if (System.currentTimeMillis() - mLastHitTime > PLAYER_INVINCIBLE_TIME) {
            player.startBlinking(PLAYER_INVINCIBLE_TIME, 200);  // 閃爍時間為無敵時間，閃爍間隔為200毫秒
            spawnExplosion(player);
            mPlayerHP--;
            mLastHitTime = System.currentTimeMillis();  // 更新最後被撞的時間
            if (mPlayerHP <= 0) {
                mIsGameOver = true;
            }
            mEnemy.remove(enemy);
            mSprites.remove(enemy);
        }
    }

    @Override
    public void onBulletEnemyCollision(AnimatedSprite bullet, AnimatedSprite enemy) {
        // 在這裡處理子彈與敵人的碰撞
        spawnExplosion(enemy);
        mScore += 10;
        mBullets.remove(bullet);
        mSprites.remove(bullet);
        mEnemy.remove(enemy);
        mSprites.remove(enemy);
    }


    private void spawnExplosion(AnimatedSprite sprite) {
        AnimatedSprite explosion = new AnimatedSprite(BitmapFactory.decodeResource(getResources(), R.drawable.explosion), 3, 6, SpriteType.EXPLOSION);
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
}