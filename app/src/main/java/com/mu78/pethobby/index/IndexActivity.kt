package com.mu78.pethobby.index

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.google.gson.GsonBuilder
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BasePopupView
import com.mu78.pethobby.MyApplication
import com.mu78.pethobby.R
import com.mu78.pethobby.common.MySideMenu
import com.mu78.pethobby.extension.MyNoticeManager
import com.mu78.pethobby.extension.MyViewPagerAdapter
import com.mu78.pethobby.extension.RongCloudManager
import com.mu78.pethobby.modules.CheckUpdateApiResult
import com.mu78.pethobby.utils.MyConfig
import com.mu78.pethobby.utils.MySession
import kotlinx.android.synthetic.main.index.*
import model.UpdateConfig
import okhttp3.*
import q.rorbin.badgeview.Badge
import q.rorbin.badgeview.QBadgeView
import update.UpdateAppUtils
import java.io.IOException

class IndexActivity : AppCompatActivity() {

    // ViewPager 的功能适配器
    private lateinit var viewPagerAdapter: MyViewPagerAdapter

    private var sideMenuPopView: BasePopupView? = null

    private var menuBadge:Badge? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.setContentView(R.layout.index)

        this.supportActionBar?.hide()

        // 初始化侧边导航对象，context对象生命周期与当前Activity一致
        MySideMenu.init(this)

        this.app_bar_more.setOnClickListener {
            this.setMenuBadge(0)
            if (this.sideMenuPopView == null) {
                this.sideMenuPopView = XPopup.Builder(this).asCustom(MySideMenu.getInstance())
            }
            MySideMenu.getInstance().refreshStatus()
            this.sideMenuPopView?.show()
        }

        this.setupTabLayout()

        instance = this

        // 融云IM
        if (MySession.isLogin()) {
            RongCloudManager.getInstance().init(MyApplication.instance)
        }

        this.checkUpdate()
    }

    private fun checkUpdate() {
        // 删除已安装APK
        UpdateAppUtils.getInstance().deleteInstalledApk()

        val url = "${MyConfig.APP_API_HOST}/server/version"
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient.Builder().build()
        val that = this
        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val apiResult = GsonBuilder().create().fromJson(body, CheckUpdateApiResult::class.java)
                val info = that.packageManager.getPackageInfo(that.packageName, 0)
                if (info.versionName != apiResult.version) {
                    // 设置全局状态
                    MyApplication.instance.newVersion = apiResult
                    // 更新内容
                    var content = ""
                    for (str in apiResult.info) {
                        content += "$str \n"
                    }
                    runOnUiThread {
                        // 通知中心，有新版本
                        MyNoticeManager.getInstance().setNotice("version", -1)

                        // 更新配置
                        val updateConfig = UpdateConfig().apply {
                            // force = true
                            // checkWifi = true
                            // needCheckMd5 = true
                            // isShowNotification = true
                            // notifyImgRes = R.drawable.ic_logo
                            apkSavePath = Environment.getExternalStorageDirectory().absolutePath +"/teprinciple"
                            apkSaveName = "app${apiResult.version}.apk"
                        }
                        // 弹出自动更新对话框
                        UpdateAppUtils.getInstance()
                            .apkUrl(apiResult.apkurl)
                            .updateConfig(updateConfig)
                            .updateTitle("发现新版本: ${apiResult.version}")
                            .updateContent(content)
                            .update()
                    }
                }
            }
        })
    }

    private fun setupTabLayout() {
        // 生成三个菜单
        for(index in 1..3) {
            val tab = mTabLayout.newTab()
            tab.setCustomView(R.layout.index_tabitem)
            val ivIcon = tab.customView?.findViewById<ImageView>(R.id.ivIcon)
            val tvTitle = tab.customView?.findViewById<TextView>(R.id.tvTitle)
            when(index) {
                1 -> {
                    ivIcon?.setImageResource(R.drawable.icon_home)
                    tvTitle?.text = "主页"
                }
                2 -> {
                    ivIcon?.setImageResource(R.drawable.icon_library_add)
                    tvTitle?.text = "发布"
                }
                3 -> {
                    ivIcon?.setImageResource(R.drawable.icon_account_circle)
                    tvTitle?.text = "我的"
                }
            }

            mTabLayout.addTab(tab)
        }

        this.viewPagerAdapter = MyViewPagerAdapter(supportFragmentManager)
        this.viewPagerAdapter.addFragment(TabHomeFragment())
        this.viewPagerAdapter.addFragment(TabPublishFragment())
        this.viewPagerAdapter.addFragment(TabMyFragment())
        mViewPager.adapter = this.viewPagerAdapter
        mViewPager.setScroll(false)

        // 自定义TAB样式时不要使用下面的方法绑定viewPager，否则会默认使用系统样式
        // tabs.setupWithViewPager(viewPager)

        this.setTabStatus(mTabLayout.getTabAt(0)!!, true)

        mTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(p0: TabLayout.Tab?) {
            }

            override fun onTabUnselected(p0: TabLayout.Tab?) {
                setTabStatus(p0!!, false)
            }

            override fun onTabSelected(p0: TabLayout.Tab?) {
                setTabStatus(p0!!, true)
                mViewPager.currentItem = p0.position
            }
        })
    }

    // 动态设置TAB ITEM的样式
    private fun setTabStatus(tab: TabLayout.Tab, isActive : Boolean) {
        val v = tab.customView!!
        val tvTitle = tab.customView?.findViewById<TextView>(R.id.tvTitle)
        val set  = AnimatorSet()
        if (isActive) {
            tvTitle?.visibility = View.VISIBLE
            set.playTogether(listOf<ObjectAnimator>(
                ObjectAnimator.ofFloat(v,"translationY",0f,-25f),
                ObjectAnimator.ofFloat(v,"alpha",0.5f,1f)
            ))
        }
        else {
            tvTitle?.visibility = View.INVISIBLE
            set.playTogether(listOf<ObjectAnimator>(
                ObjectAnimator.ofFloat(v,"translationY",-25f,0f),
                ObjectAnimator.ofFloat(v,"alpha",1f,0.5f)
            ))
        }
        set.setDuration(300).start()
    }

    fun setMenuBadge(num: Int) {
        if (this.menuBadge == null) {
            this.menuBadge = QBadgeView(this).bindTarget(this.app_bar_more)
        }
        this.menuBadge?.badgeNumber = num
    }

    companion object {
        lateinit var instance: IndexActivity
            private set
    }
}