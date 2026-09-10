package com.karaoq.app.data.model

import com.google.gson.annotations.SerializedName

data class SeparationCreateResponse(
    @SerializedName("task_id")
    val taskId: String,
    @SerializedName("filename")
    val filename: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String
)

data class SeparationStatusResponse(
    @SerializedName("task_id")
    val taskId: String,
    @SerializedName("original_filename")
    val originalFilename: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("progress_percentage")
    val progressPercentage: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("vocals_url")
    val vocalsUrl: String?,
    @SerializedName("instrumental_url")
    val instrumentalUrl: String?,
    @SerializedName("error")
    val error: String?
)

enum class JobStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED,
    UNKNOWN;

    companion object {
        fun fromString(status: String?): JobStatus {
            return when (status?.uppercase()) {
                "QUEUED" -> QUEUED
                "PROCESSING" -> PROCESSING
                "COMPLETED" -> COMPLETED
                "FAILED" -> FAILED
                else -> UNKNOWN
            }
        }
    }
}
