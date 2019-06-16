package com.example.unsplash

import com.example.unsplash.api.UnsplashApiService
import com.example.unsplash.repository.UnsplashPostRepository
import com.example.unsplash.repository.byPage.InMemoryByPageKeyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

interface ServiceLocator {
    companion object {
        private val LOCK = Any()
        private var instance: ServiceLocator? = null
        fun instance(): ServiceLocator {
            synchronized(LOCK) {
                instance?.let { return@instance it }
                val newInstance = AppServiceLocator()
                instance = newInstance
                return newInstance
            }
        }
    }

    val unsplashApi: UnsplashApiService
    val repository: UnsplashPostRepository
    val repoScope: CoroutineScope
}

/**
 * default implementation of ServiceLocator that uses production endpoints.
 */
class AppServiceLocator : ServiceLocator {
    override val repoScope by lazy {
        CoroutineScope(Dispatchers.IO)
    }
    override val unsplashApi by lazy {
        UnsplashApiService.create()
    }
    override val repository by lazy {
        InMemoryByPageKeyRepository(unsplashApi = unsplashApi, coroutineScope = repoScope)
    }
}