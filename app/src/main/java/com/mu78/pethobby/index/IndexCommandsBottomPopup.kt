package com.mu78.pethobby.index

import android.content.Context
import android.widget.TextView
import com.lxj.xpopup.core.BottomPopupView
import com.mu78.pethobby.R
import kotlinx.android.synthetic.main.index_commands_popup.view.*

class IndexCommandsBottomPopup (ctx: Context) : BottomPopupView(ctx) {

    interface OnCommandListener {
        fun onCommand(cmd: String, articleId: Int)
    }

    var articleId: Int = -1
    var onCommandListener:OnCommandListener? = null

    override fun getImplLayoutId(): Int {
        return R.layout.index_commands_popup
    }

    override fun onCreate() {
        super.onCreate()

        for(index in 0..5) {
            val tv = this.mLayout.getChildAt(index * 2) as TextView
            tv.setOnClickListener {
                onCommandListener?.onCommand(tv.text.toString(), this.articleId)
                this.dismiss()
            }
        }

    }
}