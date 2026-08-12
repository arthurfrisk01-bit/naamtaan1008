package com.naamtaan1008.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 社区用户（对登录者自身返回的完整字段，含 email 等私有字段）。 */
@Serializable
data class CommunityUser(
    val id: String = "",
    val nickname: String = "",
    val bio: String = "",
    val roles: List<String> = emptyList(),
    @SerialName("locationLevel") val locationLevel: String = "L1",
    val city: String = "",
    val district: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val active: Boolean = true,
    @SerialName("createdAt") val createdAt: String = "",
    // 私有字段（仅 /me 返回）
    val email: String = "",
    @SerialName("refreshLocation") val refreshLocation: Boolean = false,
    val following: List<String> = emptyList()
)

/** 登录响应。 */
@Serializable
data class LoginResponse(
    val success: Boolean = false,
    val token: String = "",
    val user: CommunityUser = CommunityUser(),
    val error: String? = null
)

/** 通用社区响应（register/verify/location 等）。 */
@Serializable
data class CommunityApiResponse(
    val success: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

/** /me 响应。 */
@Serializable
data class MeResponse(
    val success: Boolean = false,
    val user: CommunityUser = CommunityUser(),
    val error: String? = null
)

/** 地图聚合点（/api/map/overview 中的场馆点）。 */
@Serializable
data class VenuePoint(
    val type: String = "",
    val id: String = "",
    val name: String = "",
    val city: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val shows: List<Show> = emptyList()
)

/** 地图概览响应。 */
@Serializable
data class MapOverviewResponse(
    val success: Boolean = false,
    val venues: List<VenuePoint> = emptyList(),
    val scene: List<VenuePoint> = emptyList()
)

/** /api/shows/upcoming 响应（未来 N 天窗口演出）。 */
@Serializable
data class UpcomingShowsResponse(
    val success: Boolean = false,
    val total: Int = 0,
    val days: Int = 7,
    val shows: List<Show> = emptyList()
)
