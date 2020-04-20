package com.mu78.pethobby.extension

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

/*
* 禁止左右滑动的 ViewPager
* 原理是重写两个方法
* */
class NoScrollViewPager : ViewPager {

    private var isScroll: Boolean = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    fun setScroll(scroll: Boolean) {
        isScroll = scroll
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return this.isScroll && super.onTouchEvent(event)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return this.isScroll && super.onInterceptTouchEvent(event)
    }
}