package com.mu78.pethobby.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.google.gson.GsonBuilder
import com.kaopiz.kprogresshud.KProgressHUD
import com.lxj.xpopup.XPopup
import com.mu78.pethobby.BaseActivity
import com.mu78.pethobby.extension.MyOkHttpCallback
import com.mu78.pethobby.extension.TokenInterceptor
import com.mu78.pethobby.utils.MyConfig
import com.mu78.pethobby.utils.MySession
import com.mu78.pethobby.utils.MySharedPreferences
import kotlinx.android.synthetic.main.auth_reg.*
import okhttp3.*
import java.util.*


class RegActivity : BaseActivity() {

    private var regType = "account"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(com.mu78.pethobby.R.layout.auth_reg)

        this.title = "注册"

        btnRegTypeAccount.setOnClickListener {
            this.setRegType("account")
        }

        btnRegTypeEmail.setOnClickListener {
            this.setRegType("email")
        }

        btnRegTypeMobile.setOnClickListener {
            this.setRegType("mobile")
        }

        btnSubmit.setOnClickListener {
            this.reg()
        }

        this.setRegType("account")
    }

    private fun reg() {
        if (!this.verify(this.regType)) {
            return
        }
        if (!this.verify("password")) {
            return
        }

        var account = ""
        when(this.regType) {
            "account" ->  account = etAccount.text.toString()
            "email" ->  account = etEmail.text.toString()
            "mobile" ->  account = etMobile.text.toString()
        }

        val formBody = FormBody.Builder()
            .add("account", account)
            .add("reg_type", this.regType)
            .add("pwd", etPassword.text.toString())
            .build()
        val client = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()
        val request = Request.Builder().url("${MyConfig.APP_API_HOST}/users/").post(formBody).build()

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
                    MySession.getInstance().time = Date().time.toString()
                    MySession.getInstance().save()

                    MySharedPreferences.saveValue("lastloginname", account)

                    XPopup.Builder(that)
                        .dismissOnTouchOutside(false)
                        .asConfirm("😀", "欢迎您 ${apiResult.nickname}") {
                            runOnUiThread {
                                val backIntent = Intent()
                                setResult(99, backIntent)
                                that.finish()
                            }
                        }
                        .hideCancelBtn()
                        .show()
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

    private fun verify(elname: String) : Boolean {
        var flag = false
        var msg = ""
        when (elname) {
            "account" -> {
                flag = Regex("^[a-zA-Z0-9_\u4e00-\u9fa5]{6,18}\$").matches(etAccount.text.toString())
                if (!flag) { msg = "账号格式不正确, 6~18位中英文数字下划线" }
            }
            "email" -> {
                flag = Regex("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*\$").matches(etEmail.text.toString())
                if (!flag) { msg = "邮箱格式不正确" }
            }
            "mobile" -> {
                flag = Regex("^(13[0-9]|14[0-9]|15[0-9]|166|17[0-9]|18[0-9]|19[8|9])\\d{8}\$").matches(etMobile.text.toString())
                if (!flag) { msg = "手机格式不正确" }
            }
            "password" -> {
                flag = Regex("^.{6,18}\$").matches(etPassword.text.toString())
                if (!flag) { msg = "密码格式不正确, 长度在6~18之间,任意字符" }
                if (flag) {
                    flag = etPassword.text.toString() == etPasswordConfirm.text.toString()
                    if (!flag) { msg = "两次密码输入不一至" }
                }
            }
        }
        if(!flag) {
            XPopup.Builder(this).asConfirm("😱", msg) {}.hideCancelBtn().show()
        }
        return flag
    }

    private fun setRegType(tp: String) {
        this.regType = tp
        etAccount.visibility = View.GONE
        etEmail.visibility = View.GONE
        etMobile.visibility = View.GONE
        btnRegTypeAccount.background.setTint(Color.parseColor("#757575"))
        btnRegTypeEmail.background.setTint(Color.parseColor("#757575"))
        btnRegTypeMobile.background.setTint(Color.parseColor("#757575"))

        when(tp) {
            "account" -> {
                btnRegTypeAccount.background.setTint(Color.parseColor("#4CAF50"))
                etAccount.visibility = View.VISIBLE
            }
            "email" -> {
                btnRegTypeEmail.background.setTint(Color.parseColor("#4CAF50"))
                etEmail.visibility = View.VISIBLE
            }
            "mobile" -> {
                btnRegTypeMobile.background.setTint(Color.parseColor("#4CAF50"))
                etMobile.visibility = View.VISIBLE
            }
        }
    }

    // ----------------------------------------
    class ApiResult (
        val ret: Int,
        val msg: String,
        val uid: Int,
        val avatar: Int,
        val nickname: String,
        val token: String,
        val refreshtoken: String
    )
}