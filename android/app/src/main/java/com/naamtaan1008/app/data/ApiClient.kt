package com.naamtaan1008.app.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Thin HTTP layer over the public naamtaan1008.com API.
 * Base URL must end with a slash; paths are joined relative to it.
 */
object ApiClient {
    private const val BASE_URL = "https://naamtaan1008.com/api/"

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun url(path: String): String = BASE_URL + path.trimStart('/')
}
