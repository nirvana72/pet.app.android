package com.mu78.pethobby.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import com.google.gson.GsonBuilder
import com.kaopiz.kprogresshud.KProgressHUD
import com.lxj.xpopup.XPopup
import com.mu78.pethobby.BaseActivity
import com.mu78.pethobby.MyApplication
import com.mu78.pethobby.R
import com.mu78.pethobby.extension.RongCloudManager
import com.mu78.pethobby.extension.MyOkHttpCallback
import com.mu78.pethobby.extension.TokenInterceptor
import com.mu78.pethobby.utils.MyConfig
import com.mu78.pethobby.utils.MySession
import com.mu78.pethobby.utils.MySharedPreferences
import kotlinx.android.synthetic.main.auth_login.*
import okhttp3.*
import java.util.*

class LoginActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.auth_login)

        this.title = "登录"

        val lastloginname = MySharedPreferences.getString("lastloginname", "")
        this.etAccount.setText(lastloginname)

        btnSubmit.setOnClickListener {
            this.doLogin()
        }
    }

    private fun doLogin() {
        val account = etAccount.text.trim()
        val password = etPassword.text

        if (account == "") {
            XPopup.Builder(this).asConfirm("😱", "账号不能为空") {}.hideCancelBtn().show()
            return
        }

        if (password.length < 6) {
            XPopup.Builder(this).asConfirm("😱", "密码格式不正确, 长度在6~18位任意字符") {}.hideCancelBtn().show()
            return
        }

        val formBody = FormBody.Builder()
            .add("account", account.toString())
            .add("pwd", password.toString())
            .build()
        val client = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()
        val request = Request.Builder().url("${MyConfig.APP_API_HOST}/users/login").post(formBody).build()

        val hud = KProgressHUD.create(this).setCancellable(false).setDimAmount(0.4f).show()
        this.isHudWaiting = true
        val that = this

        client.newCall(request).enqueue(object: MyOkHttpCallback(this) {
            override fun onResponse(call: Call, response: Response) {
                super.onResponse(call, response)

                val body = response.body?.string()
                val apiResult = GsonBuilder().create().fromJson(body, ApiResult::class.java)

                if (apiResult.ret > 0) {
                    MySession.getInstance().uid = apiResult.uid
                    MySession.getInstance().nickname = apiResult.nickname
                    MySession.getInstance().avatar = apiResult.avatar
                    MySession.getInstance().token = apiResult.token
                    MySession.getInstance().refreshtoken = apiResult.refreshtoken
                    MySession.getInstance().role = apiResult.role
                    MySession.getInstance().time = Date().time.toString()
                    MySession.getInstance().save()

                    MySharedPreferences.saveValue("lastloginname", account.toString())

                    // 同时登录融云服务器
                    RongCloudManager.getInstance().init(MyApplication.instance)

                    // 场景切换必须回主线程
                    runOnUiThread {
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

    // 代码生成导航栏右边链接
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(1, Menu.FIRST + 1, 0, "注册")!!.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        val item = menu.getItem(0)
        val txt = TextView(this)
        txt.text = "注册"
        txt.setTextColor(Color.parseColor("#2296F3"))
        txt.setPadding(0, 0, 50, 0)
        txt.setOnClickListener {
            val intent = Intent(this, RegActivity::class.java)
            this.startActivityForResult(intent, 1)
        }
        item.actionView = txt
        return true
    }

    // Intent 回调
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 1 && resultCode == 99) {
            this.finish()
        }
    }

    // ----------------------------------------
    class ApiResult (
        val ret: Int,
        val msg: String,
        val uid: Int,
        val avatar: Int,
        val nickname: String,
        val role: String,
        val token: String,
        val refreshtoken: String
    )
}