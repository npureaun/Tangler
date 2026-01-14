package com.example.tangler.service.gptapi

import com.example.tangler.service.gptapi.dto.GptRequest
import com.example.tangler.service.gptapi.dto.GptResponse
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
