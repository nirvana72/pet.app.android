package com.mu78.pethobby.auth

import android.os.Bundle
import com.google.gson.GsonBuilder
import com.kaopiz.kprogresshud.KProgressHUD
import com.lxj.xpopup.XPopup
import com.mu78.pethobby.BaseActivity
import com.mu78.pethobby.R
import com.mu78.pethobby.extension.MyOkHttpCallback
import com.mu78.pethobby.extension.TokenInterceptor
import com.mu78.pethobby.utils.MyConfig
import kotlinx.android.synthetic.main.auth_changepassword.*
import kotlinx.android.synthetic.main.auth_changepassword.btnSubmit
import okhttp3.*

class ChangePasswordActivity :BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.title = "修改密码"
        this.setContentView(R.layout.auth_changepassword)

        this.btnSubmit.setOnClickListener {
            this.onSubmit()
        }
    }

    private fun onSubmit () {
        if (etPasswordOld.text.trim() == "" || etPasswordNew.text.trim() == "") {
            return
        }
        val flag = Regex("^.{6,18}\$").matches(etPasswordNew.text.toString())
        if (!flag) {
            this.toast("密码格式不正确, 长度在6~18之间,任意字符")
            return
        }

        if (etPasswordNew.text.toString() != etPasswordConfirm.text.toString()) {
            this.toast("两次密码输入不一致")
            return
        }

        val formBody = FormBody.Builder()
            .add("old", etPasswordOld.text.toString())
            .add("new", etPasswordNew.text.toString())
            .build()
        val client = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()
        val request = Request.Builder().url("${MyConfig.APP_API_HOST}/users/changepassowrd").put(formBody).build()

        val hud = KProgressHUD.create(this).setCancellable(false).setDimAmount(0.4f).show()
        this.isHudWaiting = true
        val that = this

        client.newCall(request).enqueue(object: MyOkHttpCallback(this) {
            override fun onResponse(call: Call, response: Response) {
                super.onResponse(call, response)

                val body = response.body?.string()
                val apiResult = GsonBuilder().create().fromJson(body, ApiResult::class.java)

                if (apiResult.ret > 0) {
                    XPopup.Builder(that)
                        .dismissOnTouchOutside(false)
                        .asConfirm("😀", "修改成功") {
                            runOnUiThread {
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

    // ----------------------------------------
    class ApiResult (
        val ret: Int,
        val msg: String
    )
}