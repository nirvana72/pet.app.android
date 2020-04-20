package com.mu78.pethobby.article

import android.content.Context
import android.util.Log
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import com.google.gson.GsonBuilder
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.CenterPopupView
import com.lxj.xpopup.util.XPopupUtils
import com.mu78.pethobby.R
import com.mu78.pethobby.extension.MyOkHttpCallback
import com.mu78.pethobby.extension.TokenInterceptor
import com.mu78.pethobby.utils.MyConfig
import kotlinx.android.synthetic.main.article_review.*
import kotlinx.android.synthetic.main.article_review_reject.view.*
import okhttp3.*
import java.io.IOException

class ArticleReviewRejectPopView (val ctx: Context, val articleId : Int) : CenterPopupView(ctx) {

    interface OnRejectListener {
        fun onReject()
    }

    var listener: OnRejectListener? = null

    override fun getImplLayoutId(): Int {
        return R.layout.article_review_reject
    }

    override fun onCreate() {
        super.onCreate()

        this.tvReason1.setOnClickListener {
            this.etReason.setText(this.tvReason1.text)
        }

        this.tvReason2.setOnClickListener {
            this.etReason.setText(this.tvReason2.text)
        }

        this.tvReason3.setOnClickListener {
            this.etReason.setText(this.tvReason3.text)
        }

        this.btnReject.setOnClickListener {
            val txt = this.etReason.text.toString().trim()
            if (txt != "") {
                this.dismiss()
                this.doReject(txt)
            }
        }
    }

    private fun doReject(reason: String) {
        val formBody = FormBody.Builder()
            .add("cmd", "reject")
            .add("reason", reason)
            .build()

        val url = "${MyConfig.APP_API_HOST}/articles/${articleId}/review"
        val client = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()
        val request = Request.Builder().url(url).put(formBody).build()
        val that = this
        client.newCall(request).enqueue(object: MyOkHttpCallback(ctx) {
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val apiResult = GsonBuilder().create().fromJson(body, com.mu78.pethobby.modules.BaseApiResult::class.java)
                if (apiResult.ret < 0) {
                    XPopup.Builder(ctx).asConfirm("😱 code: ${response.code}", apiResult.msg) {}.hideCancelBtn().show()
                }
                else {
                    that.listener?.onReject()
                }
            }
        })
    }

    override fun getMaxHeight(): Int {
        return 1200
    }

    override fun getMaxWidth(): Int {
        var width = XPopupUtils.getWindowWidth(ctx)
        width -= 200
        if (width > 1500) width = 1500
        return width
    }
}