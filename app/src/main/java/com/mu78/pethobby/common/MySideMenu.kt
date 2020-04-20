package com.mu78.pethobby.common

import android.content.Context
import android.content.Intent
import android.view.View
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.DrawerPopupView
import com.mu78.pethobby.AboutUsActivity
import com.mu78.pethobby.MyApplication
import com.mu78.pethobby.R
import com.mu78.pethobby.article.ArticleReviewActivity
import com.mu78.pethobby.auth.LoginActivity
import com.mu78.pethobby.auth.UserInfoActivity
import com.mu78.pethobby.extension.RongCloudManager
import com.mu78.pethobby.utils.MySession
import com.mu78.pethobby.utils.MyTools
import com.squareup.picasso.Picasso
import io.rong.imkit.RongIM
import io.rong.imlib.model.Conversation
import kotlinx.android.synthetic.main.index_sidemenu.view.*
import q.rorbin.badgeview.Badge
import q.rorbin.badgeview.QBadgeView

class MySideMenu(private val ctx: Context) : DrawerPopupView(ctx) {

    // session状态码， 如果与AppSession不一至则更新视图
    private var sessionCode = "null"
    private var messageBadge: Badge? = null
    private var versionBadge: Badge? = null

    override fun getImplLayoutId(): Int {
        return R.layout.index_sidemenu
    }

    override fun onCreate() {
        super.onCreate()

        // 个人中心
        menu_account.setOnClickListener {
            this.dismiss()
            if (MySession.isLogin()) {
                val intent = Intent(context, UserInfoActivity::class.java)
                intent.putExtra("uid", MySession.getInstance().uid)
                context.startActivity(intent)
            } else {
                val intent = Intent(context, LoginActivity::class.java)
                context.startActivity(intent)
            }
        }

        // 关于我们
        menu_aboutus.setOnClickListener {
            this.dismiss()
            val intent = Intent(context, AboutUsActivity::class.java)
            context.startActivity(intent)
        }

        // 消息
        menu_message.setOnClickListener {
            this.dismiss()
            if (MySession.isLogin()) {
                this.setMessageBadge(0)
                RongIM.getInstance().startSubConversationList(ctx, Conversation.ConversationType.PRIVATE)
            } else {
                val intent = Intent(context, LoginActivity::class.java)
                context.startActivity(intent)
            }
        }

        // 文章审核
        menu_review.setOnClickListener {
            this.dismiss()
            val intent = Intent(context, ArticleReviewActivity::class.java)
            context.startActivity(intent)
        }

        // 登录
        menu_login.setOnClickListener {
            this.dismiss()
            val intent = Intent(context, LoginActivity::class.java)
            context.startActivity(intent)
        }

        // 退出
        menu_logout.setOnClickListener {
            this.dismiss()

            // 融云服务器断开连接
            RongCloudManager.getInstance().disconnect()

            MySession.getInstance().clean()
            XPopup.Builder(context).asConfirm("😀", "账号已退出") {}.hideCancelBtn().show()
        }

        this.refreshStatus()
    }

    fun refreshStatus() {
        // 强制刷新或登录状态改变时刷新
        if (this.sessionCode != MySession.getInstance().time) {
            this.sessionCode = MySession.getInstance().time

            val isLogin = MySession.isLogin()

            this.ivAvatar.visibility = if (isLogin) View.VISIBLE else View.GONE
            this.tvNickName.visibility = if (isLogin) View.VISIBLE else View.GONE
            this.tvRole.visibility = if (isLogin && MySession.getInstance().role != "custom") View.VISIBLE else View.GONE
            this.layout_login.visibility = if (isLogin) View.GONE else View.VISIBLE
            this.layout_logout.visibility = if (isLogin) View.VISIBLE else View.GONE
            this.layout_review.visibility = if (isLogin && MySession.getInstance().role == "admin") View.VISIBLE else View.GONE
            if (isLogin) {
                val avatarPath = MyTools.avatarPath(MySession.getInstance().avatar)
                Picasso.get().load(avatarPath).into(ivAvatar)

                this.tvNickName.text = MySession.getInstance().nickname
                this.tvRole.text = MySession.getInstance().role
            }
        }
    }

    fun setMessageBadge(num: Int) {
        MyApplication.instance.getCurrentActivity()?.runOnUiThread {
            if (this.messageBadge == null) {
                this.messageBadge = QBadgeView(ctx).bindTarget(this.menu_message)
            }
            this.messageBadge?.badgeNumber = num
        }
    }

    fun setVersionBadge(num: Int) {
        MyApplication.instance.getCurrentActivity()?.runOnUiThread {
            if (this.versionBadge == null) {
                this.versionBadge = QBadgeView(ctx).bindTarget(this.menu_aboutus)
            }
            this.versionBadge?.badgeNumber = num
        }
    }

    companion object {
        private var instance: MySideMenu? = null

        fun getInstance(): MySideMenu {
            return instance!!
        }

        fun init(ctx: Context) {
            instance = MySideMenu(ctx)
        }
    }
}