package com.mu78.pethobby.index

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.github.nukc.LoadMoreWrapper.LoadMoreWrapper
import com.google.gson.GsonBuilder
import com.lxj.xpopup.XPopup
import com.mu78.pethobby.R
import com.mu78.pethobby.extension.MyOkHttpCallback
import com.mu78.pethobby.extension.SmoothScrollLayoutManager
import com.mu78.pethobby.extension.TokenInterceptor
import com.mu78.pethobby.modules.BaseApiResult
import com.mu78.pethobby.utils.MySession
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.index_my_tabview.*
import okhttp3.*

open class TabMyTabBaseFragment : Fragment() {

    private lateinit var mSmoothScrollLayoutManager: SmoothScrollLayoutManager

    protected lateinit var mAdapter: MyRecyclerViewAdapter
    protected var dataSource = mutableListOf<Any>()
    protected val pageLimit = 15 // 每次加载数量
    protected var page = 1 // 页码
    protected var tabIndex = 0

    var eventListener: MyTabEventListener? = null

    // 三个懒加载判断参数
    private var mLazyLoadHasCreateView = false
    private var mLazyLoadHasSetupView = false
    private var mLazyLoadIsVisibleToUser = false

    //----------------------------------------------------------------------------------------------------

    open fun myConvertJson(body: String?): CommonApiResult {
        TODO("not implemented")
    }

    open fun myCreateAdapter(ds: MutableList<Any>): MyRecyclerViewAdapter {
        TODO("not implemented")
    }

    open fun myGetRequestUrl(): String {
        TODO("not implemented")
    }

    //----------------------------------------------------------------------------------------------------
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        // Log.d("log","TabMyTabBaseFragment${this.tabIndex}.setUserVisibleHint isVisibleToUser=$isVisibleToUser")
        // 此处为了实现 viewpager 懒加载
        this.mLazyLoadIsVisibleToUser = isVisibleToUser
        if (this.mLazyLoadHasCreateView && !this.mLazyLoadHasSetupView && isVisibleToUser) {
            this.setupView()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Log.d("log","TabMyTabBaseFragment${this.tabIndex}.onCreateView")

        this.mLazyLoadHasCreateView = true

        return inflater.inflate(R.layout.index_my_tabview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // Log.d("log","TabMyTabBaseFragment${this.tabIndex}.onViewCreated")

        super.onViewCreated(view, savedInstanceState)
        // 此处为了实现 viewpager 懒加载
        if (this.mLazyLoadIsVisibleToUser && !this.mLazyLoadHasSetupView) {
            this.setupView()
        }
    }

    private fun setupView(){
        this.mLazyLoadHasSetupView = true

        // Log.d("log","TabMyTabBaseFragment${this.tabIndex}.setupView")
        this.mSmoothScrollLayoutManager = SmoothScrollLayoutManager(context) // 平滑返回顶部
        mRecyclerView.layoutManager = this.mSmoothScrollLayoutManager
        this.mAdapter = this.myCreateAdapter(this.dataSource)
        this.mAdapter.onViewShowListener = object : OnViewShowListener {
            override fun onShow(position: Int) {
                if (position > 15) {
                    mFAB.show()
                } else {
                    mFAB.hide()
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
                this.fetchData("load") { count, result ->
                    if (this.page == 1) {
                        this.eventListener?.updateNum(this.tabIndex, count)
                    }

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
            this.fetchData("refresh") { count, result ->
                if (this.page == 1) {
                    this.eventListener?.updateNum(this.tabIndex, count)
                }

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

    private fun fetchData (action: String, callback: (count: Int, result: Array<Any>) -> Unit) {
        // Log.d("log","TabMyTabBaseFragment${this.tabIndex}.fetchData")
        if (!MySession.isLogin()) {
            val toast = Toasty.info(context!!, "请先登录", Toast.LENGTH_SHORT, true)
            toast.setGravity(Gravity.CENTER, 0, 100)
            toast.show()

            val ary:Array<Any> = arrayOf()
            callback(0, ary) // 回调通知
            return
        }
        if (action == "refresh") {
            this.page = 1
        }
        val url = this.myGetRequestUrl()
        val client = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object: MyOkHttpCallback(context!!) {
            override fun onResponse(call: Call, response: Response) {

                val body = response.body?.string()
                val baseApiResult = GsonBuilder().create().fromJson(body, BaseApiResult::class.java)
                if (baseApiResult.ret > 0) {
                    val apiResult = myConvertJson(body)
                    callback(apiResult.count, apiResult.list!!) // 回调通知
                    page ++
                } else {
                    XPopup.Builder(context).asConfirm("😱 code: ${response.code}", baseApiResult.msg) {
                        val ary:Array<Any> = arrayOf()
                        callback(0, ary) // 回调通知
                    }.hideCancelBtn().show()
                }
            }
        })
    }

    //----------------------------------------------------------------------------------------------------
    // 根据CELL显示回调，判断是否显示返回顶部按钮
    interface OnViewShowListener {
        fun onShow(position: Int)
    }

    interface OnItemClickListener {
        fun onItemClick(holder: RecyclerView.ViewHolder, position: Int)

        fun onItemMenuClick(holder: RecyclerView.ViewHolder, position: Int, cmd: String)
    }

    class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view)

    class CommonApiResult(val ret: Int, val msg: String, val count: Int, val list: Array<Any>? = null)

    open class MyRecyclerViewAdapter(private val ds: List<Any>) : RecyclerView.Adapter<RecyclerView.ViewHolder> () {

        // 自定义点击事件
        var onViewShowListener: OnViewShowListener? = null
        var onItemClickListener: OnItemClickListener? = null

        // 数据源总数
        override fun getItemCount(): Int {
            var count = ds.count()
            if (count == 0) {
                count++ // 没有数据时加上 empty view
            }
            return count
        }

        override fun getItemViewType(position: Int): Int {
            if (ds.count() == 0)
                return CELL_TYPE_EMPTY // empty

            return  CELL_TYPE_CELL
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)

            return when(viewType) {
                CELL_TYPE_EMPTY -> {
                    val cell = inflater.inflate(R.layout.common_cell_empty, parent, false)
                    EmptyViewHolder(cell)
                }
                else -> {
                    this.myCreateViewHolder(parent)
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val viewType = this.getItemViewType(position)
            if (viewType > 0) {
                if (this.onViewShowListener != null) {
                    this.onViewShowListener!!.onShow(position)
                }

                this.myBindViewHolder(holder, position, ds[position])
            }
        }

        open fun myCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
            TODO("not implemented")
        }

        open fun myBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, module: Any) {
            TODO("not implemented")
        }

        companion object {
            private const val CELL_TYPE_EMPTY = -3
            private const val CELL_TYPE_CELL = 1
        }
    }
}