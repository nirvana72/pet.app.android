package com.mu78.pethobby.extension

import android.content.Context
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager


class SmoothScrollLayoutManager(context: Context?) : LinearLayoutManager(context) {

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView,
        state: RecyclerView.State?, position: Int
    ) {

        val smoothScroller = object : LinearSmoothScroller(recyclerView.context) {
            // 返回：滑过1px时经历的时间(ms)。
            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return 15f / displayMetrics.densityDpi
            }
        }

        smoothScroller.targetPosition = position
        startSmoothScroll(smoothScroller)
    }
}
