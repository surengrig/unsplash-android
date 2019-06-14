package com.example.unsplash.repository.byPage

import androidx.annotation.MainThread
import androidx.lifecycle.Transformations
import androidx.paging.toLiveData
import com.example.unsplash.api.UnsplashApiService
import com.example.unsplash.repository.Listing
import com.example.unsplash.model.UnsplashPost
import com.example.unsplash.repository.UnsplashPostRepository

/**
 * Repository implementation that returns a Listing that loads data directly from network.
 */
class InMemoryByPageKeyRepository(
    private val unsplashApi: UnsplashApiService
) : UnsplashPostRepository {

    @MainThread
    override fun getPosts(page: String, pageSize: Int): Listing<UnsplashPost> {
        val sourceFactory = PostsDataSourceFactory(unsplashApi)

        val livePagedList = sourceFactory.toLiveData(
            pageSize = pageSize
        )

        val refreshState = Transformations.switchMap(sourceFactory.sourceLiveData) {
            it.initialLoad
        }
        return Listing(
            pagedList = livePagedList,
            networkState = Transformations.switchMap(sourceFactory.sourceLiveData) {
                it.networkState
            },
            retry = {
                sourceFactory.sourceLiveData.value?.retryAllFailed()
            },
            refresh = {
                sourceFactory.sourceLiveData.value?.invalidate()
            },
            refreshState = refreshState
        )
    }
}

