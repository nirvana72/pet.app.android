package com.mu78.pethobby.article

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.alibaba.sdk.android.oss.OSSClient
import com.alibaba.sdk.android.oss.common.auth.OSSAuthCredentialsProvider
import com.alibaba.sdk.android.oss.model.PutObjectRequest
import com.google.gson.GsonBuilder
import com.kaopiz.kprogresshud.KProgressHUD
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.util.XPopupUtils
import com.mu78.pethobby.BaseActivity
import com.mu78.pethobby.extension.TokenInterceptor
import com.mu78.pethobby.utils.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.article_publish_image.*
import okhttp3.*
import java.io.IOException
import com.alibaba.sdk.android.oss.ServiceException
import com.alibaba.sdk.android.oss.ClientException
import com.alibaba.sdk.android.oss.model.PutObjectResult
import android.util.Log
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import com.mu78.pethobby.extension.MyOkHttpCallback
import java.io.File
import java.io.FileInputStream


class PublishImageActivity :BaseActivity() {

    private var module = Module()
    private lateinit var mHud: KProgressHUD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.title = "发布图文"
        this.setContentView(com.mu78.pethobby.R.layout.article_publish_image)

        this.btnSubmit.setOnClickListener {
            this.submit()
        }

        this.showSelectedImage()
    }

    private fun submit() {
        if (this.module.selectedImgs.count() == 0) {
            this.toast("请至少选择一张图片")
            return
        }

        if (this.etContent.text.trim().toString() == "") {
            this.toast("写点什么吧...")
            return
        }

        val formBody = FormBody.Builder()
            .add("title", this.etContent.text.trim().toString())
            .add("type", "image")
            .build()
        val client = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()
        val request = Request.Builder().url("${MyConfig.APP_API_HOST}/articles/create").post(formBody).build()

        this.mHud = KProgressHUD.create(this).setCancellable(false).setDimAmount(0.4f).setDetailsLabel("正在发布...").show()
        this.isHudWaiting = true
        val that = this

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                that.mHud.dismiss()
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
                    that.mHud.dismiss()
                    that.isHudWaiting = false
                    XPopup.Builder(that).asConfirm("😱 code: ${response.code}", apiResult.msg) {}.hideCancelBtn().show()
                }
            }
        })
    }

    private fun ossUpload() {
        val mOSSAuthCredentialsProvider = OSSAuthCredentialsProvider(MyConfig.APP_OSS_stsurl)
        val mOSSClient = OSSClient(this, MyConfig.APP_OSS_endpoint, mOSSAuthCredentialsProvider)

        for((index, filePath) in this.module.selectedImgs.withIndex()) {
            runOnUiThread {
                this.mHud.setDetailsLabel("上传图片 ${index + 1} / ${this.module.selectedImgs.count()}")
            }

            val ossName = "${index + 1}.jpeg"
            this.module.ossImgs.add(ossName)
            val ossPath = MyTools.ossPath(this.module.Id, this.module.writetime, ossName, false)
            val put = PutObjectRequest(MyConfig.APP_OSS_bucket, ossPath, filePath)
            put.setProgressCallback { request, currentSize, totalSize ->
//                Log.d("oss", "currentSize: $currentSize totalSize: $totalSize")
            }

            val task = mOSSClient.asyncPutObject(put, object: OSSCompletedCallback<PutObjectRequest, PutObjectResult> {
                override  fun onSuccess(request: PutObjectRequest , result: PutObjectResult) {
                    Log.d("oss", "UploadSuccess")
                }

                override fun onFailure(request: PutObjectRequest , clientExcepion: ClientException , serviceException: ServiceException ) {
                    Log.e("oss", "onFailure")
                }
            })
            // task.cancel(); // 可以取消任务。
            task.waitUntilFinished() // 等待任务完成。
        }

        this.finishPost()

    }

    private fun finishPost() {
        runOnUiThread {
            this.mHud.setDetailsLabel("更新发布状态")
        }

        val medias= mutableListOf<String>()
        for(ossName in this.module.ossImgs) {
            medias.add("{\"type\":\"image\",\"name\":\"$ossName\"}")
        }

        val formBody = FormBody.Builder()
            .add("medias", medias.toString()) // [{"type":"image","name":"1.jpeg"},...]
            .build()
        val client = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()
        val request = Request.Builder().url("${MyConfig.APP_API_HOST}/articles/${this.module.Id}/created").put(formBody).build()

        val that = this

        client.newCall(request).enqueue(object: MyOkHttpCallback(this) {
            override fun onResponse(call: Call, response: Response) {
                super.onResponse(call, response)

                val body = response.body?.string()
                val apiResult = GsonBuilder().create().fromJson(body, ApiResult::class.java)

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
                that.mHud.dismiss()
                that.isHudWaiting = false
            }
        })
    }

    private fun showSelectedImage() {
        this.m9gridLayout.removeAllViews()

        val layoutW = XPopupUtils.getWindowWidth(this) - MyTools.dp2px(this, 20f * 2)
        val spacing = 20 // 间隔
        val imgSize = (layoutW - spacing * 2) / 3

        for(index in 0 ..(this.module.selectedImgs.count())) {
            if (index >= 9) break

            val left = index % 3 * (imgSize + spacing)
            val top = (index / 3) * (imgSize + spacing)

            val imageView = ImageView(this)
            val imgLayout = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            imgLayout.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            imgLayout.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
            imgLayout.width = imgSize
            imgLayout.height = imgSize
            imgLayout.setMargins(left, top,0,0)
            this.m9gridLayout.addView(imageView, imgLayout)

            if (index < this.module.selectedImgs.count()) {
                imageView.background = resources.getDrawable(com.mu78.pethobby.R.drawable.style_image_border, null)
                Picasso.get().load("file://${this.module.selectedImgs[index]}").into(imageView)

                val btnDel = ImageView(this)
                btnDel.setImageResource(com.mu78.pethobby.R.drawable.icon_cancel)
                btnDel.setColorFilter(Color.parseColor("#ff0000"))
                val btnDelLayout = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
                btnDelLayout.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                btnDelLayout.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
                val btnSize = MyTools.dp2px(this, 20f)
                btnDelLayout.width = btnSize
                btnDelLayout.height = btnSize
                btnDelLayout.setMargins((left + imgSize - btnSize), top,0,0)
                this.m9gridLayout.addView(btnDel, btnDelLayout)

                // 图集显示
                imageView.setOnClickListener { v ->
                    val imgList = mutableListOf<Any>()
                    for(filePath in this.module.selectedImgs) {
                        imgList.add(filePath)
                    }
                    XPopup.Builder(this)
                        .asImageViewer(v as ImageView, index, imgList ,  { popupView, position ->
                            val ivImg = this.m9gridLayout.getChildAt(position * 2) as ImageView
                            popupView.updateSrcView(ivImg)
                        } , MyXPopupImageLoader(true))
                        .isShowSaveButton(false)
                        .show()
                }

                // 删除按钮事件
                btnDel.setOnClickListener {
                    val ani = AlphaAnimation(1f, 0f)
                    ani.duration = 500
                    val that = this
                    ani.setAnimationListener(object: Animation.AnimationListener {
                        override fun onAnimationRepeat(animation: Animation?) {}
                        override fun onAnimationStart(animation: Animation?) {}
                        override fun onAnimationEnd(animation: Animation?) {
                            that.module.selectedImgs.removeAt(index)
                            that.showSelectedImage()
                        }
                    })
                    imageView.startAnimation(ani)
                }

            } else if(index < 9) {
                var icon = resources.getDrawable(com.mu78.pethobby.R.drawable.icon_add, null)
                val iconSize = MyTools.dp2px(this, (imgSize / 2).toFloat())
                icon = MyTools.zoomDrawable(icon, iconSize, iconSize)

                imageView.setImageDrawable(icon)
                imageView.scaleType = ImageView.ScaleType.CENTER
                imageView.setColorFilter(Color.parseColor("#a3a3a3"))
                imageView.background = resources.getDrawable(com.mu78.pethobby.R.drawable.style_image_border_dash, null)

                if (index == 0) {
                    val txt = TextView(this)
                    txt.text = "图片文件最多支持5M以内"
                    val txtLayout = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
                    txtLayout.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    txtLayout.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    txtLayout.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    txtLayout.setMargins(left + imgSize + 50, 0,0,0)
                    this.m9gridLayout.addView(txt, txtLayout)
                }
                imageView.setOnClickListener {
                    val imgCount = 9 - this.module.selectedImgs.count()
                    PictureSelector.create(this@PublishImageActivity)
                        .openGallery(PictureMimeType.ofImage())
                        .maxSelectNum(imgCount)
                        .isCamera(false)
                        .forResult(PictureConfig.CHOOSE_REQUEST)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when(requestCode) {
                PictureConfig.CHOOSE_REQUEST -> {
                    val selectList = PictureSelector.obtainMultipleResult(data)
                    for (itm in selectList) {
                        val file = File(itm.path)
                        val fis = FileInputStream(file)
                        val size = fis.available()
                        if (size <  1024 * 1024 * 5) {
                            if (this.module.selectedImgs.count() < 9) {
                                this.module.selectedImgs.add(itm.path)
                            }
                        } else {
                            this.toast("最大支持上传5M以内的图片")
                        }
                    }
                    this.showSelectedImage()
                }
            }
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
        var selectedImgs = mutableListOf<String>()
        var ossImgs = mutableListOf<String>()
    }
}