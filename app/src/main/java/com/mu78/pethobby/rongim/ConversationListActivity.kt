package com.mu78.pethobby.rongim

import android.os.Bundle
import com.mu78.pethobby.BaseActivity
import com.mu78.pethobby.R

class ConversationListActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.title = "会话列表"
        this.setContentView(R.layout.rongim_conversationlist)
    }
}