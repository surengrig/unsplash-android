package com.example.unsplash.repository

import com.example.unsplash.model.UnsplashPost

/**
 * Common interface shared by the different repository implementations.
 */
interface UnsplashPostRepository {
    fun getPosts(pageSize: Int): Listing<UnsplashPost>
    fun cancel()
}