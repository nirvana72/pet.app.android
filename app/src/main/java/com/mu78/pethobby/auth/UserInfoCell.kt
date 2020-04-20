package com.mu78.pethobby.auth

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.mu78.pethobby.R
import kotlinx.android.synthetic.main.auth_userinfo_cell.view.*

class UserInfoCell(ctx: Context, attrs: AttributeSet) : ConstraintLayout(ctx, attrs) {

    private var mTit = ""
    private var mVal = ""
    private var mClickable = false

    init {
        LayoutInflater.from(ctx).inflate(R.layout.auth_userinfo_cell, this, true)
        val ary = context.obtainStyledAttributes(attrs, R.styleable.UserInfoCell)
        this.mTit = ary.getString(R.styleable.UserInfoCell_my_tit)!!
        this.mVal = ary.getString(R.styleable.UserInfoCell_my_val)!!
        this.mClickable = ary.getBoolean(R.styleable.UserInfoCell_my_clickable, false)
        this.tvTit.text = mTit
        this.tvVal.text = mVal
        this.ivClickable.visibility = if (mClickable) { View.VISIBLE } else { View.INVISIBLE }
    }

    fun setVal(v: String) {
        this.tvVal.text = v
    }

    fun setClickAble(v: Boolean) {
        this.ivClickable.visibility = if (v) { View.VISIBLE } else { View.INVISIBLE }
    }

}