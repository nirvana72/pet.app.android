package com.mu78.pethobby.index

import android.content.Context
import com.lxj.xpopup.core.HorizontalAttachPopupView
import com.mu78.pethobby.R
import kotlinx.android.synthetic.main.index_home_cell_commandpopup.view.*

class TabHomeFragmentCommandAttachPopup(ctx: Context) : HorizontalAttachPopupView(ctx) {

    interface OnCommandLinstner {
        fun onCommand(cmd: String)
    }

    var onCommandLinstner:OnCommandLinstner? = null

    override fun getImplLayoutId(): Int {
        return R.layout.index_home_cell_commandpopup
    }

    override fun onCreate() {
        super.onCreate()

        this.tvCmd_jubao.setOnClickListener {
            onCommandLinstner?.onCommand("jubao")
            this.dismiss()
        }

        this.tvCmd_pingbi.setOnClickListener {
            onCommandLinstner?.onCommand("pingbi")
            this.dismiss()
        }
    }
}