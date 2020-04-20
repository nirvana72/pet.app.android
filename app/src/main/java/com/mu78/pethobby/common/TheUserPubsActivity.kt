package com.mu78.pethobby.common

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import com.mu78.pethobby.BaseActivity
import com.mu78.pethobby.R
import com.mu78.pethobby.article.ArticleViewActivity
import com.mu78.pethobby.extension.MyViewPagerAdapter
import com.mu78.pethobby.index.TabMyTabBaseFragment
import com.mu78.pethobby.index.TabMyTabMyFragment
import com.mu78.pethobby.modules.Article
import com.mu78.pethobby.utils.MyConfig
import kotlinx.android.synthetic.main.common_theuserpubs.*

// 本界面与 TabMyTabMyFragment 极度相似，所以模仿了结构

class TheUserPubsActivity :BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.title = "发布动态"
        this.setContentView(R.layout.common_theuserpubs)

        val uid = this.intent.getIntExtra("uid", -1)

        val mViewPagerAdapter = MyViewPagerAdapter(supportFragmentManager)
        val page1 = TheUserPubsFragment(uid)
        mViewPagerAdapter.addFragment(page1)

        mViewPager.adapter = mViewPagerAdapter
        mViewPager.currentItem = 0
    }
}


class TheUserPubsFragment(private val uid: Int) : TabMyTabBaseFragment() {

    override fun myGetRequestUrl(): String {
        return "${MyConfig.APP_API_HOST}/articles/${this.uid}/users?page=${this.page}&limit=${this.pageLimit}"
    }

    override fun myCreateAdapter(ds: MutableList<Any>): MyRecyclerViewAdapter {
        val adapter = TabMyTabMyFragment.MyRvAdapter(ds)
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

    override fun myConvertJson(body: String?): CommonApiResult {
        val apiResult = GsonBuilder().create().fromJson(body, TabMyTabMyFragment.ApiResult::class.java)
        return CommonApiResult(apiResult.ret, apiResult.msg, apiResult.count, apiResult.list as Array<Any>)
    }
}