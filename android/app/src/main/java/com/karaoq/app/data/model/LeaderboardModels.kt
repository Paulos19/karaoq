package com.karaoq.app.data.model

import com.google.gson.annotations.SerializedName

data class LeaderboardEntry(
    @SerializedName("id") val id: String,
    @SerializedName("song_id") val songId: String,
    @SerializedName("singer_name") val singerName: String,
    @SerializedName("score") val score: Int,
    @SerializedName("rank") val rank: String,
    @SerializedName("max_combo") val maxCombo: Int,
    @SerializedName("perfect_hits") val perfectHits: Int = 0,
    @SerializedName("created_at") val createdAt: Double
)

data class LeaderboardSubmitRequest(
    @SerializedName("singer_name") val singerName: String,
    @SerializedName("score") val score: Int,
    @SerializedName("rank") val rank: String,
    @SerializedName("max_combo") val maxCombo: Int,
    @SerializedName("perfect_hits") val perfectHits: Int = 0
)
