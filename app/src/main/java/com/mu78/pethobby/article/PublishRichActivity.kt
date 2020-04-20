package com.mu78.pethobby.article

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.GsonBuilder
import com.kaopiz.kprogresshud.KProgressHUD
import com.lxj.xpopup.XPopup
import com.mu78.pethobby.BaseActivity
import com.mu78.pethobby.R
import com.mu78.pethobby.utils.MyConfig
import com.mu78.pethobby.utils.MySession
import com.mu78.pethobby.utils.MyTools
import kotlinx.android.synthetic.main.article_publish_rich.webView

class PublishRichActivity :BaseActivity() {

    private var mFileData: ValueCallback<Uri>? = null       // data/header received after file selection
    private var mFilePath: ValueCallback<Array<Uri>>? = null     // received file(s) temp. location
    private val mFileReqCode = 1
    private lateinit var mHud: KProgressHUD

    @SuppressLint("JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.title = "发布长贴"
        this.setContentView(R.layout.article_publish_rich)

        this.mHud = KProgressHUD.create(this).setCancellable(false)

        // 设置与Js交互的权限
        this.webView.settings.javaScriptEnabled = true
        // 设置允许访问文件数据
        this.webView.settings.allowFileAccess = true
        // js 调用原生
        this.webView.addJavascriptInterface(this, "android")

        // webView中页面加载完回调后注入javascript
        this.webView.webViewClient = object: WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                val session = "{" +
                        "uid:'${MySession.getInstance().uid}', " +
                        "token:'${MySession.getInstance().token}'," +
                        "time:'${MySession.getInstance().time}'" +
                        "}"
                val config = "{" +
                        "api_host:'${MyConfig.APP_API_HOST}'," +
                        "oss_accessKeyId:'${MyConfig.APP_OSS_accessKeyId}', " +
                        "oss_accessKeySecret: '${MyConfig.APP_OSS_accessKeySecret}', " +
                        "oss_bucket: '${MyConfig.APP_OSS_bucket}', " +
                        "oss_region: '${MyConfig.APP_OSS_region}', " +
                        "oss_host: '${MyConfig.APP_OSS_HOST}'" +
                        "}"
                val device = "{env: 'android', version: '${MyTools.getDeviceDescription()}'}"

                val js = "javascript:window.nativeCallJs({cmd:'init',session:$session,config:$config,device:$device})"
                // Log.d("log","js = $js")
                view?.loadUrl(js)
            }
        }
        // 安卓file=input默认不支持， 需要自己实现方法
        this.webView.webChromeClient = object : WebChromeClient() {
            /*-- openFileChooser is not a public Android API and has never been part of the SDK. --*/
            /*-- handling input[type="file"] requests for android API 16+ --*/
            fun openFileChooser(uploadMsg: ValueCallback<Uri>, acceptType: String, capture: String) {
                mFileData = uploadMsg
                val i = Intent(Intent.ACTION_GET_CONTENT)
                i.addCategory(Intent.CATEGORY_OPENABLE)
                i.type = acceptType
//                if (Build.VERSION.SDK_INT >= 18) {
//                    i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
//                }
                startActivityForResult(Intent.createChooser(i, "File Chooser"), mFileReqCode)
            }

            override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams? ): Boolean {
                if (!(filePermission() && Build.VERSION.SDK_INT >= 21)) { return false }

                var acceptType = "*/*" // "image/*" / "video/*" / "image/gif, image/png ..."
                if(fileChooserParams != null && fileChooserParams.acceptTypes.isNotEmpty()) {
                    acceptType = fileChooserParams.acceptTypes.joinToString()
                }
                // Log.d("log","acceptType = $acceptType")

                mFilePath = filePathCallback
                val albumIntent = Intent(Intent.ACTION_PICK)
                // albumIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "video/*")
                albumIntent.type = acceptType
                // albumIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                startActivityForResult(albumIntent, mFileReqCode)
                return true
            }
        }

        this.webView.loadUrl("file:///android_asset/h5/richeditor.html")

// http 允许
// <application
//  ...
//  android:usesCleartextTraffic="true"
//        this.webView.loadUrl("http://192.168.51.127:8080/richeditor")
    }

    @SuppressLint("ObsoleteSdkInt")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (Build.VERSION.SDK_INT >= 21) {

            /*-- if file request cancelled; exited camera. we need to send null value to make future attempts workable --*/
            if (resultCode == Activity.RESULT_CANCELED) {
                if (requestCode == mFileReqCode) {
                    mFilePath?.onReceiveValue(null)
                    return
                }
            }

            /*-- continue if response is positive --*/
            if (resultCode == Activity.RESULT_OK && requestCode == mFileReqCode) {
                if (null == mFilePath) {
                    return
                }
                val results = arrayOf(Uri.parse(intent?.dataString))
                mFilePath?.onReceiveValue(results)
            }
            mFilePath = null
        } else {
            if (requestCode == mFileReqCode) {
                if (null == mFileData) return
                val result = if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
                mFileData?.onReceiveValue(result)
                mFileData = null
            }
        }
    }

    override fun finish() {
        // Log.d("log","finish")
        this.webView.loadUrl("about:blank")
        this.webView.clearCache(true)

        super.finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        if (!this.isHudWaiting) {
            XPopup.Builder(this).asConfirm("😎", "离开将不会保存您编辑的内容，确定离开？") {
                this.finish()
            }.hideCancelBtn().show()
        }
        return true
    }

    /*-- checking and asking for required file permissions --*/
    fun filePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= 23 && (
             ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
          || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA),1)
            return false
        } else {
            return true
        }
    }

    @JavascriptInterface
    fun jsCallAndroid(jsonStr: String) {
        // Log.d("log","jsonStr = $jsonStr")
        val json = GsonBuilder().create().fromJson(jsonStr, JsCallAndroidObject::class.java)
        when(json.cmd) {
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
            "progress" -> {
                runOnUiThread {
                    if (json.msg == "close") {
                        this.mHud.dismiss()
                    } else {
                        this.mHud.setDetailsLabel(json.msg).show()
                    }
                }
            }
            "success" -> {
                runOnUiThread {
                    XPopup.Builder(this)
                        .dismissOnTouchOutside(false)
                        .asConfirm("😀", "发布成功") {
                            this.finish()
                        }
                        .hideCancelBtn()
                        .show()
                }
            }
        }
    }

    // ----------------------------------------
    // 与JS交互时， JS提交JOSN字符串，用此类转换
    class JsCallAndroidObject (
        val cmd: String,
        val msg: String
    )
}