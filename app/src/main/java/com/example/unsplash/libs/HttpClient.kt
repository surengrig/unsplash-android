package com.example.unsplash.libs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


data class Response(val rawData: String, val status: Int, val headers: Map<String, List<String>>)

/**
 * DSL for building @HttpClient
 */
inline fun httpClient(buildHttpClient: HttpClient.Builder.() -> Unit): HttpClient {
    val builder = HttpClient.Builder()
    builder.buildHttpClient()
    return builder.build()
}

/**
 * Class for making requests
 */
class HttpClient private constructor(
    private val baseUrl: String,
    private val baseParams: List<Pair<String, String>>
) {

    /**
     * Enum for request method
     */
    private enum class RequestType { GET, POST, DELETE }

    data class Builder(var baseUrl: String = "", var baseParams: List<Pair<String, String>> = listOf()) {
        fun baseUrl(baseUrl: String) = apply { this.baseUrl = baseUrl }
        fun baseParams(params: List<Pair<String, String>>) = apply { this.baseParams = params }
        fun build() = HttpClient(baseUrl, baseParams)
    }

    /**
     * Implements basic GET request
     *
     * @param path relative path
     * @param params list of key-value pair for request queries, combines with baseParams
     */
    suspend fun get(path: String = "", params: List<Pair<String, String>> = listOf()): Response? {
        return coroutineScope {
            withContext(Dispatchers.IO) {
                makeRequest(RequestType.GET, buildUrl(baseUrl, baseParams, params, path))
            }
        }
    }

    /**
     * Builds url from base path, relative path and query params
     */
    private fun buildUrl(
        baseUrl: String,
        baseParams: List<Pair<String, String>>,
        params: List<Pair<String, String>>,
        path: String
    ): String {
        // TODO url validation, leading space trunction
        val allParams = baseParams.union(params)
        val url = baseUrl + path +
                allParams.joinToString(prefix = "?", separator = "&") {
                    "${it.first}=${it.second}"
                }
        return url
    }


    /**
     * Does the request with the specified method type.
     */
//    TODO better error handling, implement POST and DELETE request methods
    private fun makeRequest(type: RequestType, url: String): Response? {
        try {

            val httpURLConnection = URL(url)
                .openConnection() as HttpURLConnection
            httpURLConnection.apply {
                requestMethod = when (type) {
                    RequestType.GET -> "GET"
                    RequestType.POST -> TODO()
                    RequestType.DELETE -> TODO()
                }
                setRequestProperty("Content-Type", "application/json")
            }
            httpURLConnection.connect()


            val statusCode = httpURLConnection.responseCode
            val headers = httpURLConnection.headerFields

            if (statusCode != 200) return Response(status = statusCode, rawData = "", headers = headers)

            val inputStream = httpURLConnection.inputStream
            if (inputStream == null) return null

            val buffer = StringBuffer()
            BufferedReader(InputStreamReader(inputStream)).useLines {
                it.forEach { line ->
                    buffer.append(line + "\n")
                }
            }

            if (buffer.isEmpty()) return null

            val response = buffer.toString()

            return Response(rawData = response, status = statusCode, headers = headers)

        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }
}