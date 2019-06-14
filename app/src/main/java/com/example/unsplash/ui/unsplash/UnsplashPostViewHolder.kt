package com.example.unsplash.ui.unsplash

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.unsplash.R
import com.example.unsplash.model.UnsplashPost
import com.example.unsplash.libs.ImageLoader


class UnsplashPostViewHolder(
    view: View,
    private val imageLoader: ImageLoader
) : RecyclerView.ViewHolder(view) {
    private val thumbnail: ImageView = view.findViewById(R.id.thumbnail)
    private var post: UnsplashPost? = null


    fun bind(post: UnsplashPost?) {
        this.post = post
        post?.urls?.regular?.let { imageLoader.load(it, thumbnail) }
    }

    companion object {
        fun create(
            parent: ViewGroup,
            imageLoader: ImageLoader
        ): UnsplashPostViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.post_item, parent, false)
            return UnsplashPostViewHolder(
                view, imageLoader
            )
        }
    }
}