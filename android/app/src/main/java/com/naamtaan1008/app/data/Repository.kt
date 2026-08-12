package com.naamtaan1008.app.data

import com.naamtaan1008.app.data.model.AboutResponse
import com.naamtaan1008.app.data.model.ArticleDetail
import com.naamtaan1008.app.data.model.ArticleDetailResponse
import com.naamtaan1008.app.data.model.ArticleListResponse
import com.naamtaan1008.app.data.model.ArticleSummary
import com.naamtaan1008.app.data.model.CommunityApiResponse
import com.naamtaan1008.app.data.model.CommunityUser
import com.naamtaan1008.app.data.model.ContactResult
import com.naamtaan1008.app.data.model.LoginResponse
import com.naamtaan1008.app.data.model.MapOverviewResponse
import com.naamtaan1008.app.data.model.MeResponse
import com.naamtaan1008.app.data.model.SceneResponse
import com.naamtaan1008.app.data.model.Show
import com.naamtaan1008.app.data.model.ShowsResponse
import com.naamtaan1008.app.data.model.UpcomingShowsResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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

    // ==================== 社区 API (v0.3) ====================

    /** 统一的社区请求：带 Bearer token + 指定 HTTP 方法 + JSON body。 */
    private suspend fun communityRequest(
        path: String,
        method: String = "GET",
        token: String? = null,
        jsonBody: String? = null
    ): Pair<Int, String> {
        val builder = Request.Builder().url(ApiClient.url(path))
        if (token != null) builder.header("Authorization", "Bearer $token")
        val body = jsonBody?.toRequestBody("application/json".toMediaType())
        when (method) {
            "POST" -> builder.post(body ?: "".toRequestBody("application/json".toMediaType()))
            "PATCH" -> builder.patch(body ?: "".toRequestBody("application/json".toMediaType()))
            "PUT" -> builder.put(body ?: "".toRequestBody("application/json".toMediaType()))
            else -> builder.get()
        }
        ApiClient.client.newCall(builder.build()).execute().use { resp ->
            return Pair(resp.code, resp.body?.string().orEmpty())
        }
    }

    /** 注册：发验证码。 */
    suspend fun communityRegister(email: String, password: String, nickname: String): CommunityApiResponse {
        val payload = buildJsonObject {
            put("email", email)
            put("password", password)
            put("nickname", nickname)
        }.toString()
        val (code, body) = communityRequest("community/register", "POST", null, payload)
        return try {
            json.decodeFromString<CommunityApiResponse>(body)
        } catch (e: Exception) {
            CommunityApiResponse(success = false, error = "HTTP $code")
        }
    }

    /** 验证邮箱 + 激活账户。 */
    suspend fun communityVerify(email: String, code: String, password: String, nickname: String): CommunityApiResponse {
        val payload = buildJsonObject {
            put("email", email)
            put("code", code)
            put("password", password)
            put("nickname", nickname)
        }.toString()
        val (httpCode, body) = communityRequest("community/verify", "POST", null, payload)
        return try {
            json.decodeFromString<CommunityApiResponse>(body)
        } catch (e: Exception) {
            CommunityApiResponse(success = false, error = "HTTP $httpCode")
        }
    }

    /** 登录，返回 JWT + 用户。 */
    suspend fun communityLogin(email: String, password: String): LoginResponse {
        val payload = buildJsonObject {
            put("email", email)
            put("password", password)
        }.toString()
        val (_, body) = communityRequest("community/login", "POST", null, payload)
        return try {
            json.decodeFromString<LoginResponse>(body)
        } catch (e: Exception) {
            LoginResponse(success = false, error = "登录失败")
        }
    }

    /** 获取当前用户资料。 */
    suspend fun communityMe(token: String): MeResponse {
        val (code, body) = communityRequest("community/me", "GET", token, null)
        return try {
            json.decodeFromString<MeResponse>(body)
        } catch (e: Exception) {
            MeResponse(success = false, error = "HTTP $code")
        }
    }

    /** 更新资料（昵称/bio/城市/区县/角色/位置五档/刷新开关）。 */
    suspend fun communityUpdateProfile(
        token: String,
        nickname: String,
        bio: String,
        city: String,
        district: String,
        roles: List<String>,
        locationLevel: String,
        refreshLocation: Boolean
    ): MeResponse {
        val rolesArr = JsonArray(roles.map { JsonPrimitive(it) })
        val payload = buildJsonObject {
            put("nickname", nickname)
            put("bio", bio)
            put("city", city)
            put("district", district)
            put("locationLevel", locationLevel)
            put("refreshLocation", refreshLocation)
            put("roles", rolesArr)
        }.toString()
        val (code, body) = communityRequest("community/me", "PATCH", token, payload)
        return try {
            json.decodeFromString<MeResponse>(body)
        } catch (e: Exception) {
            MeResponse(success = false, error = "HTTP $code")
        }
    }

    /** 动态位置上报。 */
    suspend fun communityReportLocation(token: String, lat: Double, lng: Double): CommunityApiResponse {
        val payload = buildJsonObject {
            put("lat", lat)
            put("lng", lng)
        }.toString()
        val (code, body) = communityRequest("community/me/location", "PUT", token, payload)
        return try {
            json.decodeFromString<CommunityApiResponse>(body)
        } catch (e: Exception) {
            CommunityApiResponse(success = false, error = "HTTP $code")
        }
    }

    /** 关注用户。 */
    suspend fun communityFollow(token: String, userId: String): CommunityApiResponse {
        val (code, body) = communityRequest("community/follow/$userId", "POST", token, null)
        return try {
            json.decodeFromString<CommunityApiResponse>(body)
        } catch (e: Exception) {
            CommunityApiResponse(success = false, error = "HTTP $code")
        }
    }

    /** 地图概览（演出场馆点 + 场景点）。 */
    suspend fun fetchMapOverview(): MapOverviewResponse {
        val (code, body) = communityRequest("map/overview", "GET", null, null)
        return try {
            json.decodeFromString<MapOverviewResponse>(body)
        } catch (e: Exception) {
            MapOverviewResponse(success = false)
        }
    }

    /** 未来 N 天演出窗口。 */
    suspend fun fetchUpcomingShows(days: Int = 7): List<Show> {
        val (_, body) = communityRequest("shows/upcoming?days=$days", "GET", null, null)
        return try {
            json.decodeFromString<UpcomingShowsResponse>(body).shows
        } catch (e: Exception) {
            emptyList()
        }
    }
}
