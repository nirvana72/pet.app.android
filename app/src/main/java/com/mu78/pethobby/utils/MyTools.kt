@file:Suppress("DEPRECATION")

package com.mu78.pethobby.utils

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.graphics.PixelFormat
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import kotlin.math.floor


object MyTools {

    enum class AliOssThumb(val value:String) {
        Avatar("?x-oss-process=style/avatar"),
        Video("?x-oss-process=video/snapshot,t_1000,m_fast"),
        Image1("?x-oss-process=style/thumb300_180"),
        Image2("?x-oss-process=style/thumb150_120"),
        Image3("?x-oss-process=style/thumb100_100")
    }

    // 根据头像ID取OSS上的d存储路径
    fun avatarPath(avatar: Int, thumb: Boolean = true): String {

        var url = if (avatar < 100) {
            "${MyConfig.APP_OSS_HOST}/avatar/$avatar.png"
        } else {
            val groupId = (floor(avatar.toFloat() / 1000) * 1000).toInt()
            "${MyConfig.APP_OSS_HOST}/avatar/$groupId/$avatar.png"
        }

        url += if (thumb) {
            AliOssThumb.Image3.value
        } else {
            AliOssThumb.Avatar.value
        }

        if (avatar > 100 && avatar == MySession.getInstance().avatar) {
            url += "&v=${MySession.getInstance().time}"
        }

        return url
    }

    // 根据文章信息取OSSdd存储路径
    fun ossPath(articleId: Int, writeTime: String, fileName: String, withHost: Boolean = true) : String {
        val yyyymm = writeTime.replace("-", "").subSequence(0, 6)
        var path = "articles/$yyyymm/$articleId/$fileName"
        if (withHost) {
            path = "${MyConfig.APP_OSS_HOST}/$path"
        }
        return path
    }

    fun dp2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    fun px2dp(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    //把过去的秒数转换成 多少时间前
    fun durationString(duration:Int) : String {
        val hour = duration / 3600
        val minute = (duration % 3600) / 60
        val second = duration % 60
        var str = ""
        if (hour > 0) {
            str += "$hour:"
        }

        str += if (minute < 10) {
            "0$minute:"
        } else {
            "$minute:"
        }

        str += if (second < 10) {
            "0$second"
        } else {
            "$second"
        }

        return str
    }

    fun zoomDrawable(drawable: Drawable, w: Int, h: Int): Drawable {
        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight
        val oldbmp = drawableToBitmap(drawable)
        val matrix = Matrix()
        val scaleWidth = w.toFloat() / width
        val scaleHeight = h.toFloat() / height
        matrix.postScale(scaleWidth, scaleHeight)
        val newbmp = Bitmap.createBitmap(
            oldbmp, 0, 0, width, height,
            matrix, true
        )
        return BitmapDrawable(null, newbmp)
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap {
        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight
        val config = if (drawable.opacity != PixelFormat.OPAQUE)
            Bitmap.Config.ARGB_8888
        else
            Bitmap.Config.RGB_565
        val bitmap = Bitmap.createBitmap(width, height, config)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bitmap
    }

    fun getDeviceDescription(): String{
        // android|9|HUAWEI
        return "android|${android.os.Build.VERSION.RELEASE}|${android.os.Build.BRAND}" // ${android.os.Build.VERSION.SDK_INT}
    }

    fun getIconScaleAnimation(): ScaleAnimation {
        val scaleAnimation = ScaleAnimation(1f,1.5f,1f,1.5f,
            Animation.RELATIVE_TO_SELF,0.5f, Animation.RELATIVE_TO_SELF,0.5f)
        scaleAnimation.duration = 200
        return scaleAnimation
    }
}
