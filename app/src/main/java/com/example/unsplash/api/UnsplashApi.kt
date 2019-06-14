package com.example.unsplash.api

import com.example.unsplash.BuildConfig
import com.example.unsplash.libs.HttpClient
import com.example.unsplash.libs.httpClient


class UnsplashApiService private constructor(private val client: HttpClient) {
    /**
     * Endpoint for photo posts
     * GET /photos, params: page (Page number to retrieve. (Optional; default: 1))
     *
     * @param pageIndex page number to retrieve
     */
    suspend fun getPage(
        pageIndex: Int
    ) = client.get(
        path = "photos",
        params = listOf("page" to pageIndex.toString())
    )

    companion object {
        private const val BASE_URL = "https://api.unsplash.com/"
        private const val CLIENT_ID = BuildConfig.client_id
        fun create() = create(BASE_URL, CLIENT_ID)

        fun create(url: String, client_id: String): UnsplashApiService {
            val client = httpClient {
                baseUrl(url)
                baseParams(listOf("client_id" to client_id))
            }
            return UnsplashApiService(client)
        }
    }
}