package com.example.unsplash.repository.byPage

import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import com.example.unsplash.api.UnsplashApiService
import com.example.unsplash.model.NetworkState
import com.example.unsplash.model.UnsplashPost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray

/**
 * A data source that uses the page index returned for page requests.
 */
class PageKeyedPostsDataSource(
    private val unsplashApi: UnsplashApiService
) : PageKeyedDataSource<Int, UnsplashPost>() {

    val networkState = MutableLiveData<NetworkState>()
    val initialLoad = MutableLiveData<NetworkState>()

    private var nextPage = 1

    // keep a function reference for the retry event
    private var retry: (() -> Any)? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    fun retryAllFailed() {
        val prevRetry = retry
        retry = null
        prevRetry?.let {
            it.invoke()
        }
    }

    override fun loadBefore(
        params: LoadParams<Int>,
        callback: LoadCallback<Int, UnsplashPost>
    ) {
    }


    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, UnsplashPost>) {
        networkState.postValue(NetworkState.LOADING)

        scope.launch {
            val response = unsplashApi.getPage(
                pageIndex = params.key
            )

            if (response == null || response.status != 200) {
                networkState.postValue(NetworkState.error("network error"))
                initialLoad.postValue(NetworkState.error("network error"))
                retry = {
                    loadAfter(params, callback)
                }
                return@launch
            }

            retry = null
            networkState.postValue(NetworkState.LOADED)
            initialLoad.postValue(NetworkState.LOADED)

            val items = UnsplashPost.listFromJson(JSONArray(response.rawData))
            val totalPages = response.headers["X-Total"]?.get(0)?.toInt()
            if (totalPages != null && nextPage < totalPages) nextPage++
            callback.onResult(items, nextPage)
        }
    }

    override fun loadInitial(
        params: LoadInitialParams<Int>,
        callback: LoadInitialCallback<Int, UnsplashPost>
    ) {

        networkState.postValue(NetworkState.LOADING)
        initialLoad.postValue(NetworkState.LOADING)

        scope.launch {
            val response = unsplashApi.getPage(pageIndex = 0)
            retry = {
                loadInitial(params, callback)
            }

            if (response == null || response.status != 200) {
                networkState.postValue(NetworkState.error("network error"))
                initialLoad.postValue(NetworkState.error("network error"))
                return@launch
            }

            retry = null
            networkState.postValue(NetworkState.LOADED)
            initialLoad.postValue(NetworkState.LOADED)

            val items = UnsplashPost.listFromJson(JSONArray(response.rawData))
            val totalPages = response.headers["X-Total"]?.get(0)?.toInt()
            if (totalPages != null && nextPage < totalPages) nextPage++
            callback.onResult(items, null, nextPage)
        }
    }
}