package com.example.falconstrikeapp;

public interface CollisionListener {
    void onPlayerEnemyCollision(AnimatedSprite player, AnimatedSprite enemy);
    void onBulletEnemyCollision(AnimatedSprite bullet, AnimatedSprite enemy);
}

