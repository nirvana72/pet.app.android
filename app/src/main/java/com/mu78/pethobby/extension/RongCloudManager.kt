package com.mu78.pethobby.extension

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import com.google.gson.GsonBuilder
import com.mu78.pethobby.MyApplication
import com.mu78.pethobby.R
import com.mu78.pethobby.auth.UserInfoActivity
import com.mu78.pethobby.rongim.ConversationActivity
import com.mu78.pethobby.utils.MyConfig
import com.mu78.pethobby.utils.MySession
import com.mu78.pethobby.utils.MySharedPreferences
import com.mu78.pethobby.utils.MyTools
import com.tapadoo.alerter.Alerter
import io.rong.imkit.RongIM
import io.rong.imlib.RongIMClient
import io.rong.imlib.model.Conversation
import io.rong.imlib.model.Message
import io.rong.imlib.model.MessageContent
import io.rong.imlib.model.UserInfo
import io.rong.message.FileMessage
import io.rong.message.ImageMessage
import io.rong.message.TextMessage
import io.rong.message.VoiceMessage
import io.rong.push.PushType
import io.rong.push.notification.PushMessageReceiver
import io.rong.push.notification.PushNotificationMessage
import okhttp3.*
import java.io.IOException

class RongCloudManager: RongIM.UserInfoProvider, RongIMClient.OnReceiveMessageListener {

    // 正式版时修改这个KEY
    private val appKey = "mgb7ka1nmdugg"

    // 活动期间未读消息计数
    var messageCount = 0

    fun init(app: Application) {

        RongIM.init(app, this.appKey, true)

        // 设置当前用户
        val currentUserAvatarUrl = MyTools.avatarPath(MySession.getInstance().avatar)
        val currentUser = UserInfo(MySession.getInstance().uid.toString(), MySession.getInstance().nickname, Uri.parse(currentUserAvatarUrl))
        RongIM.getInstance().setCurrentUserInfo(currentUser)

        // 消息携带用户信息 * 消息携带用户信息和用户信息提供者不能混用
        // RongIM.getInstance().setMessageAttachedUserInfo(true)

        // 用户信息提供者
        RongIM.setUserInfoProvider(this, true)

        // 收到信息的代理
        RongIM.setOnReceiveMessageListener(this)

        // 发送事件监听
        RongIM.getInstance().setSendMessageListener(object: RongIM.OnSendMessageListener {
            // 发送消息时，把发送者的昵称放入extra中，以便收信息者显示
            override fun onSend(message: Message): Message {
                message.extra = MySession.getInstance().nickname
                return message
            }

            override fun onSent(p0: Message?, p1: RongIM.SentMessageErrorCode?): Boolean {
                return false
            }
        })

        // 会话界面点击事件
        RongIM.setConversationClickListener(object: RongIM.ConversationClickListener {
            override fun onUserPortraitLongClick(p0: Context?, p1: Conversation.ConversationType?, p2: UserInfo?, p3: String?): Boolean {
                // Log.d("log", "onUserPortraitLongClick")
                return false
            }

            override fun onMessageLinkClick(p0: Context?, p1: String?, p2: Message?): Boolean {
                // Log.d("log", "onMessageLinkClick")
                return false
            }

            override fun onMessageLongClick(p0: Context?, p1: View?, p2: Message?): Boolean {
                // Log.d("log", "onMessageLongClick")
                return false
            }
            // 会话界面点击头像
            override fun onUserPortraitClick(p0: Context?, p1: Conversation.ConversationType?, p2: UserInfo?, p3: String?): Boolean {
                // Log.d("log", "onUserPortraitClick")
                val intent = Intent(p0, UserInfoActivity::class.java)
                intent.putExtra("uid", p2?.userId!!.toInt())
                intent.putExtra("fromConversation", true) // 加一个标识，使得打开用户面板时再点会话， 返回当前
                p0?.startActivity(intent)
                return false
            }

            override fun onMessageClick(p0: Context?, p1: View?, p2: Message?): Boolean {
                // Log.d("log", "onMessageClick")
                return false
            }
        })


        var token = MySharedPreferences.getString("RCIMToken","")
        if (token == "") {
            val url = "${MyConfig.APP_API_HOST}/RongCloudApi/register?uid=${MySession.getInstance().uid}"
            val client = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()
            val request = Request.Builder().url(url).build()

            Log.d("log","RongCloudManager url=$url")
            client.newCall(request).enqueue(object: Callback{
                override fun onFailure(call: Call, e: IOException) {
                    Log.d("log","RongCloudManager getToken onFailure")
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    Log.d("log","RongCloudManager getToken body=$body")
                    val apiResult = GsonBuilder().create().fromJson(body, ApiResult::class.java)
                    if (apiResult.ret > 0) {
                        MySharedPreferences.saveValue("RCIMToken", apiResult.token)
                        this@RongCloudManager.connect(apiResult.token)
                    }
                }
            })
        } else {
            this.connect(token)
        }
    }

    private fun connect(token: String) {
        Log.d("log","RongCloudManager connect token = $token")
        RongIM.connect(token, object: RongIMClient.ConnectCallbackEx() {
            override fun onSuccess(userid: String?) {
                Log.d("log","rongcloud 连接成功 userid=$userid")

                RongIM.getInstance().getTotalUnreadCount(object: RongIMClient.ResultCallback<Int>() {
                    override fun onSuccess(count: Int?) {
                        // 设置未读消息状态
                        if (count != null && count > 0) {
                            getInstance().messageCount = count
                            MyNoticeManager.getInstance().setNotice("message", count)
                        }
                    }

                    override fun onError(p0: RongIMClient.ErrorCode?) {
                    }
                })
            }

            override fun OnDatabaseOpened(p0: RongIMClient.DatabaseOpenStatus?) {
                Log.d("log","RongCloudManager OnDatabaseOpened")
            }

            override fun onError(p0: RongIMClient.ErrorCode?) {
                Log.d("log","RongCloudManager onError")
            }

            override fun onTokenIncorrect() {
                Log.d("log","RongCloudManager onTokenIncorrect")
            }
        })
    }

    fun disconnect() {
        MySharedPreferences.remove("RCIMToken")
        RongIM.getInstance().disconnect()
        instance = null
    }

    // 客户端修改个人信息后，刷新融云服务器上的用户信息
    fun refreshUserInfoOnServer(uid: Int) {
        val client = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()
        val request = Request.Builder().url("${MyConfig.APP_API_HOST}/RongCloudApi/update?uid=$uid").build()
        client.newCall(request).enqueue(object :Callback{
            override fun onResponse(call: Call, response: Response) {
            }

            override fun onFailure(call: Call, e: IOException) {
            }
        })
    }

    // 收到信息的代理
    override fun onReceived(message: Message?, p1: Int): Boolean {
        // Log.d("log","RongCloudManager onReceived p1 = $p1")
        val activity = MyApplication.instance.getCurrentActivity()

        if (activity != null) {
            if (activity.localClassName == "rongim.ConversationListActivity") {
                // 会话列表中
                return true
            }
            if (activity.localClassName == "rongim.ConversationActivity") {
                // 会话界面中，但是新消息发送者不是当前聊天对象
                if (message?.senderUserId != ConversationActivity.instance?.targetId) {
                    // 返回图标加小红点提示有新消息
                    ConversationActivity.instance?.setMessageBadge()
                }
                return true
            }
            if (message != null) {
                // 在当前ACTIVITY中弹出新消息提示
                var username = ""
                var txt = "新的消息"
                when {
                    message.content is TextMessage -> {
                        val msg = message.content as TextMessage
                        username = msg.extra
                        txt = msg.content
                    }
                    message.content is ImageMessage -> {
                        val msg = message.content as ImageMessage
                        username = msg.extra
                        txt = "图片"
                    }
                    message.content is VoiceMessage -> {
                        val msg = message.content as VoiceMessage
                        username = msg.extra
                        txt = "语音"
                    }
                    message.content is FileMessage -> {
                        val msg = message.content as FileMessage
                        username = msg.extra
                        txt = "文件"
                    }
                }
                Alerter.create(activity)
                    .setTitle(username)
                    .setText(txt)
                    .setIcon(R.drawable.icon_textsms)
                    .setBackgroundColorRes(R.color.colorPrimary)
                    .setDuration(2000)
                    .show()
            }
        }
        this.messageCount++
        MyNoticeManager.getInstance().setNotice("message", this.messageCount)
        return false
    }

    // 获取用户信息
    override fun getUserInfo(userId: String?): UserInfo {
        val url = "${MyConfig.APP_API_HOST}/users/${userId}"
        Log.d("log", url)
        val client = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("log", "getUserInfo onFailure")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val apiResult = GsonBuilder().create().fromJson(body, ApiUserInfoResult::class.java)
                if (apiResult.ret > 0) {
                    val avatarUri = if(apiResult.user.avatar > 0) {
                        val avatarUrl = MyTools.avatarPath(apiResult.user.avatar)
                        Uri.parse(avatarUrl)
                    } else null
                    val user = UserInfo(apiResult.user.uid.toString(), apiResult.user.nickname, avatarUri)
                    RongIM.getInstance().refreshUserInfoCache(user)
                }
            }
        })

        return UserInfo(userId, "", null)
    }

    // 对应HTTP接口返回结构的类
    class ApiResult(val ret: Int, val msg: String, val token: String)

    class ApiUserInfoResult(val ret: Int, val msg: String, val user: ApiUserInfo)

    class ApiUserInfo(val uid: Int, val nickname: String, val avatar: Int = 0)

    companion object {
        private var instance: RongCloudManager? = null

        fun getInstance(): RongCloudManager {
            if (instance == null) {
                instance =
                    RongCloudManager()
            }
            return instance!!
        }
    }
}

// 后台状态下， 融云负责消息推送
class SealNotificationReceiver: PushMessageReceiver() {
    override fun onNotificationMessageClicked(p0: Context?, p1: PushType?, p2: PushNotificationMessage?): Boolean {
        return false
    }

    override fun onNotificationMessageArrived(p0: Context?, p1: PushType?, p2: PushNotificationMessage?): Boolean {
        return false
    }
}