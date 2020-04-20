package com.mu78.pethobby

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

class LunchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.setContentView(R.layout.lunch)

        Handler().postDelayed({
            val intent = Intent()
            intent.setClass(this, com.mu78.pethobby.index.IndexActivity::class.java)
            startActivity(intent)
            this@LunchActivity.finish()
        }, 1000)
    }
}