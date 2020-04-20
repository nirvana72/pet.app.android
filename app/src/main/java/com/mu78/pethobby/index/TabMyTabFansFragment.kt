package com.mu78.pethobby.index

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import com.mu78.pethobby.R
import com.mu78.pethobby.auth.UserInfoActivity
import com.mu78.pethobby.utils.MyConfig
import com.mu78.pethobby.utils.MySession
import com.mu78.pethobby.utils.MyTools
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.my_fans_cell.view.*

class TabMyTabFansFragment : TabMyTabBaseFragment() {
    init {
        this.tabIndex = 3
    }

    override fun myConvertJson(body: String?): CommonApiResult {
        val apiResult = GsonBuilder().create().fromJson(body, ApiResult::class.java)
        return CommonApiResult(apiResult.ret, apiResult.msg, apiResult.count, apiResult.list as Array<Any>)
    }

    override fun myGetRequestUrl(): String {
        return "${MyConfig.APP_API_HOST}/subscribes/${MySession.getInstance().uid}/fans?page=${this.page}&limit=${this.pageLimit}"
    }

    override fun myCreateAdapter(ds: MutableList<Any>): MyRecyclerViewAdapter {
        val adapter = MyRvAdapter(ds)
        adapter.onItemClickListener = object: OnItemClickListener {
            override fun onItemClick(holder: RecyclerView.ViewHolder, position: Int) {
                val subscribe = ds[position] as Fans
                val intent = Intent(context, UserInfoActivity::class.java)
                intent.putExtra("uid", subscribe.uid)
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
            val cell = inflater.inflate(R.layout.my_fans_cell, parent, false)
            return CellViewHolder(cell)
        }

        override fun myBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, module: Any) {
            val cellViewHolder = holder as CellViewHolder
            val item = cellViewHolder.view.findViewById<ConstraintLayout>(R.id.layoutItem)
            item.setOnClickListener {
                this.onItemClickListener?.onItemClick(holder, position)
            }
            val m = module as Fans
            cellViewHolder.createView(m)
        }
    }

    class CellViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun createView(module: Fans) {
            val avatarUrl = MyTools.avatarPath(module.avatar,false)
            Picasso.get().load(avatarUrl).into(view.ivPicture)

            view.tvTitle.text = module.nickname
            view.tvSubTitle.text = "粉丝 ${module.fans} ● 发布 ${module.articles}"
            view.tvProfile.text = module.profile
        }
    }

    //----------------------------------------------------------------------------------------------------
    // 对应HTTP接口返回结构的类
    class ApiResult(val ret: Int, val msg: String, val count: Int, val list: Array<Fans>)

    class Fans(val uid: Int, val nickname: String, val avatar: Int, val profile: String, val fans: Int, val articles: Int)

}