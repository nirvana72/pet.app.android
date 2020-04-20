package com.mu78.pethobby.common

import android.net.Uri
import android.os.Bundle
import com.mu78.pethobby.BaseActivity
import com.mu78.pethobby.R
import kotlinx.android.synthetic.main.videoplay.*

class VideoPlayActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.videoplay)

        this.supportActionBar?.hide()

        this.window.setBackgroundDrawable(resources.getDrawable(R.drawable.background_black, null))

        val url = this.intent.getStringExtra("url")
        videoView.setVideoURI(Uri.parse(url))
        videoView.start()

        img_back.setOnClickListener {
            this.finish()
        }
    }
}