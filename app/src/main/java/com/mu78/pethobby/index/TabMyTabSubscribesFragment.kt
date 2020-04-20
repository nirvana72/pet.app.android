package com.mu78.pethobby.index

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import com.lxj.xpopup.XPopup
import com.mu78.pethobby.R
import com.mu78.pethobby.auth.UserInfoActivity
import com.mu78.pethobby.extension.MyOkHttpCallback
import com.mu78.pethobby.extension.TokenInterceptor
import com.mu78.pethobby.utils.MyConfig
import com.mu78.pethobby.utils.MySession
import com.mu78.pethobby.utils.MyTools
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.my_subscribes_cell.view.*
import okhttp3.*

class TabMyTabSubscribesFragment : TabMyTabBaseFragment() {
    init {
        this.tabIndex = 1
    }

    override fun myConvertJson(body: String?): CommonApiResult {
        val apiResult = GsonBuilder().create().fromJson(body, ApiResult::class.java)
        return CommonApiResult(apiResult.ret, apiResult.msg, apiResult.count, apiResult.list as Array<Any>)
    }

    override fun myCreateAdapter(ds: MutableList<Any>): MyRecyclerViewAdapter {
        val adapter = MyRvAdapter(ds)
        adapter.onItemClickListener = object: OnItemClickListener {
            override fun onItemClick(holder: RecyclerView.ViewHolder, position: Int) {
                val subscribe = ds[position] as Subscribe
                val intent = Intent(context, UserInfoActivity::class.java)
                intent.putExtra("uid", subscribe.uid)
                startActivity(intent)
            }

            override fun onItemMenuClick(holder: RecyclerView.ViewHolder, position: Int, cmd: String) {
                if (cmd == "delete") {
                    val subscribe = dataSource[position] as Subscribe
                    this@TabMyTabSubscribesFragment.deleteSubmit(subscribe.uid) {
                        val ani = AlphaAnimation(1f, 0f)
                        ani.duration = 500
                        ani.setAnimationListener(object: Animation.AnimationListener {
                            override fun onAnimationRepeat(animation: Animation?) {}
                            override fun onAnimationStart(animation: Animation?) {}
                            override fun onAnimationEnd(animation: Animation?) {
                                dataSource.removeAt(position)
                                mAdapter.notifyDataSetChanged()
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

    private fun deleteSubmit(sub_uid: Int, callback: () -> Unit) {
        val formBody = FormBody.Builder().build()
        val url = "${MyConfig.APP_API_HOST}/subscribes/$sub_uid"
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
        return "${MyConfig.APP_API_HOST}/subscribes/${MySession.getInstance().uid}?page=${this.page}&limit=${this.pageLimit}"
    }

    //----------------------------------------------------------------------------------------------------
    class MyRvAdapter(ds: List<Any>): TabMyTabBaseFragment.MyRecyclerViewAdapter(ds) {
        override fun myCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val cell = inflater.inflate(R.layout.my_subscribes_cell, parent, false)
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
            val m = module as Subscribe
            cellViewHolder.createView(m)
        }
    }

    class CellViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun createView(module: Subscribe) {
            val avatarUrl = MyTools.avatarPath(module.avatar,false)
            Picasso.get().load(avatarUrl).into(view.ivPicture)

            view.tvTitle.text = module.nickname
            view.tvSubTitle.text = "粉丝 ${module.fans} ● 发布 ${module.articles}"
            view.tvProfile.text = module.profile
        }
    }
    //----------------------------------------------------------------------------------------------------
    // 对应HTTP接口返回结构的类
    class ApiResult(val ret: Int, val msg: String, val count: Int, val list: Array<Subscribe>)

    class Subscribe(val uid: Int, val nickname: String, val avatar: Int, val profile: String, val fans: Int, val articles: Int)
}