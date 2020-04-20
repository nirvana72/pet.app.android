package com.mu78.pethobby.index

import android.content.Intent
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import com.lxj.xpopup.XPopup
import com.mu78.pethobby.R
import com.mu78.pethobby.article.ArticleViewActivity
import com.mu78.pethobby.extension.MyOkHttpCallback
import com.mu78.pethobby.extension.TokenInterceptor
import com.mu78.pethobby.modules.Article
import com.mu78.pethobby.utils.MyConfig
import com.mu78.pethobby.utils.MySession
import com.mu78.pethobby.utils.MyTools
import com.squareup.picasso.Picasso
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.my_likes_cell.view.*
import okhttp3.*

class TabMyTabLikesFragment : TabMyTabBaseFragment() {

    init {
        this.tabIndex = 0
    }

    override fun myConvertJson(body: String?): CommonApiResult {
        val apiResult = GsonBuilder().create().fromJson(body, ApiResult::class.java)
        return CommonApiResult(apiResult.ret, apiResult.msg, apiResult.count, apiResult.list as Array<Any>)
    }

    override fun myCreateAdapter(ds: MutableList<Any>): MyRecyclerViewAdapter {
        val adapter = MyRvAdapter(ds)
        adapter.onItemClickListener = object: OnItemClickListener {
            override fun onItemClick(holder: RecyclerView.ViewHolder, position: Int) {
                val article = ds[position] as LikeArticle
                if (article.status == Article.Status.Reject.value || article.status == Article.Status.Delete.value) {
                    val toast = Toasty.info(context!!, "文章不见了。。。", Toast.LENGTH_SHORT, true)
                    toast.setGravity(Gravity.CENTER, 0, 100)
                    toast.show()
                } else {
                    val intent = Intent(context, ArticleViewActivity::class.java)
                    intent.putExtra("articleId", article.Id)
                    startActivity(intent)
                }
            }

            override fun onItemMenuClick(holder: RecyclerView.ViewHolder, position: Int, cmd: String) {
                if (cmd == "delete") {
                    val article = dataSource[position] as LikeArticle
                    this@TabMyTabLikesFragment.deleteSubmit(article.Id) {
                        val ani = AlphaAnimation(1f, 0f)
                        ani.duration = 500
                        ani.setAnimationListener(object: Animation.AnimationListener {
                            override fun onAnimationRepeat(animation: Animation?) {}
                            override fun onAnimationStart(animation: Animation?) {}
                            override fun onAnimationEnd(animation: Animation?) {
                                dataSource.removeAt(position)
                                mAdapter!!.notifyDataSetChanged()
                                eventListener?.updateNum(tabIndex, -1)
                            }
                        })
                        holder.itemView.startAnimation(ani)
                    }
                }
            }
        }
        return adapter
    }

    private fun deleteSubmit(articleId: Int, callback: () -> Unit) {
        val formBody = FormBody.Builder()
            .add("aid", articleId.toString())
            .add("uid", MySession.getInstance().uid.toString())
            .build()

        val url = "${MyConfig.APP_API_HOST}/articles/$articleId/like"
        val client = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()
        val request = Request.Builder().url(url).delete(formBody).build()
        client.newCall(request).enqueue(object: MyOkHttpCallback(context!!) {
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val apiResult = GsonBuilder().create().fromJson(body, CommonApiResult::class.java)
                if (apiResult.ret > 0) {
                    callback() // 回调通知
                }
                else {
                    XPopup.Builder(context).asConfirm("😱 code: ${response.code}", apiResult.msg) {}.hideCancelBtn().show()
                }
            }
        })
    }

    override fun myGetRequestUrl(): String {
        return "${MyConfig.APP_API_HOST}/articles/${MySession.getInstance().uid}/likes?page=${this.page}&limit=${this.pageLimit}"
    }

    //----------------------------------------------------------------------------------------------------

    class MyRvAdapter(ds: List<Any>): TabMyTabBaseFragment.MyRecyclerViewAdapter(ds) {
        override fun myCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val cell = inflater.inflate(R.layout.my_likes_cell, parent, false)
            return CellViewHolder(cell)
        }

        override fun myBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, module: Any) {
            val cellViewHolder = holder as CellViewHolder
            val item = cellViewHolder.view.findViewById<ConstraintLayout>(R.id.layoutItem)
            item.setOnClickListener {
                this.onItemClickListener?.onItemClick(holder, position)
            }
            val tvDelete = cellViewHolder.view.findViewById<TextView>(R.id.tvDelete)
            tvDelete.setOnClickListener {
                this.onItemClickListener?.onItemMenuClick(holder, position, "delete")
            }
            val m = module as LikeArticle
            cellViewHolder.createView(m)
        }
    }

    //----------------------------------------------------------------------------------------------------

    class CellViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun createView(module: LikeArticle) {
            if (module.status == Article.Status.Reject.value || module.status == Article.Status.Delete.value) {
                view.ivPicture.setImageDrawable(view.context.resources.getDrawable(R.color.color_757575, null))
                view.tvTitle.text = "文章不见了。。。"
                view.tvSubTitle.text = ""
                view.tvTime.text = ""
                view.ivPlayIcon.visibility = View.GONE
            } else {
                if(module.type == "image" || module.type == "rich"){
                    val picUrl = MyTools.ossPath(module.Id, module.writetime, module.images[0])
                    Picasso.get().load("${picUrl}${MyTools.AliOssThumb.Image3.value}").into(view.ivPicture)
                }
                if(module.type == "video"){
                    val picUrl = MyTools.ossPath(module.Id, module.writetime, module.videos[0].fname)
                    Picasso.get().load("${picUrl}${MyTools.AliOssThumb.Video.value}").into(view.ivPicture)
                }

                view.tvTitle.text = module.title
                view.tvSubTitle.text = "😀 ${module.authorname}"
                view.tvTime.text = module.writetime
                view.ivPlayIcon.visibility = if( module.type == "video" ) View.VISIBLE else View.GONE
            }
        }
    }

    //----------------------------------------------------------------------------------------------------
    // 对应HTTP接口返回结构的类
    class ApiResult(val ret: Int, val msg: String, val count: Int, val list: Array<LikeArticle>)

    class LikeArticle(
        val Id: Int,
        val title: String,
        val type: String,
        val writetime: String,
        val status: Int,
//        val lauds: Int,
//        val likes: Int,
//        val comments: Int,
        val authorname: String,
        val videos: Array<Video>,
        val images: Array<String>
    )

    class Video(val fname: String, duration: Int)
}