package com.mu78.pethobby.auth

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import com.google.gson.GsonBuilder
import com.kaopiz.kprogresshud.KProgressHUD
import com.lxj.xpopup.XPopup
import com.mu78.pethobby.BaseActivity
import com.mu78.pethobby.R
import com.mu78.pethobby.extension.RongCloudManager
import com.mu78.pethobby.extension.MyOkHttpCallback
import com.mu78.pethobby.extension.TokenInterceptor
import com.mu78.pethobby.utils.MyConfig
import com.mu78.pethobby.utils.MySession
import com.mu78.pethobby.utils.MyTools
import kotlinx.android.synthetic.main.auth_userinfo_update.*
import okhttp3.*

class UserInfoUpdateActivity : BaseActivity() {

    var attrName: String = ""
    private var attrNameCn: String = ""
    private var attrValue: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.auth_userinfo_update)

        this.attrName = intent.getStringExtra("attrName")
        this.attrNameCn = intent.getStringExtra("attrNameCn")
        this.attrValue = intent.getStringExtra("attrValue")

        this.title = this.attrNameCn

        this.etAttrName.hint = this.attrNameCn
        this.etAttrName.setText(this.attrValue)

        if (this.attrName == "profile") {
            // Log.d("log","profile")
            this.etAttrName.layoutParams.height = MyTools.dp2px(this, 100f)
            this.etAttrName.maxLines = 5
            this.etAttrName.gravity = Gravity.TOP
        }

        btnSubmit.setOnClickListener {
            this.updateSubmit()
        }
    }

    private fun updateSubmit() {
        val v = this.etAttrName.text.trim().toString()
        val formBody = FormBody.Builder()
            .add("key", this.attrName)
            .add("val", v)
            .build()

        val client = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()
        val request = Request.Builder().url("${MyConfig.APP_API_HOST}/users/").put(formBody).build()
        val hud = KProgressHUD.create(this).setCancellable(false).setDimAmount(0.4f).show()
        this.isHudWaiting = true
        val that = this

        client.newCall(request).enqueue(object: MyOkHttpCallback(this) {
            override fun onResponse(call: Call, response: Response) {
                super.onResponse(call, response)

                val body = response.body?.string()
                val apiResult = GsonBuilder().create().fromJson(body, ApiResult::class.java)

                if (apiResult.ret > 0) {
                    if (this@UserInfoUpdateActivity.attrName == "nickename") {
                        // 同步更新融云服务器上的个人信息
                        RongCloudManager.getInstance().refreshUserInfoOnServer(MySession.getInstance().uid)
                    }
                    // 场景切换必须回主线程
                    runOnUiThread {
                        val backIntent = Intent()
                        backIntent.putExtra("attrName", that.attrName)
                        backIntent.putExtra("attrValue", v)
                        setResult(1, backIntent)
                        that.finish()
                    }
                }
                else {
                    XPopup.Builder(that).asConfirm("😱 code: ${response.code}", apiResult.msg) {}.hideCancelBtn().show()
                }
            }

            override fun finally() {
                hud.dismiss()
                that.isHudWaiting = false
            }
        })
    }

    // ----------------------------------------
    class ApiResult (val ret: Int, val msg: String)
}