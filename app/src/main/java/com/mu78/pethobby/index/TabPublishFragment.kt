package com.mu78.pethobby.index

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mu78.pethobby.R
import com.mu78.pethobby.article.PublishImageActivity
import com.mu78.pethobby.article.PublishRichActivity
import com.mu78.pethobby.article.PublishVideoActivity
import com.mu78.pethobby.auth.LoginActivity
import com.mu78.pethobby.utils.MySession
import kotlinx.android.synthetic.main.index_publish.*

class TabPublishFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.index_publish, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        this.initView()
    }

    private fun initView(){

        btnPublishImage.setOnClickListener {
            this.showPublish("image")
        }

        btnPublishVideo.setOnClickListener {
            this.showPublish("video")
        }

        btnPublishRich.setOnClickListener {
            this.showPublish("rich")
        }
    }

    private fun showPublish (tp: String) {

        if (!MySession.isLogin()) {
            val intent = Intent(this.context, LoginActivity::class.java)
            startActivity(intent)
            return
        }

        when(tp) {
            "image" -> {
                val intent = Intent(this.context, PublishImageActivity::class.java)
                startActivity(intent)
            }
            "video" -> {
                val intent = Intent(this.context, PublishVideoActivity::class.java)
                startActivity(intent)
            }
            "rich" -> {
                val intent = Intent(this.context, PublishRichActivity::class.java)
                startActivity(intent)
            }
        }
    }
}