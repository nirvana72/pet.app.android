package com.mu78.pethobby

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.View
import kotlinx.android.synthetic.main.aboutus.*
import kotlinx.android.synthetic.main.index_sidemenu.*
import kotlinx.android.synthetic.main.index_sidemenu.view.*
import model.UpdateConfig
import q.rorbin.badgeview.QBadgeView
import update.UpdateAppUtils

class AboutUsActivity: BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.title = "关于我们"
        this.setContentView(R.layout.aboutus)

        try {
            val info = this.packageManager.getPackageInfo(this.packageName, 0)
            this.tvVersion.text = "版本:${info.versionName}"
        }
        catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        // 有新版本
        if (MyApplication.instance.newVersion != null) {
            var newVersion = MyApplication.instance.newVersion
            this.tvNewVersion.visibility = View.VISIBLE
            QBadgeView(this).bindTarget(this.tvNewVersion).badgeNumber = -1
            this.tvNewVersion.setOnClickListener {
                // 更新内容
                var content = ""
                for (str in newVersion!!.info) {
                    content += "$str \n"
                }
                // 更新配置
                val updateConfig = UpdateConfig().apply {
                    // force = true
                    // checkWifi = true
                    // needCheckMd5 = true
                    // isShowNotification = true
                    // notifyImgRes = R.drawable.ic_logo
                    apkSavePath = Environment.getExternalStorageDirectory().absolutePath +"/teprinciple"
                    apkSaveName = "app${newVersion.version}.apk"
                }
                // 弹出自动更新对话框
                UpdateAppUtils.getInstance()
                    .apkUrl(newVersion.apkurl)
                    .updateConfig(updateConfig)
                    .updateTitle("发现新版本: ${newVersion.version}")
                    .updateContent(content)
                    .update()
            }
        }

    }
}