package com.example.unsplash.ui.unsplash

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.example.unsplash.model.UnsplashPost
import com.example.unsplash.repository.Listing
import com.example.unsplash.repository.UnsplashPostRepository

class UnsplashViewModel(val repository: UnsplashPostRepository) : ViewModel() {
    private val repoResult = MutableLiveData<Listing<UnsplashPost>>()
    val posts = Transformations.switchMap(repoResult) { it.pagedList }
    val networkState = Transformations.switchMap(repoResult) { it.networkState }
    val refreshState = Transformations.switchMap(repoResult) { it.refreshState }

    fun refresh() {
        repoResult.value?.refresh?.invoke()
    }

    fun showPosts() {
        repoResult.value = repository.getPosts(30)
    }

    fun retry() {
        val listing = repoResult.value
        listing?.retry?.invoke()
    }

    override fun onCleared() {
        repository.cancel()
        super.onCleared()
    }
}