package com.mu78.pethobby.index

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.mu78.pethobby.R
import com.mu78.pethobby.extension.MyViewPagerAdapter
import kotlinx.android.synthetic.main.index_my.*

// tab 加载后，更新主窗口数据
interface MyTabEventListener {
    fun updateNum(position: Int, num: Int)
}

class TabMyFragment : Fragment(), MyTabEventListener {

    private lateinit var viewPagerAdapter: MyViewPagerAdapter // ViewPager 的功能适配器

    // 三个懒加载判断参数
    private var mLazyLoadHasCreateView: Boolean = false
    private var mLazyLoadHasSetupView: Boolean = false
    private var mLazyLoadIsVisibleToUser: Boolean = false

    private var tabMenuCount = intArrayOf(0,0,0,0)  // tab计数

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Log.d("log","TabMyFragment.onCreateView")

        this.mLazyLoadHasCreateView = true
        return inflater.inflate(R.layout.index_my, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // og.d("log","TabMyFragment.onViewCreated")

        super.onViewCreated(view, savedInstanceState)

        if (this.mLazyLoadIsVisibleToUser && !this.mLazyLoadHasSetupView) {
            this.setupView()
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        //Log.d("log","TabMyFragment.setUserVisibleHint isVisibleToUser=$isVisibleToUser")

        this.mLazyLoadIsVisibleToUser = isVisibleToUser

        if (isVisibleToUser && this.mLazyLoadHasCreateView && !this.mLazyLoadHasSetupView) {
            this.setupView()
        }
    }

    private fun setupView() {
        this.mLazyLoadHasSetupView = true

        // Log.d("log","TabMyFragment.initView")

        // 生成菜单
        for(index in 1..4) {
            val tab = this.mTabLayout.newTab()
            tab.setCustomView(R.layout.index_my_tabitem)
            val ivIcon = tab.customView?.findViewById<ImageView>(R.id.ivIcon)
            val tvTitle = tab.customView?.findViewById<TextView>(R.id.tvTitle)
            when(index) {
                1 -> {
                    ivIcon?.setImageResource(R.drawable.icon_favorite)
                    tvTitle?.text = "收藏"
                }
                2 -> {
                    ivIcon?.setImageResource(R.drawable.icon_star)
                    tvTitle?.text = "关注"
                }
                3 -> {
                    ivIcon?.setImageResource(R.drawable.icon_account_circle)
                    tvTitle?.text = "我的"
                }
                4 -> {
                    ivIcon?.setImageResource(R.drawable.icon_add)
                    tvTitle?.text = "粉丝"
                }
            }
            mTabLayout.addTab(tab)
        }

        this.viewPagerAdapter = MyViewPagerAdapter(childFragmentManager)
        val page1 = TabMyTabLikesFragment()
        page1.eventListener = this
        this.viewPagerAdapter.addFragment(page1)

        val page2 = TabMyTabSubscribesFragment()
        page2.eventListener = this
        this.viewPagerAdapter.addFragment(page2)

        val page3 = TabMyTabMyFragment()
        page3.eventListener = this
        this.viewPagerAdapter.addFragment(page3)

        val page4 = TabMyTabFansFragment()
        page4.eventListener = this
        this.viewPagerAdapter.addFragment(page4)

        mViewPager.adapter = this.viewPagerAdapter
        mViewPager.setScroll(false)

        // 自定义TAB样式时不要使用下面的方法绑定viewPager，否则会默认使用系统样式
        // tabs.setupWithViewPager(viewPager)

        this.setTabStatus(mTabLayout.getTabAt(0)!!, true)

        mTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(p0: TabLayout.Tab?) {
            }

            override fun onTabUnselected(p0: TabLayout.Tab?) {
                setTabStatus(p0!!, false)
            }

            override fun onTabSelected(p0: TabLayout.Tab?) {
                setTabStatus(p0!!, true)
                mViewPager.currentItem = p0.position
            }
        })
    }

    // MyTabEventListener
    @SuppressLint("SetTextI18n")
    override fun updateNum(position: Int, num: Int) {
        activity?.runOnUiThread{
            val tab = mTabLayout.getTabAt(position)
            val tvNum = tab?.customView?.findViewById<TextView>(R.id.tvNum)
            if (num < 0) {
                tabMenuCount[position] -= 1
            } else {
                tabMenuCount[position] = num
            }
            tvNum?.text = "·${tabMenuCount[position]}"
        }
    }

    // 动态设置TAB ITEM的样式
    private fun setTabStatus(tab: TabLayout.Tab, isActive : Boolean) {
        val ivIcon = tab.customView?.findViewById<ImageView>(R.id.ivIcon)
        val tvTitle = tab.customView?.findViewById<TextView>(R.id.tvTitle)
        val tvNum = tab.customView?.findViewById<TextView>(R.id.tvNum)
        if (isActive) {
            tvTitle?.setTextColor(Color.parseColor("#ff0000"))
            tvNum?.visibility = View.VISIBLE
            ivIcon?.visibility = View.VISIBLE
        }
        else {
            tvTitle?.setTextColor(Color.parseColor("#757575"))
            tvNum?.visibility = View.GONE
            ivIcon?.visibility = View.GONE
        }
    }
}