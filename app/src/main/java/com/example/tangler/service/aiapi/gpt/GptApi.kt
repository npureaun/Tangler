package com.example.tangler.service.aiapi.gpt

import com.example.tangler.service.aiapi.gpt.dto.GptRequest
import com.example.tangler.service.aiapi.gpt.dto.GptResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GptApi {
    @POST("v1/chat/completions")
    fun getChatResponse(
        @Header("Authorization") authHeader: String,
        @Body request: GptRequest
    ): Call<GptResponse>
}