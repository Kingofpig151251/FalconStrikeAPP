package com.example.falcon_strike_app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CollisionThread extends Thread {
    private final CollisionListener mCollisionListener;

    private final CopyOnWriteArrayList<AnimatedSprite> mSprites;

    private final List<AnimatedSprite>[][] mGrid;
    private final int mGridSize;
    private final int mScreenWidth;
    private final int mScreenHeight;

    private volatile boolean running = true;

    public CollisionThread(CopyOnWriteArrayList<AnimatedSprite> sprites, int gridSize, int screenWidth, int screenHeight, CollisionListener collisionListener) {
        this.mSprites = sprites;
        this.mGridSize = gridSize;
        this.mScreenWidth = screenWidth;
        this.mScreenHeight = screenHeight;
        this.mGrid = new ArrayList[screenWidth / gridSize + 1][screenHeight / gridSize + 1];
        for (int i = 0; i < mGrid.length; i++) {
            for (int j = 0; j < mGrid[i].length; j++) {
                mGrid[i][j] = new ArrayList<>();
            }
        }
        this.mCollisionListener = collisionListener;
    }

    @Override
    public void run() {
        while (running) {
            clearGrid();
            for (AnimatedSprite sprite : mSprites) {
                addSpriteToGrid(sprite);
            }
            for (AnimatedSprite sprite : mSprites) {
                checkCollisionsForSprite(sprite);
            }
        }
    }

    private void clearGrid() {
        for (int i = 0; i < mGrid.length; i++) {
            for (int j = 0; j < mGrid[i].length; j++) {
                mGrid[i][j].clear();
            }
        }
    }

    private void addSpriteToGrid(AnimatedSprite sprite) {
        int x = (int) (sprite.getX() / mGridSize);
        int y = (int) (sprite.getY() / mGridSize);
        if (x >= 0 && x < mGrid.length && y >= 0 && y < mGrid[0].length) {
            mGrid[x][y].add(sprite);
        }
    }

    private void checkCollisionsForSprite(AnimatedSprite sprite) {
        int x = (int) (sprite.getX() / mGridSize);
        int y = (int) (sprite.getY() / mGridSize);
        for (int i = Math.max(0, x - 1); i <= Math.min(mGrid.length - 1, x + 1); i++) {
            for (int j = Math.max(0, y - 1); j <= Math.min(mGrid[i].length - 1, y + 1); j++) {
                for (AnimatedSprite otherSprite : mGrid[i][j]) {
                    if (sprite != otherSprite && sprite.collideWith(otherSprite)) {
                        if (sprite.getType() == SpriteType.PLAYER && otherSprite.getType() == SpriteType.ENEMY) {
                            mCollisionListener.onPlayerEnemyCollision(sprite, otherSprite);
                        } else if (sprite.getType() == SpriteType.BULLET && otherSprite.getType() == SpriteType.ENEMY) {
                            mCollisionListener.onBulletEnemyCollision(sprite, otherSprite);
                        }
                    }
                }
            }
        }
    }

    public void shutDown() {
        running = false;
    }
}

