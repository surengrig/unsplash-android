/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.unsplash.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Data class representation for /photos json response
 */
data class UnsplashPost(
    val id: String,
    val width: Int,
    val height: Int,
    val color: String,
    val description: String?,
    val urls: Urls
) {

    data class Urls(
        val raw: String,
        val full: String,
        val regular: String,
        val small: String,
        val thumb: String
    ) {
        companion object {
            fun fromJson(jsonObject: JSONObject): Urls {
                val raw = jsonObject.getString("raw")
                val full = jsonObject.getString("full")
                val regular = jsonObject.getString("regular")
                val small = jsonObject.getString("small")
                val thumb = jsonObject.getString("thumb")
                return Urls(
                    raw = raw,
                    full = full,
                    regular = regular,
                    small = small,
                    thumb = thumb
                )
            }
        }
    }

    companion object {
        fun fromJson(jsonObject: JSONObject): UnsplashPost {
            val id = jsonObject.get("id") as String
            val width = jsonObject.get("width") as Int
            val height = jsonObject.get("height") as Int
            val color = jsonObject.get("color") as String
            val description = jsonObject.get("description") as? String?
            val urls = Urls.fromJson(jsonObject.getJSONObject("urls"))

            return UnsplashPost(
                id = id,
                width = width,
                height = height,
                color = color,
                description = description,
                urls = urls
            )
        }

        fun listFromJson(jsonArray: JSONArray): List<UnsplashPost> {
            return (0 until jsonArray.length()).map {
                fromJson(jsonArray.getJSONObject(it))
            }
        }
    }
}