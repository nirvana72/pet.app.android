package com.mu78.pethobby.extension

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

// ViewPager 的功能适配器
class MyViewPagerAdapter(manager: FragmentManager) : FragmentPagerAdapter(manager) {

    private val fragmentList : MutableList<Fragment> = ArrayList()

    override fun getItem(p0: Int): Fragment {
        // Log.d("log", "getItem")
        return fragmentList[p0]
    }

    override fun getCount(): Int {
        // Log.d("log", "MyViewPagerAdapter getCount")
        return fragmentList.size
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        // viewpager+fragment来回滑动fragment重新加载的简单解决办法
        // super.destroyItem(container, position, `object`)
    }

    fun addFragment(fragment: Fragment) {
        fragmentList.add(fragment)
    }
}