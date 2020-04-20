package com.mu78.pethobby

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import es.dmoral.toasty.Toasty

open class BaseActivity : AppCompatActivity() {

    var isHudWaiting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val drawable = resources.getDrawable(R.drawable.icon_chevron_left, null)
        drawable.setTint(Color.parseColor("#2296F3"))
        supportActionBar?.setHomeAsUpIndicator(drawable)

        this.title = this.intent.getStringExtra("title")
    }

    // 返回按钮事件
    override fun onSupportNavigateUp(): Boolean {
        if (!this.isHudWaiting) {
            this.finish()
        }
        return true
    }

    // 强制等待时不让按返回键
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode == KeyEvent.KEYCODE_BACK && this.isHudWaiting){
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    protected fun toast(text: String) {
        val toast = Toasty.warning(this, text, Toast.LENGTH_SHORT, true)
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }
}