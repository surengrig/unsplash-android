package com.example.unsplash.ui.unsplash

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import androidx.paging.PagedList
import androidx.recyclerview.widget.GridLayoutManager
import com.example.unsplash.R
import com.example.unsplash.ServiceLocator
import com.example.unsplash.model.NetworkState
import com.example.unsplash.model.UnsplashPost
import com.example.unsplash.libs.ImageLoader
import kotlinx.android.synthetic.main.activity_unsplash.*

/**
 * A list activity that shows reddit posts in the given sub-reddit.
 * <p>
 * The intent arguments can be modified to make it use a different repository (see MainActivity).
 */
class UnsplashActivity : AppCompatActivity() {
    private lateinit var model: UnsplashViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unsplash)

        val layoutManager = GridLayoutManager(this, 2)

        list.layoutManager = layoutManager

        model = getViewModel()
        initAdapter()
        initSwipeToRefresh()
        model.showPosts()
    }

    private fun getViewModel(): UnsplashViewModel {
        return ViewModelProviders.of(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repo = ServiceLocator.instance().repository
                @Suppress("UNCHECKED_CAST")
                return UnsplashViewModel(repo) as T
            }
        }).get()
    }

    private fun initAdapter() {
        val imageLoader = ImageLoader()
            .placeholder(R.drawable.ic_placeholder)
        val adapter = PostsAdapter(imageLoader) {
            model.retry()
        }
        list.adapter = adapter
        model.posts.observe(this, Observer<PagedList<UnsplashPost>> {
            adapter.submitList(it)
        })
        model.networkState.observe(this, Observer {
            adapter.setNetworkState(it)
        })
    }

    private fun initSwipeToRefresh() {
        model.refreshState.observe(this) {
            swipe_refresh.isRefreshing = it == NetworkState.LOADING
        }
        swipe_refresh.setOnRefreshListener {
            model.refresh()
        }
    }
}
