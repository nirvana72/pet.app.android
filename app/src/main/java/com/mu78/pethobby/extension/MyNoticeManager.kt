package com.mu78.pethobby.extension

import com.mu78.pethobby.common.MySideMenu
import com.mu78.pethobby.index.IndexActivity

class MyNoticeManager {

    // 只管添加新消息状态， 不管清除消息状态
    // 清除消息逻辑 各组件自行判断处理
    fun setNotice(type: String, num: Int) {
        when (type) {
            "message" -> {
                IndexActivity.instance.setMenuBadge(-1)
                MySideMenu.getInstance().setMessageBadge(num)
            }
            "version" -> {
                IndexActivity.instance.setMenuBadge(-1)
                MySideMenu.getInstance().setVersionBadge(-1)
            }
        }
    }

    companion object {
        private var instance: MyNoticeManager? = null

        fun getInstance(): MyNoticeManager {
            if (instance == null) {
                instance =
                    MyNoticeManager()
            }
            return instance!!
        }
    }
}