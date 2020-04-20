package com.mu78.pethobby.index

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.gson.GsonBuilder
import com.kaopiz.kprogresshud.KProgressHUD
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BottomPopupView
import com.lxj.xpopup.util.XPopupUtils
import com.mu78.pethobby.R
import com.mu78.pethobby.common.InputPopView
import com.mu78.pethobby.utils.MyConfig
import com.mu78.pethobby.utils.MySession
import com.mu78.pethobby.utils.MyTools
import kotlinx.android.synthetic.main.index_comments_popup.view.*

class IndexCommentsPopup (val ctx: Context, val articleId : Int, val authorId: Int) : BottomPopupView(ctx) {

    private lateinit var mHud: KProgressHUD
    private var mReplyTxt = "" // 回复内容存放变量
    private var isReplySubject = true // 是否回复主题变量， 如果是直接点击回复框，则回复主题， 如果是客户端触发，则是回复评论

    override fun getImplLayoutId(): Int {
        return R.layout.index_comments_popup
    }

    override fun getMaxHeight(): Int {
        val winH = XPopupUtils.getWindowHeight(context)
        val popH = winH * 0.8f
        return popH.toInt()
    }

    @SuppressLint("JavascriptInterface")
    override fun onCreate() {
        super.onCreate()

        this.mHud = KProgressHUD.create(ctx).setCancellable(false).show()

        this.layoutPopWin.layoutParams.height = this.maxHeight

        // 允许加载 HTTP manifests文件中还有一个设置
        // android:usesCleartextTraffic="true"
        // this.webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // 设置与Js交互的权限
        this.webView.settings.javaScriptEnabled = true
        // 设置允许JS弹窗
        // this.webView.settings.javaScriptCanOpenWindowsAutomatically = true
        // js 调用原生
        this.webView.addJavascriptInterface(this, "android")

        // webView中页面加载完回调后注入javascript
        this.webView.webViewClient = object: WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val params = "{" +
                        "articleId: '${this@IndexCommentsPopup.articleId}', " +
                        "authorId:'${this@IndexCommentsPopup.authorId}'" +
                        "}"
                val session = "{" +
                        "uid:'${MySession.getInstance().uid}', " +
                        "token:'${MySession.getInstance().token}'," +
                        "time:'${MySession.getInstance().time}'" +
                        "}"
                val config = "{" +
                        "api_host:'${MyConfig.APP_API_HOST}'," +
                        "oss_host: '${MyConfig.APP_OSS_HOST}'" +
                        "}"
                val device = "{env: 'android', version: '${MyTools.getDeviceDescription()}'}"

                val js = "javascript:window.nativeCallJs({cmd:'init', params:$params, session:$session, config:$config, device:$device})"
                view?.loadUrl(js)
            }
        }

        this.webView.loadUrl("file:///android_asset/h5/comment.html")
        // this.webView.loadUrl("http://192.168.31.43:8080/comment")

        this.tvInput.setOnClickListener {
            this.showReplyView("主题", true)
        }
    }

    private fun showReplyView(replyTo: String, replySubject: Boolean) {

        if (!MySession.isLogin()) {
            XPopup.Builder(ctx).asConfirm("😱", "登录后回复") {}.hideCancelBtn().show()
            return
        }

        this.isReplySubject = replySubject

        val mInputPopView = InputPopView(ctx)

        mInputPopView.initData("回复:${replyTo}", this.mReplyTxt)

        mInputPopView.mInputCallBack = object: InputPopView.InputCallBack {
            override fun onClose(txt: String, isSend: Boolean) {
                this@IndexCommentsPopup.setReplyTxt(txt)

                if (isSend) {
                    // 调用JS发送评论
                    val js = "javascript:window.nativeCallJs({cmd:'postComment',content: '$txt', isReplySubject: '${this@IndexCommentsPopup.isReplySubject}'})"
                    this@IndexCommentsPopup.webView.loadUrl(js)
                }
            }
        }
        //弹出新的弹窗用来输入
        XPopup.Builder(ctx)
            .autoOpenSoftInput(true)
            //.moveUpToKeyboard(false)
            .asCustom(mInputPopView)
            .show()
    }

    private fun setReplyTxt(txt: String) {
        this.mReplyTxt = txt
        this.tvInput.text = this.mReplyTxt
        if (this.mReplyTxt == "") {
            tvInput.text = "留下你的评论..."
        }
    }

    override fun onDismiss() {
        // Log.d("log","onDismiss")
        this.webView.loadUrl("about:blank")
        this.webView.clearCache(true)

        super.onDismiss()
    }

    @JavascriptInterface
    fun jsCallAndroid(jsonStr: String) {
        // Log.d("log","jsonStr = $jsonStr")
        val json = GsonBuilder().create().fromJson(jsonStr, JsCallAndroidObject::class.java)
        when(json.cmd) {
            "didLoad" -> { // 加载完成
                this.mHud.dismiss()
            }
            "reply" -> {// 回复他人评论
                this.showReplyView(json.replyTo, false)
            }
            "posted" -> {// 发布完成
                this.setReplyTxt("")
                XPopup.Builder(ctx).asConfirm("😀","提交成功") {}.hideCancelBtn().show()
            }
            "alert" -> {// 弹出提示
                XPopup.Builder(ctx).asConfirm("😀", json.msg) {}.hideCancelBtn().show()
            }
            "error" -> {// 弹出错误提示
                this.mHud.dismiss()
                XPopup.Builder(ctx).asConfirm("😱", json.msg) {}.hideCancelBtn().show()
            }
            "token-expired" -> { // 登录超时
                MySession.callRefreshToken(object : MySession.InputCallBack {
                    override fun refreshed(newToken: String) {
                        XPopup.Builder(ctx).asConfirm("😱", "登录信息过期，请重试") {}.hideCancelBtn().show()
                        val activity = ctx as Activity
                        activity.runOnUiThread {
                            val js = "javascript:window.nativeCallJs({cmd:'refreshToken',token: '${MySession.getInstance().token}'})"
                            this@IndexCommentsPopup.webView.loadUrl(js)
                        }
                    }
                })
            }
        }
    }

    // ----------------------------------------
    // 与JS交互时， JS提交JOSN字符串，用此类转换
    class JsCallAndroidObject (
        val cmd: String,
        val msg: String,
        val replyTo: String
    )
}