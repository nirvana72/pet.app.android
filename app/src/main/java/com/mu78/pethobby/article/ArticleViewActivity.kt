package com.mu78.pethobby.article

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.*
import android.widget.ImageView
import com.google.gson.GsonBuilder
import com.kaopiz.kprogresshud.KProgressHUD
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.util.XPopupUtils
import com.mu78.pethobby.BaseActivity
import com.mu78.pethobby.R
import com.mu78.pethobby.auth.UserInfoActivity
import com.mu78.pethobby.common.InputPopView
import com.mu78.pethobby.common.VideoPlayActivity
import com.mu78.pethobby.extension.MyOkHttpCallback
import com.mu78.pethobby.extension.TokenInterceptor
import com.mu78.pethobby.modules.BaseApiResult
import com.mu78.pethobby.utils.MyConfig
import com.mu78.pethobby.utils.MySession
import com.mu78.pethobby.utils.MyTools
import com.mu78.pethobby.utils.MyXPopupImageLoader
import kotlinx.android.synthetic.main.article_view.*
import okhttp3.*

class ArticleViewActivity : BaseActivity() {

    private var articleId = -1
    private var isLiked: Boolean? = null
    private lateinit var ivLike: ImageView
    private lateinit var mHud: KProgressHUD
    private var mReplyTxt = "" // 回复内容存放变量
    private var isReplySubject = true // 是否回复主题变量， 如果是直接点击回复框，则回复主题， 如果是客户端触发，则是回复评论

    @SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.articleId = this.intent.getIntExtra("articleId", -1)

        this.setContentView(R.layout.article_view)

        this.mHud = KProgressHUD.create(this).setCancellable(false).show()

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

                val params = "{articleId: '${this@ArticleViewActivity.articleId}'}"
                val session = "{" +
                        "uid:'${MySession.getInstance().uid}', " +
                        "token:'${MySession.getInstance().token}'," +
                        "time:'${MySession.getInstance().time}'" +
                        "}"
                val config = "{" +
                        "api_host:'${MyConfig.APP_API_HOST}'," +
                        "oss_host: '${MyConfig.APP_OSS_HOST}'" +
                        "}"
                var width = XPopupUtils.getWindowWidth(this@ArticleViewActivity)
                width = MyTools.px2dp(this@ArticleViewActivity, width.toFloat())
                val device = "{" +
                        "env: 'android', " +
                        "width: $width, " +
                        "version: '${MyTools.getDeviceDescription()}'" +
                        "}"

                val js = "javascript:window.nativeCallJs({cmd:'init', params:$params, session:$session, config:$config, device:$device})"
                view?.loadUrl(js)
            }
        }

        this.webView.loadUrl("file:///android_asset/h5/article.html")
        // this.webView.loadUrl("http://192.168.51.127:8080/article")

        this.tvInput.setOnClickListener {
            this.showReplyView("主题", true)
        }
    }

    // 代码生成导航栏右边链接
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(1, Menu.FIRST + 1, 0, "")!!.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        val item = menu.getItem(0)

        ivLike = ImageView(this)
        ivLike.setImageDrawable(resources.getDrawable(R.drawable.icon_favorite_border, null))
        ivLike.setColorFilter(Color.parseColor("#757575"))
        ivLike.setPadding(50, 0, 50, 0)
        ivLike.setOnClickListener {
            this.setLike {
                this.isLiked = true
                runOnUiThread {
                    ivLike.startAnimation(MyTools.getIconScaleAnimation())
                    ivLike.setImageDrawable(resources.getDrawable(R.drawable.icon_favorite, null))
                    ivLike.setColorFilter(Color.parseColor("#ff0000"))
                    this.toast("收藏成功")
                }
            }
        }
        item.actionView = ivLike
        return true
    }

    private fun setLike(callback:() -> Unit) {
        if (!MySession.isLogin()) {
            this.toast("登录后才能收藏")
            return
        }
        if (this.isLiked == null || this.isLiked!!) return

        val formBody = FormBody.Builder()
            .add("aid", this.articleId.toString())
            .add("uid", MySession.getInstance().uid.toString())
            .build()
        val url = "${MyConfig.APP_API_HOST}/articles/${this.articleId}/like"
        val client = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()
        val request = Request.Builder().url(url).put(formBody).build()
        client.newCall(request).enqueue(object: MyOkHttpCallback(this) {
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val apiResult = GsonBuilder().create().fromJson(body, BaseApiResult::class.java)
                if (apiResult.ret > 0) {
                    callback() // 回调通知
                }
                else {
                    XPopup.Builder(this@ArticleViewActivity).asConfirm("😱 code: ${response.code}", apiResult.msg) {}.hideCancelBtn().show()
                }
            }
        })
    }

    private fun showReplyView(replyTo: String, replySubject: Boolean) {

        if (!MySession.isLogin()) {
            XPopup.Builder(this).asConfirm("😱", "登录后回复") {}.hideCancelBtn().show()
            return
        }

        this.isReplySubject = replySubject

        val mInputPopView = InputPopView(this)

        mInputPopView.initData("回复:${replyTo}", this.mReplyTxt)

        mInputPopView.mInputCallBack = object: InputPopView.InputCallBack {
            override fun onClose(txt: String, isSend: Boolean) {
                this@ArticleViewActivity.setReplyTxt(txt)

                if (isSend) {
                    // 调用JS发送评论
                    val js = "javascript:window.nativeCallJs({cmd:'postComment',content: '$txt', isReplySubject: '${this@ArticleViewActivity.isReplySubject}'})"
                    this@ArticleViewActivity.webView.loadUrl(js)
                }
            }
        }
        //弹出新的弹窗用来输入
        XPopup.Builder(this)
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

    override fun finish() {
        // Log.d("log","finish")
        this.webView.loadUrl("about:blank")
        this.webView.clearCache(true)

        super.finish()
    }

    @JavascriptInterface
    fun jsCallAndroid(jsonStr: String) {
        // Log.d("log","jsonStr = $jsonStr")
        val json = GsonBuilder().create().fromJson(jsonStr, JsCallAndroidObject::class.java)
        when(json.cmd) {
            "didLoad" -> { // 加载完成
                this.mHud.dismiss()
            }
            "setLike" -> { // 是否收藏
                this.isLiked = json.msg.toBoolean()
                if (this.isLiked!!) {
                    ivLike.setImageDrawable(resources.getDrawable(R.drawable.icon_favorite, null))
                    ivLike.setColorFilter(Color.parseColor("#ff0000"))
                }
            }
            "authorClick" -> { // 点击用户头像
                val intent = Intent(this, UserInfoActivity::class.java)
                intent.putExtra("uid", json.authorId)
                this.startActivity(intent)
            }
            "reply" -> {// 回复他人评论
                this.showReplyView(json.replyTo, false)
            }
            "posted" -> {// 发布完成
                this.setReplyTxt("")
                XPopup.Builder(this).asConfirm("😀","提交成功") {}.hideCancelBtn().show()
            }
            "alert" -> {// 弹出提示
                XPopup.Builder(this).asConfirm("😀", json.msg) {}.hideCancelBtn().show()
            }
            "error" -> {// 弹出错误提示
                this.mHud.dismiss()
                XPopup.Builder(this).asConfirm("😱", json.msg) {}.hideCancelBtn().show()
            }
            "token-expired" -> { // 登录超时
                val that = this
                MySession.callRefreshToken(object : MySession.InputCallBack {
                    override fun refreshed(newToken: String) {
                        XPopup.Builder(that).asConfirm("😱", "登录信息过期，请重试") {}.hideCancelBtn().show()
                        runOnUiThread {
                            val js = "javascript:window.nativeCallJs({cmd:'refreshToken',token: '${MySession.getInstance().token}'})"
                            that.webView.loadUrl(js)
                        }
                    }
                })
            }
            "imgClick" -> { // 图文内容图片点击
                val fullPathImages = mutableListOf<Any>()
                for(img in json.list) {
                    val url = MyTools.ossPath(this.articleId, json.writetime, img)
                    fullPathImages.add(url)
                }

                XPopup.Builder(this)
                    .asImageViewer(null, json.startIndex, fullPathImages,null , MyXPopupImageLoader())
                    .isShowSaveButton(false)
                    .show()
            }
            "videoClick" -> { // 视频点击，调用原生视频播放
                val intent = Intent(this, VideoPlayActivity::class.java)
                intent.putExtra("url", json.videoUrl)
                this.startActivity(intent)
            }
            "richImageClick" -> { // 富文本内容图片点击
                val fullPathImages = mutableListOf<Any>()
                for(img in json.list) {
                    fullPathImages.add(img)
                }
                XPopup.Builder(this)
                    .asImageViewer(null, json.startIndex, fullPathImages,null , MyXPopupImageLoader())
                    .isShowSaveButton(false)
                    .show()
            }
        }
    }

    // ----------------------------------------
    // 与JS交互时， JS提交JOSN字符串，用此类转换
    class JsCallAndroidObject (
        val cmd: String,
        val msg: String,
        val authorId: Int,
        val replyTo: String,
        val startIndex: Int,
        val writetime: String,
        val videoUrl:String,
        val list: Array<String>
    )
}