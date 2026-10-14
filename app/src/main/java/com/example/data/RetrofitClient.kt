package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null,
    val tools: List<ToolItem>? = null
)

@JsonClass(generateAdapter = true)
data class ToolItem(
    val googleSearch: Map<String, String>? = null,
    val googleMaps: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

interface GeminiApiService {
    @POST("v1beta/models/{modelId}:generateContent")
    suspend fun generateContent(
        @Path("modelId") modelId: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }

    suspend fun generateContentCached(modelId: String, apiKey: String, request: GenerateContentRequest): GenerateContentResponse {
        val adapter = moshi.adapter(GenerateContentRequest::class.java)
        val requestJson = adapter.toJson(request)
        // Ensure same request body creates identical cache key
        val cacheKey = "gemini_cache_" + requestJson.hashCode().toString()
        
        val cached = LocalStorageCache.get(cacheKey)
        val responseAdapter = moshi.adapter(GenerateContentResponse::class.java)
        if (cached != null) {
            try {
                val response = responseAdapter.fromJson(cached)
                if (response != null) return response
            } catch (e: Exception) {
                // If parsing fails, fall through to network
            }
        }
        
        val response = service.generateContent(modelId, apiKey, request)
        LocalStorageCache.put(cacheKey, responseAdapter.toJson(response))
        return response
    }
}
