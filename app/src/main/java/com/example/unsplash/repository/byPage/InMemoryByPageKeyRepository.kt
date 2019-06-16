package com.example.unsplash.repository.byPage

import androidx.annotation.MainThread
import androidx.lifecycle.Transformations
import androidx.paging.toLiveData
import com.example.unsplash.api.UnsplashApiService
import com.example.unsplash.model.UnsplashPost
import com.example.unsplash.repository.Listing
import com.example.unsplash.repository.UnsplashPostRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Repository implementation that returns a Listing that loads data directly from network.
 */
class InMemoryByPageKeyRepository(
    private val unsplashApi: UnsplashApiService,
    private val coroutineScope: CoroutineScope
) : UnsplashPostRepository {

    @ExperimentalCoroutinesApi
    override fun cancel() {
        coroutineScope.cancel()
    }

    @MainThread
    override fun getPosts(pageSize: Int): Listing<UnsplashPost> {
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
                coroutineScope.launch {
                    sourceFactory.sourceLiveData.value?.retryAllFailed()
                }
            },
            refresh = {
                sourceFactory.sourceLiveData.value?.invalidate()
            },
            refreshState = refreshState
        )
    }
}

