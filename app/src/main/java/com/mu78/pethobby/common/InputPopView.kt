package com.mu78.pethobby.common

import android.content.Context
import com.lxj.xpopup.core.BottomPopupView
import com.mu78.pethobby.R
import kotlinx.android.synthetic.main.common_inputpopview.view.*

class InputPopView (ctx : Context) : BottomPopupView(ctx) {

    var mInputCallBack: InputCallBack? = null
    private var isSend = false
    private var mReplyTo = ""
    private var mReplyTxt = ""

    override fun getImplLayoutId(): Int {
        return R.layout.common_inputpopview
    }

    override fun onCreate() {
        super.onCreate()

        btnSend.setOnClickListener {
            if (this.mInputCallBack != null) {
                val txt = txtEdit.text.toString().trim()
                if (txt != "") {
                    this.isSend = true
                    this.dismiss()
                }
            }
        }

        tvReplyTo.text = this.mReplyTo
        txtEdit.setText(this.mReplyTxt)
    }

    override fun onDismiss() {
        super.onDismiss()

        if (this.mInputCallBack != null) {
            val txt = txtEdit.text.toString().trim()
            this.mInputCallBack!!.onClose(txt, this.isSend)
            this.isSend = false
        }
    }

    fun initData(reply: String, txt: String) {
        this.mReplyTo = reply
        this.mReplyTxt = txt
    }

    interface InputCallBack {
        fun onClose(txt: String, isSend: Boolean)
    }
}