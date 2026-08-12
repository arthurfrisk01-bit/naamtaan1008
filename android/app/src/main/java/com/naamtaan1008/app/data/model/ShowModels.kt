package com.naamtaan1008.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShowsResponse(
    val lastFetched: String? = null,
    val total: Long? = null,
    val shows: List<Show> = emptyList()
)

@Serializable
data class Show(
    val id: Long = 0,
    val title: String = "",
    val showTime: String = "",
    val venue: String = "",
    val city: String = "",
    val poster: String = "",
    val price: String = "",
    val performers: String = "",
    val soldOut: Boolean = false,
    val url: String = "",
    val status: String = ""
) : java.io.Serializable
