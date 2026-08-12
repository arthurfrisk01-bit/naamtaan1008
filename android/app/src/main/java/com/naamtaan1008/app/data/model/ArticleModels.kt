package com.naamtaan1008.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Summary item returned by GET /api/articles (list). */
@Serializable
data class ArticleSummary(
    val id: String = "",
    val title: String = "",
    val date: String = "",
    val category: String = "",
    val column: String = "",
    val summary: String = ""
)

/** Wrapper for the paged article list endpoint. */
@Serializable
data class ArticleListResponse(
    val success: Boolean = false,
    val total: Int = 0,
    val page: Int = 1,
    @SerialName("pageSize") val pageSize: Int = 10,
    val articles: List<ArticleSummary> = emptyList()
)

/** Full article body returned by GET /api/article/:id. */
@Serializable
data class ArticleDetail(
    val id: String = "",
    val title: String = "",
    val date: String = "",
    val category: String = "",
    val column: String = "",
    val summary: String = "",
    val content: String = ""
)

/** Wrapper for the single-article endpoint (site metadata is ignored). */
@Serializable
data class ArticleDetailResponse(
    val article: ArticleDetail = ArticleDetail()
)

/** About page content returned by GET /api/about. */
@Serializable
data class AboutResponse(
    val title: String = "",
    val content: String = ""
)

/** Result of POST /api/contact/submit. */
@Serializable
data class ContactResult(
    val success: Boolean = false,
    val error: String? = null
)
