package com.karaoq.app.data.remote

import com.karaoq.app.data.model.SeparationCreateResponse
import com.karaoq.app.data.model.SeparationStatusResponse
import com.karaoq.app.domain.model.LyricsSearchResponseDto
import com.karaoq.app.domain.model.SavedSong
import com.karaoq.app.domain.model.SongCreatePayloadDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface KaraoqApiService {

    // --- Separação de Stems ---
    @Multipart
    @POST("api/v1/separate")
    suspend fun separateAudio(
        @Part audioFile: MultipartBody.Part
    ): Response<SeparationCreateResponse>

    @GET("api/v1/separate/{task_id}")
    suspend fun getSeparationStatus(
        @Path("task_id") taskId: String
    ): Response<SeparationStatusResponse>

    // --- Busca de Letras Sincronizadas ---
    @GET("api/v1/lyrics/search")
    suspend fun searchLyrics(
        @Query("artist") artist: String,
        @Query("title") title: String,
        @Query("duration") durationSeconds: Int? = null
    ): Response<LyricsSearchResponseDto>

    // --- Biblioteca de Músicas no Storage ---
    @GET("api/v1/songs")
    suspend fun getSavedSongs(): Response<List<SavedSong>>

    @GET("api/v1/songs/{song_id}")
    suspend fun getSavedSong(
        @Path("song_id") songId: String
    ): Response<SavedSong>

    @POST("api/v1/songs")
    suspend fun saveSong(
        @Body payload: SongCreatePayloadDto
    ): Response<SavedSong>

    @DELETE("api/v1/songs/{song_id}")
    suspend fun deleteSavedSong(
        @Path("song_id") songId: String
    ): Response<Unit>

    // --- Healthcheck ---
    @GET("health")
    suspend fun checkHealth(): Response<Map<String, Any>>
}
