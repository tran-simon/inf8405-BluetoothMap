package com.inf8405.bluetoothmap

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class StartScreenActivity: AppCompatActivity() {

    companion object {
        const val SPLASH_SCREEN_TIME: Long = 3000;
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        supportActionBar?.hide()
        setContentView(R.layout.activity_splash_screen);
        super.onCreate(savedInstanceState);

        // TODO: change container according to dark/light theme
        val container: ImageView = findViewById(R.id.container);
        container.setImageResource(R.drawable.light_splash_screen);
    }


    override fun onResume() {
        super.onResume();
        startTimer();
    }


    private fun startTimer() {
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java);
            startActivity(intent);
            finish();
        }, SPLASH_SCREEN_TIME);

    }

}