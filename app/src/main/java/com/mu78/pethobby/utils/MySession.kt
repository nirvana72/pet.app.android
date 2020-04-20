package com.mu78.pethobby.utils

import com.google.gson.GsonBuilder
import okhttp3.*
import java.io.IOException

class MySession {

    var uid: Int = -1
    var nickname: String = ""
    var avatar: Int = 1
    var token: String = ""
    var refreshtoken: String = ""
    var time: String = ""
    var role: String = "custom"

    fun save() {
        val json = GsonBuilder().create().toJson(this)
        MySharedPreferences.saveValue("session", json)
    }

    fun clean() {
        this.uid = -1
        this.time = ""
        MySharedPreferences.remove("session")
    }

    interface InputCallBack {
        fun refreshed(newToken: String)
    }

    class ApiResultRefreshToken(val ret: Int, val msg: String, val token: String, val refreshtoken: String)

    companion object {
        @JvmStatic
        private var instance: MySession? = null

        @JvmStatic
        fun getInstance(): MySession {

            if (instance == null) {
                val sessionStr = MySharedPreferences.getString("session", "")
                instance = if (sessionStr == "") {
                    MySession()
                } else {
                    GsonBuilder().create().fromJson(sessionStr, MySession::class.java)
                }
            }

            return instance!!
        }

        fun isLogin(): Boolean {
            return getInstance().uid > 0
        }

        fun callRefreshToken(callback: InputCallBack) {
            val client = OkHttpClient.Builder().build()
            val request = Request.Builder().url("${MyConfig.APP_API_HOST}/jwt/refresh?refreshToken=${getInstance().refreshtoken}").build()
            client.newCall(request).enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {

                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    val apiResult = GsonBuilder().create().fromJson(body, ApiResultRefreshToken::class.java)

                    if (apiResult.ret > 0) {
                        getInstance().token = apiResult.token
                        getInstance().refreshtoken = apiResult.refreshtoken
                        getInstance().save()
                        callback.refreshed(apiResult.token)
                    }
                }
            })
        }
    }
}