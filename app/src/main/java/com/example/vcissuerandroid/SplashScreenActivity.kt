package com.example.vcissuerandroid

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.animation.AnimationUtils
import kotlinx.android.synthetic.main.activity_splash_screen.*

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        startAnimation()

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }, 2000)
    }


    private fun startAnimation(){
        val fromLeft = AnimationUtils.loadAnimation(this,R.anim.from_left)
        val fromRight = AnimationUtils.loadAnimation(this,R.anim.from_right)
        val fromBottom = AnimationUtils.loadAnimation(this,R.anim.from_bottom)
        val fromTop = AnimationUtils.loadAnimation(this,R.anim.from_top)



        appName1.startAnimation(fromRight)
        appName2.startAnimation(fromLeft)

        appDescription.startAnimation(fromBottom)

    }
}