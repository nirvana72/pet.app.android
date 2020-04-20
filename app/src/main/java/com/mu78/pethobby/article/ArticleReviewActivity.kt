package com.mu78.pethobby.article

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.enums.PopupAnimation
import com.mu78.pethobby.BaseActivity
import com.mu78.pethobby.R
import com.mu78.pethobby.extension.MyOkHttpCallback
import com.mu78.pethobby.extension.TokenInterceptor
import com.mu78.pethobby.modules.Article
import com.mu78.pethobby.utils.MyConfig
import kotlinx.android.synthetic.main.article_review.*
import kotlinx.android.synthetic.main.article_review_cell.view.*
import okhttp3.*

class ArticleReviewActivity : BaseActivity() {

    private var dataSource = mutableListOf<Article>() // 数据源

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.title = "待审列表"
        this.setContentView(R.layout.article_review)

        this.mRecyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = RvAdapter(this.dataSource)
        val that = this
        adapter.onItemClickListener = object : OnItemClickListener {
            override fun onItemMenuClick(view: View, position: Int, cmd: String) {
                when(cmd) {
                    "publish" -> {
                        that.reviewPublish(view, position)
                    }
                    "reject" -> {
                        that.reviewReject(view, position)
                    }
                }
            }

            override fun onItemClick(view: View, position: Int) {
                val article = dataSource[position]
                val intent = Intent(that, ArticleViewActivity::class.java)
                intent.putExtra("articleId", article.Id)
                startActivity(intent)
            }
        }
        this.mRecyclerView.adapter = adapter

        this.fetchData()
    }

    private fun fetchData () {
        val url = "${MyConfig.APP_API_HOST}/articles/reviewlist"
        val client = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()
        val request = Request.Builder().url(url).build()
        val that = this
        client.newCall(request).enqueue(object: MyOkHttpCallback(this) {
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val apiResult = GsonBuilder().create().fromJson(body, ApiResult::class.java)

                if (apiResult.ret < 0) {
                    XPopup.Builder(that).asConfirm("😱 code: ${response.code}", apiResult.msg) {}.hideCancelBtn().show()
                }
                else {
                    that.dataSource.addAll(apiResult.list)
                    runOnUiThread {
                        that.mRecyclerView.adapter?.notifyDataSetChanged()
                    }
                }
            }
        })
    }

    private fun reviewPublish(view: View, position: Int) {
        // Log.d("log", "reviewPass")
        val formBody = FormBody.Builder()
            .add("cmd", "publish")
            .build()

        val module = this.dataSource[position]
        val url = "${MyConfig.APP_API_HOST}/articles/${module.Id}/review"
        val client = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()
        val request = Request.Builder().url(url).put(formBody).build()
        val that = this
        client.newCall(request).enqueue(object: MyOkHttpCallback(this) {
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val apiResult = GsonBuilder().create().fromJson(body, com.mu78.pethobby.modules.BaseApiResult::class.java)
                if (apiResult.ret < 0) {
                    XPopup.Builder(that).asConfirm("😱 code: ${response.code}", apiResult.msg) {}.hideCancelBtn().show()
                }
                else {
                    val ani = AlphaAnimation(1f, 0f)
                    ani.duration = 500
                    ani.setAnimationListener(object: Animation.AnimationListener {
                        override fun onAnimationRepeat(animation: Animation?) {}
                        override fun onAnimationStart(animation: Animation?) {}
                        override fun onAnimationEnd(animation: Animation?) {
                            that.dataSource.removeAt(position)
                            that.mRecyclerView.adapter?.notifyDataSetChanged()
                        }
                    })
                    view.startAnimation(ani)
                }
            }
        })
    }

    private fun reviewReject(view: View, position: Int) {
        // Log.d("log", "reviewReject")
        val module = this.dataSource[position]
        val rejectView = ArticleReviewRejectPopView(this, module.Id)
        val that = this
        rejectView.listener = object: ArticleReviewRejectPopView.OnRejectListener {
            override fun onReject() {
                val ani = AlphaAnimation(1f, 0f)
                ani.duration = 500
                ani.setAnimationListener(object: Animation.AnimationListener {
                    override fun onAnimationRepeat(animation: Animation?) {}
                    override fun onAnimationStart(animation: Animation?) {}
                    override fun onAnimationEnd(animation: Animation?) {
                        that.dataSource.removeAt(position)
                        that.mRecyclerView.adapter?.notifyDataSetChanged()
                    }
                })
                view.startAnimation(ani)
            }
        }
        XPopup.Builder(this)
            .popupAnimation(PopupAnimation.ScaleAlphaFromCenter)
            // .moveUpToKeyboard(false) //如果不加这个，评论弹窗会移动到软键盘上面
            .asCustom(rejectView)
            .show()
    }
    //----------------------------------------------------------------------------------------------------
    // 对应HTTP接口返回结构的类
    class ApiResult(val ret: Int, val msg: String, val list: Array<Article>)

    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int)
        fun onItemMenuClick(view: View, position: Int, cmd: String)
    }
    class RvAdapter(private val ds: List<Article>): RecyclerView.Adapter<RecyclerView.ViewHolder> (){
        // 自定义点击事件
        var onItemClickListener: OnItemClickListener? = null

        override fun getItemCount(): Int {
            return ds.count()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val cell = inflater.inflate(R.layout.article_review_cell, parent, false)
            return CellViewHolder(cell)
        }
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val cellViewHolder = (holder as CellViewHolder)
            val item = cellViewHolder.view.findViewById<ConstraintLayout>(R.id.layoutItem)
            item.setOnClickListener {
                this.onItemClickListener?.onItemClick(holder.view, position)
            }
            val tvPass = cellViewHolder.view.findViewById<TextView>(R.id.tvPass)
            tvPass.setOnClickListener {
                this.onItemClickListener?.onItemMenuClick(holder.view, position, "publish")
            }
            val tvReject = cellViewHolder.view.findViewById<TextView>(R.id.tvReject)
            tvReject.setOnClickListener {
                this.onItemClickListener?.onItemMenuClick(holder.view, position, "reject")
            }
            val module = ds[position]
            cellViewHolder.createView(module)
        }
    }

    class CellViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun createView(module: Article) {
            when(module.type) {
                "image" -> {
                    view.ivPlayIcon.setImageResource(R.drawable.icon_photo)
                    view.ivPlayIcon.setColorFilter(Color.parseColor("#019688"))
                }
                "video" -> {
                    view.ivPlayIcon.setImageResource(R.drawable.icon_play_circle)
                    view.ivPlayIcon.setColorFilter(Color.parseColor("#F44436"))
                }
                "rich" -> {
                    view.ivPlayIcon.setImageResource(R.drawable.icon_description)
                    view.ivPlayIcon.setColorFilter(Color.parseColor("#9B26B0"))
                }
            }
            view.tvTitle.text = module.title
            view.tvSubTitle.text = "😀${module.authorname}"
            view.tvWriteTime.text = module.writetime
        }
    }
}