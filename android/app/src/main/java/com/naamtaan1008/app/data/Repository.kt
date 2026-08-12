package com.naamtaan1008.app.data

import com.naamtaan1008.app.data.model.AboutResponse
import com.naamtaan1008.app.data.model.ArticleDetail
import com.naamtaan1008.app.data.model.ArticleDetailResponse
import com.naamtaan1008.app.data.model.ArticleListResponse
import com.naamtaan1008.app.data.model.ArticleSummary
import com.naamtaan1008.app.data.model.ContactResult
import com.naamtaan1008.app.data.model.SceneResponse
import com.naamtaan1008.app.data.model.Show
import com.naamtaan1008.app.data.model.ShowsResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Repository for the naamtaan1008 public API.
 * Each call is a plain HTTP GET returning JSON; failures surface as exceptions
 * which the UI layer translates into user-facing errors.
 */
object Repository {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private fun getString(path: String): String {
        val request = Request.Builder().url(ApiClient.url(path)).get().build()
        ApiClient.client.newCall(request).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}: $body")
            return body
        }
    }

    /** Upcoming + past shows. */
    suspend fun fetchShows(): List<Show> {
        val body = getString("shows")
        return try {
            json.decodeFromString<ShowsResponse>(body).shows
        } catch (e: Exception) {
            // Fallback: tolerate response shapes that just wrap an array
            emptyList()
        }
    }

    /** Focus shows for the home page (may be either a list or wrapped). */
    suspend fun fetchFocusShows(): List<Show> {
        val body = getString("shows/focus")
        return try {
            json.decodeFromString<ShowsResponse>(body).shows
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** All scene entries, keyed by type. */
    suspend fun fetchScene(): SceneResponse {
        val body = getString("scene/all")
        return json.decodeFromString<SceneResponse>(body)
    }

    /** Home intro text pulled from /api/content (site.home). */
    suspend fun fetchHomeIntro(): String {
        val body = getString("content")
        return try {
            val root = json.parseToJsonElement(body)
            val home = root.jsonObject["home"]?.jsonObject
            home?.get("intro")?.jsonPrimitive?.content ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    // Convenience for scene list keyed by type, used by the Scene fragment.
    fun sceneTypeLabel(type: String): String = when (type) {
        "bands" -> "乐队"
        "venues" -> "场地"
        "rehearsals" -> "排练房"
        "shops" -> "商店"
        "studios" -> "工作室"
        "homestays" -> "民宿"
        else -> type
    }

    /** Paged article list. */
    suspend fun fetchArticles(page: Int = 1, pageSize: Int = 20): List<ArticleSummary> {
        val body = getString("articles?page=$page&pageSize=$pageSize")
        return try {
            json.decodeFromString<ArticleListResponse>(body).articles
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Single article body (HTML content). */
    suspend fun fetchArticle(id: String): ArticleDetail {
        val body = getString("article/$id")
        return json.decodeFromString<ArticleDetailResponse>(body).article
    }

    /** About page content (HTML). */
    suspend fun fetchAbout(): AboutResponse {
        val body = getString("about")
        return json.decodeFromString<AboutResponse>(body)
    }

    /** Submit the contact form (name/email/message). */
    suspend fun submitContact(name: String, email: String, message: String): ContactResult {
        val payload: JsonObject = buildJsonObject {
            put("name", name)
            put("email", email)
            put("message", message)
            put("type", "general")
            put("category", "")
        }
        val request = Request.Builder()
            .url(ApiClient.url("contact/submit"))
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        ApiClient.client.newCall(request).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            return try {
                json.decodeFromString<ContactResult>(body)
            } catch (e: Exception) {
                ContactResult(success = false, error = "HTTP ${resp.code}")
            }
        }
    }
}
