package com.naamtaan1008.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SceneResponse(
    val success: Boolean = true,
    val data: SceneData = SceneData()
)

@Serializable
data class SceneData(
    val bands: List<SceneItem> = emptyList(),
    val venues: List<SceneItem> = emptyList(),
    val rehearsals: List<SceneItem> = emptyList(),
    val shops: List<SceneItem> = emptyList(),
    val studios: List<SceneItem> = emptyList(),
    val homestays: List<SceneItem> = emptyList()
) {
    fun listFor(type: String): List<SceneItem> = when (type) {
        "bands" -> bands
        "venues" -> venues
        "rehearsals" -> rehearsals
        "shops" -> shops
        "studios" -> studios
        "homestays" -> homestays
        else -> emptyList()
    }
}

@Serializable
data class SceneItem(
    val id: String = "",
    val name: String = "",
    val city: String = "",
    val intro: String = "",
    val styles: List<String> = emptyList(),
    val members: List<String> = emptyList(),
    val links: Map<String, String> = emptyMap(),
    val contact: String = "",
    val address: String = "",
    val price: String = "",
    val capacity: String = "",
    val equipment: String = "",
    val hours: String = "",
    val booking: String = "",
    val images: List<String> = emptyList(),
    val image: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
) : java.io.Serializable
