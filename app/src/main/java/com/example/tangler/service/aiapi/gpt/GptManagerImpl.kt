package com.example.tangler.service.aiapi.gpt

import com.example.tangler.BuildConfig
import com.example.tangler.service.aiapi.gpt.dto.GptRequest
import com.example.tangler.service.aiapi.gpt.dto.GptResponse
import com.example.tangler.service.aiapi.gpt.dto.Message
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class GptManagerImpl: AIManager {
    private val apiKey = BuildConfig.GPT_KEY
    override fun requestGptResponse(userInput: String, onResult: (String?) -> Unit) {
        val request = GptRequest(
            messages = listOf(
                Message(
                    "system",
                    GptConfig.Prompt.v3()
                ),
                Message("user", userInput)
            )
        )

        val call = GptService.api.getChatResponse("Bearer $apiKey", request)
        call.enqueue(object : Callback<GptResponse> {
            override fun onResponse(call: Call<GptResponse>, response: Response<GptResponse>) {
                if (response.isSuccessful) {
                    val message = response.body()?.choices?.firstOrNull()?.message?.content
                    onResult(message)

                } else {
                    onResult("API Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<GptResponse>, t: Throwable) {
                onResult("Network Error: ${t.message}")
            }
        })
    }
}