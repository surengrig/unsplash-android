package com.example.unsplash.libs

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.collection.LruCache
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class ImageLoader {

    private val imageViews = Collections.synchronizedMap(WeakHashMap<ImageView, String>())
    private var executorService: ExecutorService = Executors.newFixedThreadPool(5)

    private var placeHolderRes: Int? = null

    private val memoryCache: LruCache<String, Bitmap>

    init {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()

        // Use 1/8th of the available memory for this memory cache.
        val cacheSize = maxMemory / 8
        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.byteCount / 1024
            }
        }
    }

    fun load(url: String, imageView: ImageView) {
        imageViews[imageView] = url
        val bitmap = memoryCache.get(url)
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap)
        } else {
            loadImage(url, imageView)
            placeHolderRes?.let {
                imageView.setImageResource(it)
            }
        }
    }

    fun placeholder(@DrawableRes drawable: Int): ImageLoader {
        placeHolderRes = drawable
        return this
    }

    private fun loadImage(url: String, imageView: ImageView) {
        executorService.submit {
            if (imageViews[imageView] != url) return@submit
            val bitmap = getBitmap(url)

            if (bitmap != null) {
                memoryCache.put(url, bitmap)
            }
            val activity = imageView.context as Activity
            activity.runOnUiThread {
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                } else {
                    placeHolderRes?.let { imageView.setImageResource(it) }
                }
            }
        }
    }

    private fun getBitmap(url: String): Bitmap? {
        try {
            val bitmap: Bitmap?

            val imageUrl = URL(url)
            val urlConnection = (imageUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = 30000
                readTimeout = 30000
                instanceFollowRedirects = true
            }

            val inputStream = urlConnection.inputStream

            bitmap = BitmapFactory.decodeStream(inputStream)
            return bitmap
        } catch (ex: Throwable) {
            ex.printStackTrace()
            return null
        }
    }

}