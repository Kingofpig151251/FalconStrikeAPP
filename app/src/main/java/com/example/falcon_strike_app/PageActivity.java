package com.example.falcon_strike_app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.Button;
import android.widget.ImageView;

import com.example.falconstrikeapp.R;

public class PageActivity extends Activity {
    Button startButton, helpButton;

    @SuppressLint("MissingInflatedId")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cover);

        ImageView imageView = findViewById(R.id.imageView);

        // Get the dimensions of the device's screen
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;

        // Decode the image file
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cover_page, options);

        // Calculate the new height to maintain the aspect ratio
        float originalWidth = bitmap.getWidth();
        float originalHeight = bitmap.getHeight();
        int newHeight = Math.round((originalHeight * screenWidth) / originalWidth);

        // Scale the bitmap
        bitmap = Bitmap.createScaledBitmap(bitmap, screenWidth, newHeight, true);

        imageView.setImageBitmap(bitmap);

        startButton = findViewById(R.id.startButton);
        helpButton = findViewById(R.id.helpButton);
        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(PageActivity.this, MainActivity.class);
            startActivityForResult(intent, 1);
        });
        helpButton.setOnClickListener(v -> {
            new AlertDialog.Builder(PageActivity.this).setTitle("How to play?").setMessage("1. Drag the aircraft to move\n2. Bullets will be automatically fired during dragging\n3. Shoot down the enemy aircraft to gain points\n4. Life will be deducted if touched by the enemy.").setPositiveButton("OK", null).show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                int score = data.getIntExtra("score", 0);
                new AlertDialog.Builder(this).setTitle("Your score: ").setMessage(String.valueOf(score)).setPositiveButton("OK", null).show();
            }
        }
    }
}