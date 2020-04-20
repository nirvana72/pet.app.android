package com.mu78.pethobby.utils

import android.content.Context
import android.widget.ImageView
import com.lxj.xpopup.interfaces.XPopupImageLoader
import com.squareup.picasso.Picasso
import java.io.File

class MyXPopupImageLoader(private val isLocal:Boolean = false) : XPopupImageLoader {
    override fun loadImage(position: Int, uri: Any, imageView: ImageView) {
        if (this.isLocal) {
            Picasso.get().load("file://$uri").into(imageView)
        }
        else {
            Picasso.get().load("$uri").into(imageView)
        }
    }

    override fun getImageFile(context: Context, uri: Any): File? {
        return null
    }
}