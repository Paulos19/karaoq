package com.karaoq.app.data.remote

import com.karaoq.app.data.model.SeparationCreateResponse
import com.karaoq.app.data.model.SeparationStatusResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface KaraoqApiService {

    @Multipart
    @POST("api/v1/separate")
    suspend fun separateAudio(
        @Part audioFile: MultipartBody.Part
    ): Response<SeparationCreateResponse>

    @GET("api/v1/separate/{task_id}")
    suspend fun getSeparationStatus(
        @Path("task_id") taskId: String
    ): Response<SeparationStatusResponse>

    @GET("health")
    suspend fun checkHealth(): Response<Map<String, Any>>
}
