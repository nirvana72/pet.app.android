package com.mu78.pethobby

import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.os.Bundle
import com.lxj.xpopup.XPopup
import com.mu78.pethobby.modules.CheckUpdateApiResult

class MyApplication : Application () {
    private var currentActivity: Activity? = null
    // 有新的版本 在启动页中判断修改值
    var newVersion: CheckUpdateApiResult? = null

    override fun onCreate() {
        super.onCreate()

        instance = this
        // XPopup 颜色主题
        XPopup.setPrimaryColor(Color.parseColor("#8AD1C3"))

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityPaused(activity: Activity?) {
                // Log.d("log","${activity?.localClassName} onActivityPaused")
            }

            override fun onActivityResumed(activity: Activity?) {
                // Log.d("log","${activity?.localClassName} onActivityResumed")
                this@MyApplication.currentActivity = activity
            }

            override fun onActivityStarted(activity: Activity?) {
                // Log.d("log","${activity?.localClassName} onActivityStarted")
            }

            override fun onActivityDestroyed(activity: Activity?) {
                // Log.d("log","${activity?.localClassName} onActivityDestroyed")
            }

            override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
                // Log.d("log","${activity?.localClassName} onActivitySaveInstanceState")
            }

            override fun onActivityStopped(activity: Activity?) {
                // Log.d("log","${activity?.localClassName} onActivityStopped")
            }

            override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
                // Log.d("log","${activity?.localClassName} onActivityCreated")
            }
        })
    }

    fun getCurrentActivity() : Activity? {
        return this.currentActivity
    }

    companion object {// 伴生对象
        lateinit var instance: MyApplication
        private set
    }
}