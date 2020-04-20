package com.mu78.pethobby.article

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.alibaba.sdk.android.oss.ClientException
import com.alibaba.sdk.android.oss.OSSClient
import com.alibaba.sdk.android.oss.ServiceException
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback
import com.alibaba.sdk.android.oss.common.auth.OSSAuthCredentialsProvider
import com.alibaba.sdk.android.oss.model.PutObjectRequest
import com.alibaba.sdk.android.oss.model.PutObjectResult
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.gson.GsonBuilder
import com.kaopiz.kprogresshud.KProgressHUD
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.util.XPopupUtils
import com.mu78.pethobby.BaseActivity
import com.mu78.pethobby.R
import com.mu78.pethobby.common.VideoPlayActivity
import com.mu78.pethobby.extension.MyOkHttpCallback
import com.mu78.pethobby.extension.TokenInterceptor
import com.mu78.pethobby.utils.MyConfig
import com.mu78.pethobby.utils.MyTools
import io.rong.imkit.utilities.videocompressor.VideoCompress
import kotlinx.android.synthetic.main.article_publish_video.*
import okhttp3.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException

@Suppress("INACCESSIBLE_TYPE")
class PublishVideoActivity :BaseActivity() {

    private var module = Module()
    private lateinit var mHudWait: KProgressHUD
    private lateinit var mHudCompress: KProgressHUD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.title = "发布视频"
        this.setContentView(R.layout.article_publish_video)

        this.btnSubmit.setOnClickListener {
            this.submit()
        }

        this.initView()
    }

    private fun initView (){
        val layoutW = XPopupUtils.getWindowWidth(this) - MyTools.dp2px(this, 20f * 2)
        this.ivVideoThumb.layoutParams.height = layoutW * 9 / 16

        // 添加按钮
        val spacing = 20 // 间隔
        val imgSize = (layoutW - spacing * 2) / 3
        this.ivAddVideo.layoutParams.width = imgSize
        this.ivAddVideo.layoutParams.height = imgSize
        // 添加按钮中间的+号，缩放
        var icon = resources.getDrawable(R.drawable.icon_add, null)
        val iconSize = MyTools.dp2px(this, (imgSize / 2).toFloat())
        icon = MyTools.zoomDrawable(icon, iconSize, iconSize)
        this.ivAddVideo.setImageDrawable(icon)
        // 添加视频事件
        this.ivAddVideo.setOnClickListener {
            if (this.filePermission() && Build.VERSION.SDK_INT >= 21) {
                PictureSelector.create(this@PublishVideoActivity)
                    .openGallery(PictureMimeType.ofVideo())
                    .maxSelectNum(1)
                    .isCamera(false)
                    .forResult(PictureConfig.CHOOSE_REQUEST)
            }
        }
        // 删除视频
        this.ivDelVideo.setOnClickListener {
            this.module.pathOrigin = ""
            this.module.pathCompress = ""
            this.module.sizeCompress = 0
            this.updateView()
        }
        // 预览视频
        this.ivVideoThumb.setOnClickListener {
            if (this.module.pathCompress != "") {
                val intent = Intent(this, VideoPlayActivity::class.java)
                intent.putExtra("url", this.module.pathCompress)
                this.startActivity(intent)
            }
        }
    }
    // 添加视频回调
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when(requestCode) {
                PictureConfig.CHOOSE_REQUEST -> {
                    val selectList = PictureSelector.obtainMultipleResult(data)
                    if (selectList.count() == 1) {
                        val video = selectList[0]
                        this.module.pathOrigin = video.path
                        this.module.duration = (video.duration / 1000).toInt()
                        this.module.fileExt = video.path.split("\\.".toRegex()).last()
                        // 压缩视频
                        this.compressVideo()
                    }
                }
            }
        }
    }

    // 压缩视频
    private fun compressVideo () {
        val fileNameFull = this.module.pathOrigin.split("/".toRegex()).last()
        val dotIndex = fileNameFull.lastIndexOf(".")
        val fileName = fileNameFull.substring(0, dotIndex)
        val fileExt = fileNameFull.split("\\.".toRegex()).last()

        val moviePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).path
        this.module.pathCompress = "${moviePath}/${fileName}_compress.${fileExt}"
        // Log.d("log", "newPath = ${this.module.pathCompress}")

        this.mHudCompress = KProgressHUD.create(this)
            .setStyle(KProgressHUD.Style.BAR_DETERMINATE)
            .setLabel("正在压缩视频")
            .setMaxProgress(100)
            .setCancellable(false)

        val that = this

        VideoCompress.compressVideo(this.module.pathOrigin, this.module.pathCompress, object : VideoCompress.CompressListener {
            override fun onSuccess() {
                // Log.d("log", "compressVideo onSuccess")
                that.mHudCompress.dismiss()

                // Glide 获取视频缩略图
                val option = RequestOptions()
                    .centerCrop()
                    .placeholder(R.color.color_757575)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                Glide.with(that)
                    .load(that.module.pathCompress)
                    .apply(option)
                    .into(that.ivVideoThumb)

                val file = File(that.module.pathCompress)
                val fis = FileInputStream(file)
                that.module.sizeCompress = fis.available()

                that.updateView()
            }

            override fun onFail() {
                that.mHudCompress.dismiss()
                XPopup.Builder(that).asConfirm("😱", "视频压缩过程出错了！！！") {}.hideCancelBtn().show()
            }

            override fun onProgress(p0: Float) {
                that.mHudCompress.setProgress(p0.toInt())
            }

            override fun onStart() {
                that.mHudCompress.show()
            }
        })
    }


    // 添加或删除视频后，更新界面
    @SuppressLint("SetTextI18n")
    private fun updateView() {
        if (this.module.pathCompress != "") {
            // Glide 获取视频缩略图
            val option = RequestOptions()
                .centerCrop()
                .placeholder(R.color.color_757575)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
            Glide.with(this@PublishVideoActivity)
                .load(this.module.pathCompress)
                .apply(option)
                .into(this.ivVideoThumb)
            // 时长
            val sizeTxt = String.format("%.2f", (this.module.sizeCompress / 1024 / 1024).toFloat())
            val duration = MyTools.durationString(this.module.duration)
            this.tvDuration.text = "时长: $duration / 大小: $sizeTxt M"
        }
        this.addLayout.visibility = if (this.module.pathCompress == "") { View.VISIBLE } else { View.GONE }
        this.videoLayout.visibility = if (this.module.pathCompress == "") { View.GONE } else { View.VISIBLE }
    }
    // 提交发布
    private fun submit(){
        if (this.module.pathCompress == "") {
            this.toast("未添加视频")
            return
        }
        if (this.module.sizeCompress > (1024 * 1024 * 20)) {
            this.toast("视频文件最大支持20M")
            return
        }
        if (this.etContent.text.trim().toString() == "") {
            this.toast("写点什么吧...")
            return
        }

        val formBody = FormBody.Builder()
            .add("title", this.etContent.text.trim().toString())
            .add("type", "video")
            .build()
        val client = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()
        val request = Request.Builder().url("${MyConfig.APP_API_HOST}/articles/create").post(formBody).build()

        this.mHudWait = KProgressHUD.create(this).setCancellable(false).setDimAmount(0.4f).setDetailsLabel("正在发布...").show()
        this.isHudWaiting = true
        val that = this
        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                that.mHudWait.dismiss()
                that.isHudWaiting = false
                XPopup.Builder(that).asConfirm("😱", e.message) {}.hideCancelBtn().show()
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val apiResult = GsonBuilder().create().fromJson(body, ApiResult::class.java)

                if (apiResult.ret > 0) {
                    that.module.Id = apiResult.Id
                    that.module.writetime = apiResult.writetime

                    that.ossUpload()
                }
                else {
                    that.mHudWait.dismiss()
                    that.isHudWaiting = false
                    XPopup.Builder(that).asConfirm("😱 code: ${response.code}", apiResult.msg) {}.hideCancelBtn().show()
                }
            }
        })
    }

    private fun ossUpload() {
        runOnUiThread {
            this.mHudWait.setDetailsLabel("上传视频文件...")
        }

        val mOSSAuthCredentialsProvider = OSSAuthCredentialsProvider(MyConfig.APP_OSS_stsurl)
        val mOSSClient = OSSClient(this, MyConfig.APP_OSS_endpoint, mOSSAuthCredentialsProvider)

        this.module.ossName = "1.${this.module.fileExt}"
        val ossPath = MyTools.ossPath(this.module.Id, this.module.writetime, this.module.ossName, false)
        val put = PutObjectRequest(MyConfig.APP_OSS_bucket, ossPath, this.module.pathCompress)
        put.setProgressCallback { request, currentSize, totalSize ->
            //                Log.d("oss", "currentSize: $currentSize totalSize: $totalSize")
        }

        val task = mOSSClient.asyncPutObject(put, object:
            OSSCompletedCallback<PutObjectRequest, PutObjectResult> {
            override  fun onSuccess(request: PutObjectRequest, result: PutObjectResult) {
                Log.d("oss", "UploadSuccess")
            }

            override fun onFailure(request: PutObjectRequest, clientExcepion: ClientException, serviceException: ServiceException) {
                Log.e("oss", "onFailure")
            }
        })
        // task.cancel(); // 可以取消任务。
        task.waitUntilFinished() // 等待任务完成。

        this.finishPost()
    }

    private fun finishPost() {
        runOnUiThread {
            this.mHudWait.setDetailsLabel("更新发布状态...")
        }

        val media= "[{\"type\":\"video\",\"name\":\"${this.module.ossName}\",\"duration\":\"${this.module.duration}\"}]"

        val formBody = FormBody.Builder()
            .add("medias", media) // [{"type":"video","name":"1.mp4"},...]
            .build()
        val client = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()
        val request = Request.Builder().url("${MyConfig.APP_API_HOST}/articles/${this.module.Id}/created").put(formBody).build()

        val that = this
        client.newCall(request).enqueue(object: MyOkHttpCallback(this) {
            override fun onResponse(call: Call, response: Response) {
                super.onResponse(call, response)

                val body = response.body?.string()
                val apiResult = GsonBuilder().create().fromJson(body, PublishImageActivity.ApiResult::class.java)

                if (apiResult.ret > 0) {
                    XPopup.Builder(that)
                        .dismissOnTouchOutside(false)
                        .asConfirm("😀", "发布成功") {
                            runOnUiThread {
                                that.finish()
                            }
                        }
                        .hideCancelBtn()
                        .show()
                }
                else {
                    XPopup.Builder(that).asConfirm("😱 code: ${response.code}", apiResult.msg) {}.hideCancelBtn().show()
                }
            }

            override fun finally() {
                that.mHudWait.dismiss()
                that.isHudWaiting = false
            }
        })
    }

    /*-- checking and asking for required file permissions --*/
    @SuppressLint("ObsoleteSdkInt")
    private fun filePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 23 && (
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),1)
            false
        } else {
            true
        }
    }

    // ----------------------------------------

    class ApiResult (
        val ret: Int,
        val msg: String,
        val status: Int,
        val Id: Int,
        val writetime: String
    )
    class Module {
        var Id:Int = -1
        var title:String = ""
        var writetime:String = ""
        var duration:Int = 0
        var ossName:String = ""
        var pathOrigin:String = ""
        var pathCompress:String = ""
        var sizeCompress:Int = 0
        var fileExt:String = ""
    }
}