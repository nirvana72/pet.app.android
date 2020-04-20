package com.mu78.pethobby.index

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import com.lxj.xpopup.XPopup
import com.mu78.pethobby.BaseActivity
import com.mu78.pethobby.R
import com.mu78.pethobby.article.ArticleViewActivity
import com.mu78.pethobby.extension.MyOkHttpCallback
import com.mu78.pethobby.extension.TokenInterceptor
import com.mu78.pethobby.modules.Article
import com.mu78.pethobby.utils.MyConfig
import com.mu78.pethobby.utils.MyTools
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.index_search_result.*
import kotlinx.android.synthetic.main.index_search_result_cell.view.*
import okhttp3.*

class IndexSearchResultActivity : BaseActivity() {

    private var dataSource = mutableListOf<Article>() // 数据源

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.title = "搜索结果"
        this.setContentView(R.layout.index_search_result)

        mRecyclerView.layoutManager = LinearLayoutManager(this)
        val mAdapter = RvAdapter(this.dataSource)
        val that = this
        mAdapter.onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(holder: RecyclerView.ViewHolder, position: Int) {
                val article = dataSource[position]
                val intent = Intent(that, ArticleViewActivity::class.java)
                intent.putExtra("articleId", article.Id)
                startActivity(intent)
            }
        }
        mRecyclerView.adapter = mAdapter

        val query = this.intent.getStringExtra("query")
        this.fetchData(query) { result ->
            if (result.count() <= 0) {
                XPopup.Builder(that).asConfirm("😱", "没有找到搜索内容") {}.hideCancelBtn().show()
            }
            else {
                this.dataSource.addAll(result)
                runOnUiThread {
                    mAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun fetchData (query: String, callback: (result: Array<Article>) -> Unit) {
        val url = "${MyConfig.APP_API_HOST}/articles/?limit=10&search=$query"
        val client = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()
        val request = Request.Builder().url(url).build()
        val that = this
        client.newCall(request).enqueue(object: MyOkHttpCallback(this) {
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val apiResult = GsonBuilder().create().fromJson(body, ApiResult::class.java)

                if (apiResult.ret < 0) {
                    XPopup.Builder(that).asConfirm("😱 code: ${response.code}", apiResult.msg) {}.hideCancelBtn().show()

                    val ary:Array<Article> = arrayOf()
                    callback(ary) // 回调通知
                }
                else {
                    callback(apiResult.articles) // 回调通知
                }
            }
        })
    }

    //----------------------------------------------------------------------------------------------------
    // 对应HTTP接口返回结构的类
    class ApiResult(val ret: Int, val msg: String, val articles: Array<Article>)

    interface OnItemClickListener {
        fun onItemClick(holder: RecyclerView.ViewHolder, position: Int)
    }

    class RvAdapter(private val ds: List<Article>) : RecyclerView.Adapter<RecyclerView.ViewHolder> () {

        // 自定义点击事件
        var onItemClickListener: OnItemClickListener? = null

        override fun getItemCount(): Int {
            return ds.count()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val cell = inflater.inflate(R.layout.index_search_result_cell, parent, false)
            return CellViewHolder(cell)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val cellViewHolder = (holder as CellViewHolder)
            val item = cellViewHolder.view.findViewById<ConstraintLayout>(R.id.layoutItem)
            item.setOnClickListener {
                this.onItemClickListener?.onItemClick(holder, position)
            }
            val module = ds[position]
            cellViewHolder.createView(module)
        }
    }

    class CellViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun createView (module: Article) {
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