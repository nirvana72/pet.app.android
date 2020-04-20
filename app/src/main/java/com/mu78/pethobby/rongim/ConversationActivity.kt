package com.mu78.pethobby.rongim

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.mu78.pethobby.R
import io.rong.imkit.RongExtension
import io.rong.imkit.fragment.ConversationFragment
import kotlinx.android.synthetic.main.rongim_conversation.*
import q.rorbin.badgeview.Badge
import q.rorbin.badgeview.QBadgeView

class ConversationActivity : AppCompatActivity() {
    var targetId = ""

    private lateinit var messageBadge: Badge

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.rongim_conversation)

        this.supportActionBar?.hide()

        this.tvTitle.text = intent.data.getQueryParameter("title")
        this.targetId = intent.data.getQueryParameter("targetId")

        // 系统消息， 不让回复
        if (this.targetId?.toInt() == 1000) {
            val mConversationFragment = this.conversation as ConversationFragment
            val mRongExtension = mConversationFragment.view?.findViewById<RongExtension>(R.id.rc_extension)
            mRongExtension?.visibility = View.INVISIBLE
        }

        ivBackIcon.setOnClickListener {
            this.finish()
        }

        this.messageBadge = QBadgeView(this).bindTarget(this.ivBackIcon)
        // this.messageBadge.setGravityOffset(-5f, 0f, true)

        instance = this
    }

    fun setMessageBadge() {
        this.messageBadge?.badgeNumber = -1
    }

    companion object {
        var instance: ConversationActivity? = null
    }
}