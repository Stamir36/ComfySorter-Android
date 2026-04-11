package com.unesell.comfysorter.network

import androidx.compose.runtime.mutableStateMapOf
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import okio.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.IOException

// --- ГЛОБАЛЬНЫЙ ТРЕКЕР ПРОГРЕССА ---
object ProgressTracker {
    // Храним прогресс в формате "URL -> 0.5f"
    val progressMap = mutableStateMapOf<String, Float>()

    fun update(url: String, progress: Float) {
        progressMap[url] = progress
    }
}

// --- МОДЕЛИ ДАННЫХ ---
data class GalleryResponse(
    val cwd: String? = null,
    val folders: List<FolderItem>? = null,
    val images: List<ImageItem>? = null
)

data class FolderItem(val name: String?, val relPath: String?, val count: String?)

data class ImageItem(
    val name: String?,
    val relPath: String?,
    val url: String?,
    val thumb: String?,
    val type: String?,
    val mtime: Long?,
    val isFavorite: Boolean = false,
    val size: Long? = null
)

data class ImageDetails(
    val name: String? = null,
    val relPath: String? = null,
    val url: String? = null,
    val type: String? = null,
    val size: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Float? = null,
    val positive: String? = null,
    val negative: String? = null,
    val parameters: Map<String, Any>? = null,
    val isFavorite: Boolean = false
)

// --- ИНТЕРФЕЙС API ---
interface ApiService {
    @GET("api/list")
    suspend fun getList(
        @Query("subpath") subpath: String = "",
        @Query("q") query: String = ""
    ): GalleryResponse

    @GET("api/image")
    suspend fun getImageDetails(
        @Query("relpath") relPath: String
    ): ImageDetails

    @POST("api/favorites")
    suspend fun toggleFavorite(
        @Body request: FavoriteRequest
    ): FavoriteResponse
}

data class FavoriteRequest(val relpath: String)
data class FavoriteResponse(val isFavorite: Boolean)

// --- КЛИЕНТ С ИНТЕРЦЕПТОРОМ ---
object RetrofitClient {
    @Volatile
    private var client: OkHttpClient? = null

    fun getOkHttpClient(): OkHttpClient {
        return client ?: synchronized(this) {
            client ?: OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request()
                    val response = chain.proceed(request)
                    val url = request.url.toString()
                    response.newBuilder()
                        .body(ProgressResponseBody(url, response.body!!))
                        .build()
                }
                .addInterceptor { chain ->
                    // Пропуск варнинга ngrok (если используешь его)
                    val request = chain.request().newBuilder()
                        .addHeader("ngrok-skip-browser-warning", "true")
                        .build()
                    chain.proceed(request)
                }
                .build().also { client = it }
        }
    }

    fun create(baseUrl: String): ApiService {
        val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(cleanUrl)
            .client(getOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

// --- ВСПОМОГАТЕЛЬНЫЙ КЛАСС ДЛЯ ЧТЕНИЯ БАЙТОВ ---
class ProgressResponseBody(
    private val url: String,
    private val responseBody: ResponseBody
) : ResponseBody() {
    override fun contentType() = responseBody.contentType()
    override fun contentLength() = responseBody.contentLength()
    override fun source(): BufferedSource = object : ForwardingSource(responseBody.source()) {
        var totalBytesRead = 0L
        override fun read(sink: Buffer, byteCount: Long): Long {
            val bytesRead = super.read(sink, byteCount)
            if (bytesRead != -1L) {
                totalBytesRead += bytesRead
                val progress = if (contentLength() > 0) totalBytesRead.toFloat() / contentLength() else -1f
                ProgressTracker.update(url, progress)
            }
            return bytesRead
        }
    }.buffer()
}