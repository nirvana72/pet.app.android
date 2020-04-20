package com.mu78.pethobby.index

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.github.nukc.LoadMoreWrapper.LoadMoreWrapper
import com.google.gson.GsonBuilder
import com.lxj.xpopup.XPopup
import com.mu78.pethobby.extension.SmoothScrollLayoutManager
import com.mu78.pethobby.R
import com.mu78.pethobby.article.ArticleViewActivity
import com.mu78.pethobby.auth.UserInfoActivity
import com.mu78.pethobby.common.VideoPlayActivity
import com.mu78.pethobby.extension.MyOkHttpCallback
import com.mu78.pethobby.extension.TokenInterceptor
import com.mu78.pethobby.modules.Article
import com.mu78.pethobby.modules.BaseApiResult
import com.mu78.pethobby.utils.MyConfig
import com.mu78.pethobby.utils.MySession
import com.mu78.pethobby.utils.MyTools
import com.mu78.pethobby.utils.MyXPopupImageLoader
import com.squareup.picasso.Picasso
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.index_home.*
import kotlinx.android.synthetic.main.index_home_cell.view.*
import kotlinx.android.synthetic.main.index_home_cell_header.view.*
import okhttp3.*

class TabHomeFragment : Fragment () {

    private var dataSource = mutableListOf<Article>() // 数据源
    private val pageLimit = 10
    private lateinit var mAdapter: RvAdapter
    private lateinit var mSmoothScrollLayoutManager: SmoothScrollLayoutManager // 顺滑返回顶部
    private var commandsBottomPopup: IndexCommandsBottomPopup? = null // 举报弹窗

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.index_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 平滑返回顶部
        this.mSmoothScrollLayoutManager = SmoothScrollLayoutManager(context)
        mRecyclerView.layoutManager = this.mSmoothScrollLayoutManager
        this.mAdapter = RvAdapter(this.dataSource)
        // CELL事件抛到Adapter， Adapter再抛到Activity
        this.mAdapter.onViewEventListener = object : OnViewEventListener {
            override fun onCommand(cmd: String, position: Int) {
                if (cmd == "pingbi") {
                    this@TabHomeFragment.dataSource.removeAt(position)
                    this@TabHomeFragment.mAdapter.notifyDataSetChanged()
                }
                if (cmd == "jubao") {
                    if (commandsBottomPopup == null) {
                        commandsBottomPopup = IndexCommandsBottomPopup(view.context)
                        commandsBottomPopup?.onCommandListener = object : IndexCommandsBottomPopup.OnCommandListener {
                            override fun onCommand(cmd: String, articleId: Int) {
                                this@TabHomeFragment.jubao(cmd, articleId)
                            }
                        }
                    }
                    commandsBottomPopup?.articleId = dataSource[position].Id
                    XPopup.Builder(view.context)
                        .asCustom(commandsBottomPopup)
                        .show()
                }
            }
            // 根据CELL显示回调，判断是否显示返回顶部按钮
            override fun onShow(position: Int) {
                if (position > 8) {
                    this@TabHomeFragment.mFAB.show()
                } else {
                    this@TabHomeFragment.mFAB.hide()
                }
            }
        }

        // 回滚到顶部
        this.mFAB.setOnClickListener {
            this.mSmoothScrollLayoutManager.smoothScrollToPosition(mRecyclerView, null, 0)
        }

        // 上拉加载三方库
        val mLoadMoreWrapper = LoadMoreWrapper.with(this.mAdapter)
        mLoadMoreWrapper
            .setShowNoMoreEnabled(false)
            .setListener {
                this.fetchData("load") { result ->

                    this.dataSource.addAll(result)

                    // 如果加载数量不足一页，说明没有更多了
                    if (result.size < pageLimit) {
                        it.loadMoreEnabled = false
                    }
                    // 如果总数小于10，不要提示NO MORE
                    mLoadMoreWrapper.setShowNoMoreEnabled(dataSource.count() > 10)

                    activity?.runOnUiThread {
                        this.mAdapter.notifyDataSetChanged()
                    }
                }
            }
            .into(mRecyclerView)

        // 原生下拉加载
        mSwipeRefreshLayout.setOnRefreshListener {
            this.fetchData("refresh") { result ->
                this.dataSource.clear()
                mSwipeRefreshLayout.isRefreshing = false
                if (result.isNotEmpty()) {
                    this.dataSource.addAll(result)
                    activity?.runOnUiThread {
                        this.mAdapter.notifyDataSetChanged()

                        val toast = Toasty.info(context!!, "刷新成功", Toast.LENGTH_SHORT, true)
                        toast.setGravity(Gravity.TOP, 0, 100)
                        toast.show()
                    }
                }
                // 如果总数小于10，不要提示NO MORE
                mLoadMoreWrapper.setShowNoMoreEnabled(dataSource.count() > 10)

                // 如果加载数量不足一页，说明没有更多了
                mLoadMoreWrapper.setLoadMoreEnabled( result.size >= pageLimit )
            }
        }
    }

    private fun fetchData (action: String, callback: (result: Array<Article>) -> Unit) {

        var time = ""

        if (action == "refresh") {
            time = ""
        }
        if (action == "load" && this.dataSource.count() > 0) {
            time = this.dataSource.last().writetime
        }

        val url = "${MyConfig.APP_API_HOST}/articles/?limit=$pageLimit&time=$time"
        val client = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object: MyOkHttpCallback(context!!) {
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val apiResult = GsonBuilder().create().fromJson(body, ApiResult::class.java)

                if (apiResult.ret < 0) {
                    XPopup.Builder(context).asConfirm("😱 code: ${response.code}", apiResult.msg) {}.hideCancelBtn().show()

                    val ary:Array<Article> = arrayOf()
                    callback(ary) // 回调通知
                }
                else {
                    callback(apiResult.articles) // 回调通知
                }
            }
        })
    }

    // 举报
    private fun jubao(content: String, articleId: Int) {

        val formBody = FormBody.Builder()
            .add("article_id", articleId.toString())
            .add("report_uid", MySession.getInstance().uid.toString())
            .add("content", content)
            .build()

        val url = "${MyConfig.APP_API_HOST}/log/report"
        val client = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()
        val request = Request.Builder().url(url).put(formBody).build()
        client.newCall(request).enqueue(object: MyOkHttpCallback(context!!) {
            override fun onResponse(call: Call, response: Response) {
                XPopup.Builder(context).asConfirm("😀", "感谢您的举报，我们会在24小时内审核该内容") {}.hideCancelBtn().show()
            }
        })
    }

    // 对应HTTP接口返回结构的类
    class ApiResult(val ret: Int, val msg: String, val articles: Array<Article>)

    //----------------------------------------------------------------------------------------------------

    class RvAdapter(private val ds: List<Article>) : RecyclerView.Adapter<RecyclerView.ViewHolder> () {

        // 自定义点击事件
        var onViewEventListener: OnViewEventListener? = null

        var showHeader = true

        // 数据源总数
        override fun getItemCount(): Int {
            var count = ds.count()
            if (count == 0) {
                count++ // 没有数据时加上 empty view
            }
            if (showHeader) { count++ } // 加上 header
            return count
        }

        override fun getItemViewType(position: Int): Int {
            if (showHeader && position == 0)
                return CELL_TYPE_HEADER // header
            if (ds.count() == 0)
                return CELL_TYPE_EMPTY // empty

            return  CELL_TYPE_CELL
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)

            return when(viewType) {
                CELL_TYPE_HEADER -> {
                    val cell = inflater.inflate(R.layout.index_home_cell_header, parent, false)
                    HeaderViewHolder(cell)
                }
                CELL_TYPE_EMPTY -> {
                    val cell = inflater.inflate(R.layout.common_cell_empty, parent, false)
                    EmptyViewHolder(cell)
                }
                else -> {
                    val cell = inflater.inflate(R.layout.index_home_cell, parent, false)
                    CellViewHolder(cell)
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val viewType = this.getItemViewType(position)
            if (viewType == CELL_TYPE_HEADER) {
                val headerViewHolder = (holder as HeaderViewHolder)
                headerViewHolder.createView()
            }
            if (viewType == CELL_TYPE_CELL) {
                val pos = if (showHeader) { position - 1 } else position
                val module = ds[pos]
                val cellViewHolder = (holder as CellViewHolder)

                cellViewHolder.onViewEventListener = object : OnViewEventListener {
                    override fun onShow(position: Int) {}
                    // CELL事件抛到Adapter， Adapter再抛到Activity
                    override fun onCommand(cmd: String, position: Int) {
                        onViewEventListener?.onCommand(cmd, pos)
                    }
                }
                cellViewHolder.createView(module, pos)

                if (this.onViewEventListener != null) {
                    this.onViewEventListener!!.onShow(pos)
                }
            }
        }

        companion object {
            private const val CELL_TYPE_HEADER = -1
            private const val CELL_TYPE_EMPTY = -3
            private const val CELL_TYPE_CELL = 1
        }
    }

    //----------------------------------------------------------------------------------------------------
    // 根据CELL显示回调，判断是否显示返回顶部按钮
    interface OnViewEventListener {
        fun onShow(position: Int)

        fun onCommand(cmd: String, position: Int)
    }

    //----------------------------------------------------------------------------------------------------
    // 搜索框逻辑处理
    class HeaderViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun createView() {
            view.mSearchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
                override fun onQueryTextChange(newText: String?): Boolean {
                    return true
                }

                override fun onQueryTextSubmit(query: String?): Boolean {
                    if(query?.trim() != "") {
                        val intent = Intent(view.context, IndexSearchResultActivity::class.java)
                        intent.putExtra("query", query)
                        view.context.startActivity(intent)
                    }
                    return true
                }
            })
        }
    }

    class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view)

    class CellViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        var onViewEventListener:OnViewEventListener? = null
        private val color1 = Color.parseColor("#757575")
        private val color2 = Color.parseColor("#FF0000")
        private var layoutWidth = 500

        init {
            // 不能直接用 view.width 此时宽度还没计算
            this.layoutWidth = view.context.resources.displayMetrics.widthPixels - MyTools.dp2px(view.context, 20f * 2)
        }

        @SuppressLint("SetTextI18n")
        fun createView (module: Article, position: Int) {
            // 头像
            val avatarPath = MyTools.avatarPath(module.avatar)
            Picasso.get().load(avatarPath).into(view.ivAvatar)
            view.ivAvatar.setOnClickListener {
                val intent = Intent(view.context, UserInfoActivity::class.java)
                intent.putExtra("uid", module.authorId)
                view.context.startActivity(intent)
            }

            view.ivSubscribe.visibility = if(module.subscribe) { View.VISIBLE } else { View.INVISIBLE }
            view.tvPostAddr.text = module.postAddr
            view.tvTitle.text = module.title
            view.tvTitle.setOnClickListener {
                val intent = Intent(view.context, ArticleViewActivity::class.java)
                intent.putExtra("articleId", module.Id)
                view.context.startActivity(intent)
            }
            view.tvSubTitle.text = "${module.writetime} | ${module.authorname}"

            // 绑定类型区别部分
            view.mContentLayout.removeAllViews()
            when (module.type) {
                "image" -> this.createImageContent(view.mContentLayout, module)
                "video" -> this.createVideoContent(view.mContentLayout, module)
                "rich" -> this.createRichContent(view.mContentLayout, module)
            }
            // 点赞
            view.tvLauds.text = module.lauds.toString()
            view.ivLauds.setColorFilter( if (module.lauded) color2 else color1 )
            (view.ivLauds.parent as View).setOnClickListener {
                this.setSubmit(module, "laud") {
                    module.lauded = !module.lauded
                    module.lauds++
                    view.ivLauds.startAnimation(MyTools.getIconScaleAnimation())
                    view.ivLauds.setColorFilter(color2)
                    (view.context as Activity).runOnUiThread {
                        view.tvLauds.text = module.lauds.toString()
                    }
                }
            }
            // 弹出评论框
            view.tvComments.text = module.comments.toString()
            (view.tvComments.parent as View).setOnClickListener {
                val v = IndexCommentsPopup(view.context, module.Id, module.authorId)
                XPopup.Builder(view.context)
                    // .moveUpToKeyboard(false) //如果不加这个，评论弹窗会移动到软键盘上面
                    .asCustom(v)
                    .show()
            }
            // 收藏
            view.tvLikes.text = module.likes.toString()
            view.ivLikes.setColorFilter( if (module.liked) color2 else color1 )
            (view.ivLikes.parent as View).setOnClickListener {
                this.setSubmit(module, "like") {
                    module.liked = !module.liked
                    module.likes++
                    view.ivLikes.startAnimation(MyTools.getIconScaleAnimation())
                    view.ivLikes.setColorFilter(color2)
                    (view.context as Activity).runOnUiThread {
                        view.tvLikes.text = module.likes.toString()
                    }
                }
            }

            // 命令按钮组
            view.btnCommand.setOnClickListener {
                val cmdpopup = TabHomeFragmentCommandAttachPopup(view.context)
                cmdpopup.onCommandLinstner = object : TabHomeFragmentCommandAttachPopup.OnCommandLinstner{
                    override fun onCommand(cmd: String) {
                        if (cmd == "pingbi") {
                            val ani = AlphaAnimation(1f, 0f)
                            ani.duration = 500
                            ani.setAnimationListener(object: Animation.AnimationListener {
                                override fun onAnimationRepeat(animation: Animation?) {}
                                override fun onAnimationStart(animation: Animation?) {}
                                override fun onAnimationEnd(animation: Animation?) {
                                    onViewEventListener?.onCommand("pingbi", position)
                                }
                            })
                            view.startAnimation(ani)
                        } else {
                            onViewEventListener?.onCommand(cmd, position)
                        }
                    }
                }
                XPopup.Builder(view.context)
                        .offsetX(-10) //往左偏移10
//                        .offsetY(10)  //往下偏移10
//                        .popupPosition(PopupPosition.Right) //手动指定位置，有可能被遮盖
                    .hasShadowBg(false) // 去掉半透明背景
                    .atView(view.btnCommand)
                    .asCustom(cmdpopup)
                    .show()
            }
        }

        private fun createImageContent(layout: ConstraintLayout, module: Article) {
            val spacing = 10 // 间隔
            var imgW = 0
            var imgH = 0
            var thumb = "" // 缩略图参数

            if (module.images.count() == 1) {
                imgW = this.layoutWidth
                imgH = imgW * 18  / 30
                thumb = MyTools.AliOssThumb.Image1.value
            }
            if (module.images.count() == 2) {
                imgW = (this.layoutWidth - spacing) / 2
                imgH = imgW * 12  / 15
                thumb = MyTools.AliOssThumb.Image2.value
            }
            if (module.images.count() >= 3) {
                imgW = (this.layoutWidth - spacing * 2) / 3
                imgH = imgW
                thumb = MyTools.AliOssThumb.Image3.value
            }
            for ((index, img) in module.images.withIndex()) {
                val left = index % 3 * (imgW + spacing)
                val top = (index / 3) * (imgW + spacing)

                val imageView = ImageView(view.context)
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                val imgLayout = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
                imgLayout.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                imgLayout.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
                imgLayout.width = imgW
                imgLayout.height = imgH
                imgLayout.setMargins(left, top,0,0)
                layout.addView(imageView, imgLayout)

                imageView.setOnClickListener {
                    val fullPathImages = mutableListOf<Any>()
                    for(path in module.images) {
                        val url = MyTools.run { ossPath(module.Id, module.writetime, path) }
                        fullPathImages.add(url)
                    }

                    XPopup.Builder(layout.context)
                        .asImageViewer(it as ImageView, index, fullPathImages,  { popupView, position ->
                            val imgV = layout.getChildAt(position) as ImageView
                            popupView.updateSrcView(imgV)
                        } , MyXPopupImageLoader())
                        .isShowSaveButton(false)
                        .show()
                }
                val url = MyTools.ossPath(module.Id, module.writetime, img)
                imageView.setBackgroundColor(Color.parseColor("#e2e2e2"))
                Picasso.get().load("$url$thumb").into(imageView)
            }
        }

        private fun createVideoContent(layout: ConstraintLayout, module: Article) {
            val imgW = this.layoutWidth
            val imgH = imgW * 9 / 16

            // 缩略图
            val imgLayout = ConstraintLayout.LayoutParams(imgW, imgH)
            imgLayout.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            imgLayout.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
            val url = MyTools.ossPath(module.Id, module.writetime, module.videos[0].fname)
            val imageView = ImageView(view.context)
            layout.addView(imageView, imgLayout)
            imageView.setBackgroundColor(Color.parseColor("#000000"))
            Picasso.get().load("${url}${MyTools.AliOssThumb.Video.value}").into(imageView)

            imageView.setOnClickListener {
                val intent = Intent(view.context, VideoPlayActivity::class.java)
                intent.putExtra("url", url)
                view.context.startActivity(intent)
            }

            // 播放图标
            val size = MyTools.dp2px(view.context, 20f * 2)
            val playiconLayout = ConstraintLayout.LayoutParams(size, size)
            playiconLayout.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            playiconLayout.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            playiconLayout.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            playiconLayout.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            val playiconView = ImageView(view.context)
            playiconView.setImageResource(R.drawable.icon_play_circle)
            playiconView.setColorFilter(Color.parseColor("#ffffff"))
            playiconView.alpha = 0.8f
            layout.addView(playiconView, playiconLayout)

            // 时长
            val durationLayout = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            durationLayout.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            durationLayout.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
            durationLayout.setMargins(0, 0, 20, 20)
            val tvDuration = TextView(view.context)
            tvDuration.text = MyTools.durationString(module.videos[0].duration)
            tvDuration.setTextColor(Color.parseColor("#ffffff"))
            tvDuration.textSize = 14f
            layout.addView(tvDuration, durationLayout)
        }

        @SuppressLint("ResourceType")
        private fun createRichContent(layout: ConstraintLayout, module: Article) {
            val imgW = this.layoutWidth
            val imgH = imgW * 9 / 16

            // 缩略图
            val imgLayout = ConstraintLayout.LayoutParams(imgW, imgH)
            imgLayout.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            imgLayout.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
            val url = MyTools.ossPath(module.Id, module.writetime, module.images[0])
            val imageView = ImageView(view.context)
            imageView.id = 1 // 自动布局需要ID引用
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            layout.addView(imageView, imgLayout)
            imageView.setBackgroundColor(Color.parseColor("#000000"))
            Picasso.get().load("${url}${MyTools.AliOssThumb.Image1.value}").into(imageView)

            // 摘要
            val abstractLayout = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            abstractLayout.topToBottom = imageView.id
            abstractLayout.topMargin = 40
            val tvAbstract = TextView(view.context)
            tvAbstract.text = module.abstract
            tvAbstract.setLineSpacing(13f,1f)
            tvAbstract.setTextColor(Color.parseColor("#333333"))
            layout.addView(tvAbstract, abstractLayout)
            layout.setOnClickListener {
                val intent = Intent(view.context, ArticleViewActivity::class.java)
                intent.putExtra("articleId", module.Id)
                view.context.startActivity(intent)
            }
        }

        private fun setSubmit(module: Article, cmd: String, callback: () -> Unit) {
            if (cmd == "like") {
                if (!MySession.isLogin()) {
                    XPopup.Builder(view.context).asConfirm("😀", "请先登录") {}.hideCancelBtn().show()
                    return
                }
                if (module.liked) {
                    return
                }
            }
            if (cmd == "laud" && module.lauded) {
                return
            }
            val formBody = FormBody.Builder()
                .add("aid", module.Id.toString())
                .add("uid", MySession.getInstance().uid.toString())
                .build()

            val url = "${MyConfig.APP_API_HOST}/articles/${module.Id}/$cmd"
            val client = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()
            val request = Request.Builder().url(url).put(formBody).build()
            client.newCall(request).enqueue(object: MyOkHttpCallback(view.context) {
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    val apiResult = GsonBuilder().create().fromJson(body, BaseApiResult::class.java)
                    if (apiResult.ret > 0) {
                        callback() // 回调通知
                    }
                    else {
                        XPopup.Builder(view.context).asConfirm("😱 code: ${response.code}", apiResult.msg) {}.hideCancelBtn().show()
                    }
                }
            })
        }
    }
}