package com.mu78.pethobby.extension

import android.content.Context
import com.lxj.xpopup.XPopup
import okhttp3.*
import java.io.IOException

open class MyOkHttpCallback(val ctx: Context) : Callback {

    override fun onFailure(call: Call, e: IOException) {
        XPopup.Builder(ctx).asConfirm("😱", e.message) {}.hideCancelBtn().show()
        this.finally()
    }

    override fun onResponse(call: Call, response: Response) {
        this.finally()
    }

    open fun finally() {}
}