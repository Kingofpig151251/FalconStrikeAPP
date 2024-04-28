package com.example.falcon_strike_app;

public interface CollisionListener {
    void onPlayerEnemyCollision(AnimatedSprite player, AnimatedSprite enemy);
    void onBulletEnemyCollision(AnimatedSprite bullet, AnimatedSprite enemy);
}

