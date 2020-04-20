package com.mu78.pethobby.index

import android.content.Intent
import android.graphics.Color
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import com.mu78.pethobby.R
import com.mu78.pethobby.article.ArticleViewActivity
import com.mu78.pethobby.modules.Article
import com.mu78.pethobby.utils.MyConfig
import com.mu78.pethobby.utils.MySession
import com.mu78.pethobby.utils.MyTools
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.my_my_cell.view.*

class TabMyTabMyFragment : TabMyTabBaseFragment() {
    init {
        this.tabIndex = 2
    }

    override fun myGetRequestUrl(): String {
        return "${MyConfig.APP_API_HOST}/articles/${MySession.getInstance().uid}/users?page=${this.page}&limit=${this.pageLimit}"
    }

    override fun myConvertJson(body: String?): CommonApiResult {
        val apiResult = GsonBuilder().create().fromJson(body, ApiResult::class.java)
        return CommonApiResult(apiResult.ret, apiResult.msg, apiResult.count, apiResult.list as Array<Any>)
    }

    override fun myCreateAdapter(ds: MutableList<Any>): MyRecyclerViewAdapter {
        val adapter = MyRvAdapter(ds)
        adapter.onItemClickListener = object: OnItemClickListener {
            override fun onItemClick(holder: RecyclerView.ViewHolder, position: Int) {
                val article = ds[position] as Article
                val intent = Intent(context, ArticleViewActivity::class.java)
                intent.putExtra("articleId", article.Id)
                startActivity(intent)
            }

            override fun onItemMenuClick(holder: RecyclerView.ViewHolder, position: Int, cmd: String) {}
        }
        return adapter
    }
    //----------------------------------------------------------------------------------------------------

    class MyRvAdapter(ds: List<Any>): TabMyTabBaseFragment.MyRecyclerViewAdapter(ds) {
        override fun myCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val cell = inflater.inflate(R.layout.my_my_cell, parent, false)
            return CellViewHolder(cell)
        }

        override fun myBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, module: Any) {
            val cellViewHolder = holder as CellViewHolder
            val item = cellViewHolder.view.findViewById<ConstraintLayout>(R.id.layoutItem)
            item.setOnClickListener {
                this.onItemClickListener?.onItemClick(holder, position)
            }
            val m = module as Article
            cellViewHolder.createView(m)
        }
    }

    //----------------------------------------------------------------------------------------------------

    class CellViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        init {
            val drawableLauds = view.context.resources.getDrawable(R.drawable.icon_thumb_up, null)
            drawableLauds.setBounds(0,0,40,40)
            drawableLauds.setTint(Color.parseColor("#909090"))
            view.tvLauds.setCompoundDrawables(drawableLauds, null, null, null)

            val drawableComments = view.context.resources.getDrawable(R.drawable.icon_comment, null)
            drawableComments.setBounds(0,5,40,40)
            drawableComments.setTint(Color.parseColor("#909090"))
            view.tvComments.setCompoundDrawables(drawableComments, null, null, null)

            val drawableLikes = view.context.resources.getDrawable(R.drawable.icon_star, null)
            drawableLikes.setBounds(0,0,40,40)
            drawableLikes.setTint(Color.parseColor("#909090"))
            view.tvLikes.setCompoundDrawables(drawableLikes, null, null, null)
        }
        fun createView(module: Article) {
            if(module.type == "image" || module.type == "rich"){
                val picUrl = MyTools.ossPath(module.Id, module.writetime, module.images[0])
                Picasso.get().load("${picUrl}${MyTools.AliOssThumb.Image3.value}").into(view.ivPicture)
            }
            if(module.type == "video"){
                val picUrl = MyTools.ossPath(module.Id, module.writetime, module.videos[0].fname)
                Picasso.get().load("${picUrl}${MyTools.AliOssThumb.Video.value}").into(view.ivPicture)
            }

            if (module.status != 2) {
                var statusCn = "<span style='color:#FF9800'>【拒】</span>"
                if (module.status == 1) {
                    statusCn = "<span style='color:#03A9F4'>【审】</span>"
                }
                if (module.status == 10) {
                    statusCn = "<span style='color:#F44336'>【拒】</span>"
                }
                view.tvTitle.text = Html.fromHtml("$statusCn${module.title}", Html.FROM_HTML_MODE_COMPACT)
            } else {
                view.tvTitle.text = module.title
            }

            view.tvSubTitle.text = module.writetime
            view.ivPlayIcon.visibility = if( module.type == "video" ) View.VISIBLE else View.GONE
            view.tvLauds.text = module.lauds.toString()
            view.tvComments.text = module.comments.toString()
            view.tvLikes.text = module.likes.toString()
        }
    }

    //----------------------------------------------------------------------------------------------------
    // 对应HTTP接口返回结构的类
    class ApiResult(val ret: Int, val msg: String, val count: Int, val list: Array<Article>)
}