package com.mu78.pethobby.extension

import android.util.Log
import com.google.gson.GsonBuilder
import com.mu78.pethobby.modules.BaseApiResult
import com.mu78.pethobby.utils.MyConfig
import com.mu78.pethobby.utils.MySession
import com.mu78.pethobby.utils.MyTools
import okhttp3.*
import java.nio.charset.Charset

class TokenInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        // Log.d("log","clientDevice = $clientDevice")
        val requestBuilder = request.newBuilder()
        requestBuilder.header("ClientDevice", MyTools.getDeviceDescription())
        if(MySession.isLogin()) {
            requestBuilder.header("Authorization", "Bearer ${MySession.getInstance().token}")
            requestBuilder.header("ClientUid", MySession.getInstance().uid.toString())
        }
        request = requestBuilder.build()

        val response = chain.proceed(request)

        if (MySession.isLogin() && this.isTokenExpired(response)) {
            // Log.d("log","token过期，需要重新刷新")

            val newToken = this.getRefreshToken()
            return if (newToken.ret > 0) {
                MySession.getInstance().token = newToken.token
                MySession.getInstance().refreshtoken = newToken.refreshtoken
                MySession.getInstance().save()
                // Log.d("log","token刷新成功，并已重新赋值")

                var newRequest = chain.request()
                val newRequestBuilder = newRequest.newBuilder()
                newRequestBuilder.header("ClientDevice", MyTools.getDeviceDescription())
                newRequestBuilder.header("Authorization", "Bearer ${MySession.getInstance().token}")
                newRequestBuilder.header("ClientUid", MySession.getInstance().uid.toString())
                newRequest = newRequestBuilder.build()
                chain.proceed(newRequest)
            } else {
                // Log.d("log","token刷新失败")
                MySession.getInstance().clean()
                // 融云服务器断开连接
                RongCloudManager.getInstance().disconnect()

                response
            }
        }
        return response
    }

    private fun isTokenExpired(response: Response): Boolean {
        val body = response.body

        if (body != null) {
            val source = body.source()
            source.request(Long.MAX_VALUE)
            val buffer = source.buffer
            val bodyStr = buffer.clone().readString(Charset.forName("UTF8"))
            // Log.d("log","isTokenExpired.bodyStr = $bodyStr")
            val apiResult = GsonBuilder().create().fromJson(bodyStr, BaseApiResult::class.java)
            if (apiResult.ret  == -99) { // PHP后台，-99表示登录信息超时
                return true
            }
            // Log.d("log","apiResult.ret = ${apiResult.ret}, apiResult.msg = ${apiResult.msg}")
        }

        return false
    }

    private fun getRefreshToken(): ApiResultRefreshToken {
        val client = OkHttpClient.Builder().build()
        // Log.d("log", "refreshToken")
        val request = Request.Builder().url("${MyConfig.APP_API_HOST}/jwt/refresh?refreshToken=${MySession.getInstance().refreshtoken}").build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            val gson = GsonBuilder().create()
            // Log.d("log", "refreshToken body = ${body}")
            return gson.fromJson(body, ApiResultRefreshToken::class.java)
        }
    }

    class ApiResultRefreshToken(
        val ret: Int,
        val msg: String,
        val token: String,
        val refreshtoken: String
    )
}