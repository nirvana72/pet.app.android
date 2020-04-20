package com.mu78.pethobby.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import com.google.gson.GsonBuilder
import com.lxj.xpopup.XPopup
import com.mu78.pethobby.BaseActivity
import com.mu78.pethobby.R
import com.mu78.pethobby.common.TheUserPubsActivity
import com.mu78.pethobby.extension.MyOkHttpCallback
import com.mu78.pethobby.extension.TokenInterceptor
import com.mu78.pethobby.modules.BaseApiResult
import com.mu78.pethobby.utils.MyConfig
import com.mu78.pethobby.utils.MySession
import com.mu78.pethobby.utils.MyTools
import com.squareup.picasso.Picasso
import io.rong.imkit.RongIM
import kotlinx.android.synthetic.main.auth_userinfo.*
import okhttp3.*
import io.rong.imlib.model.Conversation
import java.util.*


class UserInfoActivity : BaseActivity () {

    private var mUid = -1
    private var mUser: User? = null
    private var mIsMe = false
    private var mFromConversation = false // 是否来自消息界面，如果是点消息按钮直接返回，否则打开消息界面

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.auth_userinfo)

        this.mUid = intent.getIntExtra("uid", -1)
        this.mIsMe = this.mUid == MySession.getInstance().uid
        this.title = if (this.mIsMe) { "个人中心" } else { "用户信息" }
        this.mFromConversation = intent.getBooleanExtra("fromConversation", false)

        if (this.mUid > 0) {
            this.fetchData()
        } else {
            XPopup.Builder(this).asConfirm("😱", "用户ID获取失败") {}.hideCancelBtn().show()
        }
    }

    private fun fetchData() {
        val url = "${MyConfig.APP_API_HOST}/users/${this.mUid}"
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()
        val that = this
        client.newCall(request).enqueue(object: MyOkHttpCallback(this) {
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val apiResult = GsonBuilder().create().fromJson(body, ApiResult::class.java)

                if (apiResult.ret < 0) {
                    XPopup.Builder(that).asConfirm("😱 code: ${response.code}", apiResult.msg) {}.hideCancelBtn().show()
                }
                else {
                    runOnUiThread {
                        that.mUser = apiResult.user
                        that.createView()
                    }
                }
            }
        })
    }

    private fun createView() {
        if(this.mUser == null) {
            return
        }
        val avatarPath = MyTools.avatarPath(this.mUser!!.avatar, false)
        Picasso.get().load(avatarPath).into(this.ivAvatar)

        this.tvProfile.text = this.mUser!!.profile

        this.cellUid.setVal(this.mUser!!.uid.toString())
        this.cellAccount.setVal(this.mUser!!.account)
        this.cellNickname.setVal(this.mUser!!.nickname)
        this.cellEmail.setVal(this.mUser!!.email)
        this.cellMobile.setVal(this.mUser!!.mobile)
        this.cellArticles.setVal(this.mUser!!.articles.toString())
        this.cellFans.setVal(this.mUser!!.fans.toString())

        this.cellArticles.setOnClickListener {
            val intent = Intent(this, TheUserPubsActivity::class.java)
            intent.putExtra("uid", this.mUser!!.uid)
            this.startActivity(intent)
        }

        if (!this.mIsMe) {
            this.setSubscribe()

            this.ivSubscrib.setOnClickListener {
                this.postSubscribe {
                    this.mUser!!.subscribed = !this.mUser!!.subscribed
                    if(this.mUser!!.subscribed) {
                        runOnUiThread {
                            this.toast("关注成功")
                        }
                    }
                    this.setSubscribe()
                }
            }

            this.ivChat.setOnClickListener {
                if (!MySession.isLogin()) {
                    this.toast("请先登录")
                } else {
                    if (this.mFromConversation) {
                        this.finish()
                    }
                    else {
                        RongIM.getInstance().startConversation(this, Conversation.ConversationType.PRIVATE, this.mUid.toString(), this.mUser?.nickname)
                    }
                }
            }

            this.ivChangeAvatar.visibility = View.GONE
            this.cellChangePwd.visibility = View.GONE
            this.cellProfile.visibility = View.GONE
            this.cellNickname.setClickAble(false)
            this.cellEmail.setClickAble(false)
            this.cellMobile.setClickAble(false)
        } else {
            this.ivSubscrib.visibility = View.GONE
            this.ivChat.visibility = View.GONE

            this.ivChangeAvatar.setOnClickListener {
                val intent = Intent(this, ChangeAvatarActivity::class.java)
                intent.putExtra("uid", this.mUser!!.uid)
                intent.putExtra("avatar", this.mUser!!.avatar)
                this.startActivityForResult(intent, 2)
            }

            this.cellNickname.setOnClickListener {
                this.showUpdateIntent("nickname", "昵称", this.mUser!!.nickname)
            }

            this.cellEmail.setOnClickListener {
                this.showUpdateIntent("email", "Email", this.mUser!!.email)
            }

            this.cellMobile.setOnClickListener {
                this.showUpdateIntent("mobile", "手机号码", this.mUser!!.mobile)
            }

            this.cellProfile.setOnClickListener {
                this.showUpdateIntent("profile", "简介", this.mUser!!.profile)
            }

            this.cellChangePwd.setOnClickListener {
                val intent = Intent(this, ChangePasswordActivity::class.java)
                this.startActivity(intent)
            }
        }
    }

    private fun showUpdateIntent(attrName: String, attrNameCn: String, attrValue: String) {
        val intent = Intent(this, UserInfoUpdateActivity::class.java)
        intent.putExtra("attrName", attrName)
        intent.putExtra("attrNameCn", attrNameCn)
        intent.putExtra("attrValue", attrValue)
        this.startActivityForResult(intent, 1)
    }

    private fun postSubscribe(callback: () -> Unit) {
        if (!MySession.isLogin()) {
            this.toast("请先登录")
            return
        }
        if (this.mUser == null) { return }

        val formBody = FormBody.Builder().build()
        val url = "${MyConfig.APP_API_HOST}/subscribes/${this.mUser!!.uid}"
        val client = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()
        val request = if (this.mUser!!.subscribed) {
            Request.Builder().url(url).delete(formBody).build()
        } else {
            Request.Builder().url(url).put(formBody).build()
        }
        client.newCall(request).enqueue(object: MyOkHttpCallback(this) {
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val apiResult = GsonBuilder().create().fromJson(body, BaseApiResult::class.java)
                if (apiResult.ret > 0) {
                    callback() // 回调通知
                }
                else {
                    XPopup.Builder(this@UserInfoActivity).asConfirm("😱 code: ${response.code}", apiResult.msg) {}.hideCancelBtn().show()
                }
            }
        })
    }

    private fun setSubscribe () {
        var icon = R.drawable.icon_star_border
        var color = "#757575"
        if (this.mUser!!.subscribed) {
            icon = R.drawable.icon_star
            color = "#ff0000"
        }
        val drawable = resources.getDrawable(icon, null)
        this.ivSubscrib.setImageDrawable(drawable)
        this.ivSubscrib.setColorFilter(Color.parseColor(color))

        if (this.mUser!!.subscribed) {
            val scaleAnimation = ScaleAnimation(1f,1.5f,1f,1.5f,
                Animation.RELATIVE_TO_SELF,0.5f,
                Animation.RELATIVE_TO_SELF,0.5f)
            scaleAnimation.duration = 200
            this.ivSubscrib.startAnimation(scaleAnimation)
        }
    }

    // Intent 回调
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // UserInfoUpdateActivity 回调
        if(requestCode == 1) {
            val attrName = data?.getStringExtra("attrName")
            val attrValue = data?.getStringExtra("attrValue")
            when(attrName) {
                "nickname" -> {
                    this.mUser?.nickname = attrValue!!
                    this.cellNickname.setVal(attrValue)

                    MySession.getInstance().nickname = attrValue
                    MySession.getInstance().time = Date().time.toString()
                    MySession.getInstance().save()
                }
                "email" -> {
                    this.mUser?.email = attrValue!!
                    this.cellEmail.setVal(attrValue)
                }
                "mobile" -> {
                    this.mUser?.mobile = attrValue!!
                    this.cellMobile.setVal(attrValue)
                }
                "profile" -> {
                    this.mUser?.profile = attrValue!!
                    this.tvProfile.text = attrValue
                }
            }
        }
        // ChangeAvatarActivity 回调
        if (requestCode == 2) {
            val avatar = data?.getIntExtra("avatar", -1)
            if (avatar != null) {
                this.mUser?.avatar = avatar
                val avatarPath = MyTools.avatarPath(avatar, false)
                Picasso.get().load(avatarPath).into(this.ivAvatar)
            }
        }
    }

    //----------------------------------------

    class User(
        val uid: Int,
        var nickname: String,
        val account: String,
        var avatar: Int,
        var mobile: String,
        var email: String,
        var profile: String,
        var subscribed: Boolean,
        val articles: Int,
        val fans: Int
    )

    class ApiResult(
        val ret: Int,
        val msg: String,
        val user: User
    )
}