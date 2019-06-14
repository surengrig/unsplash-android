package com.example.unsplash.repository

import com.example.unsplash.model.UnsplashPost

/**
 * Common interface shared by the different repository implementations.
 */
interface UnsplashPostRepository {
    fun getPosts(page: String, pageSize: Int): Listing<UnsplashPost>
}