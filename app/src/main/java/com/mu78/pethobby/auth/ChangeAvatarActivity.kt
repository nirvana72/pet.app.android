package com.mu78.pethobby.auth

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.sdk.android.oss.ClientException
import com.alibaba.sdk.android.oss.OSSClient
import com.alibaba.sdk.android.oss.ServiceException
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback
import com.alibaba.sdk.android.oss.common.auth.OSSAuthCredentialsProvider
import com.alibaba.sdk.android.oss.model.PutObjectRequest
import com.alibaba.sdk.android.oss.model.PutObjectResult
import com.google.gson.GsonBuilder
import com.kaopiz.kprogresshud.KProgressHUD
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.lxj.xpopup.XPopup
import com.mu78.pethobby.BaseActivity
import com.mu78.pethobby.R
import com.mu78.pethobby.extension.MyOkHttpCallback
import com.mu78.pethobby.extension.TokenInterceptor
import com.mu78.pethobby.utils.MyConfig
import com.mu78.pethobby.utils.MySession
import com.mu78.pethobby.utils.MyTools
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.auth_changeavatar.*
import kotlinx.android.synthetic.main.auth_changeavatar_cell.view.*
import kotlinx.android.synthetic.main.auth_changeavatar_footer.view.*
import okhttp3.*
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.math.floor

class ChangeAvatarActivity : BaseActivity() {

    private var mUid = -1
    private var mAvatar = -1
    private var mAlbumFile:LocalMedia? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.title = "修改头像"
        this.setContentView(R.layout.auth_changeavatar)

        this.mUid = this.intent.getIntExtra("uid", -1)
        this.mAvatar = this.intent.getIntExtra("avatar", -1)

        if (this.mUid > 0) {
            this.createView()
        } else {
            XPopup.Builder(this).asConfirm("😱", "用户ID获取失败") {}.hideCancelBtn().show()
        }
    }

    // 代码生成导航栏右边链接
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val txt = TextView(this)
        txt.text = "保存"
        txt.setTextColor(Color.parseColor("#2296F3"))
        txt.setPadding(0, 0, 50, 0)
        txt.setOnClickListener {
            if (this.mAvatar < 100) {
                if (this.mAvatar == MySession.getInstance().avatar) {
                    this.finish()
                } else {
                    this.saveAvatar() // 修改了系统头像
                }
            } else {
                if (this.mAlbumFile == null) {
                    this.finish()
                } else {
                    this.saveAvatar() // 修改了自定义头像
                }
            }
        }

        menu?.add(1, Menu.FIRST + 1, 0, "保存")!!.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        menu.getItem(0).actionView = txt
        return true
    }

    private fun saveAvatar() {
        val hud = KProgressHUD.create(this).setCancellable(false).setDimAmount(0.4f).show()
        this.isHudWaiting = true

        if (this.mAvatar > 100) {
            this.putOssObject()
        }

        val formBody = FormBody.Builder()
            .add("key", "avatar")
            .add("val", this.mAvatar.toString())
            .build()
        val client = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()
        val request = Request.Builder().url("${MyConfig.APP_API_HOST}/users/").put(formBody).build()

        val that = this
        client.newCall(request).enqueue(object: MyOkHttpCallback(this) {
            override fun onResponse(call: Call, response: Response) {
                super.onResponse(call, response)

                val body = response.body?.string()
                val apiResult = GsonBuilder().create().fromJson(body, ApiResult::class.java)

                if (apiResult.ret > 0) {
                    // 修改session标识, 以便其它视图获取头像地刷新图片
                    MySession.getInstance().avatar = that.mAvatar
                    MySession.getInstance().time = Date().time.toString()
                    MySession.getInstance().save()

                    XPopup.Builder(that)
                        .dismissOnTouchOutside(false)
                        .asConfirm("😀", "修改成功") {
                                runOnUiThread {
                                    // 返回告知父窗体修改了头像
                                    val backIntent = Intent()
                                    backIntent.putExtra("avatar", that.mAvatar)
                                    setResult(1, backIntent)
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
                hud.dismiss()
                that.isHudWaiting = false
            }
        })
    }

    private fun putOssObject() {
        val mOSSAuthCredentialsProvider = OSSAuthCredentialsProvider(MyConfig.APP_OSS_stsurl)
        val mOSSClient = OSSClient(this, MyConfig.APP_OSS_endpoint, mOSSAuthCredentialsProvider)

        val groupId = (floor(this.mAvatar.toFloat() / 1000) * 1000).toInt()
        val objectKey = "avatar/$groupId/${this.mAvatar}.png"
        val put = PutObjectRequest(MyConfig.APP_OSS_bucket, objectKey, this.mAlbumFile!!.path)
        put.setProgressCallback { _, _, _ ->
            //Log.d("oss", "currentSize: $currentSize totalSize: $totalSize")
        }

        val task = mOSSClient.asyncPutObject(put, object:
            OSSCompletedCallback<PutObjectRequest, PutObjectResult> {

            override  fun onSuccess(request: PutObjectRequest , result: PutObjectResult) {
                Log.d("oss", "UploadSuccess")
            }

            override fun onFailure(request: PutObjectRequest, clientExcepion: ClientException, serviceException: ServiceException) {
                Log.e("oss", "onFailure")
            }
        })
        // task.cancel(); // 可以取消任务。
        task.waitUntilFinished() // 等待任务完成。
    }

    private fun createView() {
        val avatarPath = MyTools.avatarPath(this.mAvatar, false)
        Picasso.get().load(avatarPath).into(this.ivAvatar)

        val mAdapter = RvAdapter()
        val that = this
        mAdapter.setOnItemClickListener(object: RvAdapter.OnItemClickListener {
            override fun onOpenAlbum() {
                PictureSelector.create(this@ChangeAvatarActivity)
                    .openGallery(PictureMimeType.ofImage())
                    .maxSelectNum(1)
                    .isCamera(false)
                    .forResult(PictureConfig.CHOOSE_REQUEST)
            }

            override fun onItemClick(avatar: Int) {
                if (that.mAvatar != avatar) {
                    that.mAvatar = avatar
                    val avatarPath = MyTools.avatarPath(avatar)
                    Picasso.get().load(avatarPath).into(that.ivAvatar)
                }
            }
        })
        this.mRecyclerView.adapter = mAdapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when(requestCode) {
                PictureConfig.CHOOSE_REQUEST -> {
                    val selectList = PictureSelector.obtainMultipleResult(data)
                    if (selectList.count() == 1) {
                        val localMedia = selectList[0]
                        val file = File(localMedia.path)
                        val fis = FileInputStream(file)
                        val size = fis.available()
                        if (size <  1024 * 1024 * 5) {
                            this.mAvatar = MySession.getInstance().uid
                            this.mAlbumFile = localMedia
                            Picasso.get().load("file://${localMedia.path}").into(this.ivAvatar)
                        } else {
                            val s = String.format("%.2f", (size / 1024 / 1024))
                            XPopup.Builder(this).asConfirm("😱", "size:[($s)M],图片文件最多支持5M以内") {}.hideCancelBtn().show()
                        }
                    }
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------------

    class RvAdapter : RecyclerView.Adapter<CellViewHolder> () {
        // 自定义点击事件
        interface OnItemClickListener {
            fun onItemClick(avatar: Int)

            fun onOpenAlbum()
        }

        private var mOnItemClickListener: OnItemClickListener? = null

        fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
            this.mOnItemClickListener = onItemClickListener
        }

        override fun getItemCount(): Int {
            return 34 + 1
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == 34) 1 else 0
        }

        // 没有创建XML 代码实现
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == 0) {
                val cell = inflater.inflate(R.layout.auth_changeavatar_cell, parent, false)
                CellViewHolder(cell)
            } else {
                val cell = inflater.inflate(R.layout.auth_changeavatar_footer, parent, false)
                CellViewHolder(cell)
            }
        }

        override fun onBindViewHolder(holder: CellViewHolder, position: Int) {
            val viewType = getItemViewType(position)
            if(viewType == 0) {
                val avatarPath = MyTools.avatarPath(position + 1)
                Picasso.get().load(avatarPath).into(holder.view.ivAvatar)
                holder.view.setOnClickListener {
                    this.mOnItemClickListener?.onItemClick(position + 1)
                }
            } else {
                holder.view.btnOpenAlbum.setOnClickListener {
                    this.mOnItemClickListener?.onOpenAlbum()
                }
            }
        }

        // 判断头脚，决定占用列
        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            super.onAttachedToRecyclerView(recyclerView)
            val layoutManager = recyclerView.layoutManager as GridLayoutManager
            layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup () {
                override fun getSpanSize(position: Int): Int {
                    val viewType = getItemViewType(position)
                    return if(viewType == 0) 1 else layoutManager.spanCount
                }
            }
        }
    }

    class CellViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    // ----------------------------------------
    class ApiResult (val ret: Int, val msg: String)
}