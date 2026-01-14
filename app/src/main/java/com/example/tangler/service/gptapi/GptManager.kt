package com.example.tangler.service.gptapi

import com.example.tangler.BuildConfig
import com.example.tangler.service.gptapi.dto.GptRequest
import com.example.tangler.service.gptapi.dto.GptResponse
import com.example.tangler.service.gptapi.dto.Message
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class GptManager {
    private val apiKey = BuildConfig.GPT_KEY
    fun requestGptResponse(userInput: String, onResult: (String?) -> Unit) {
        val request = GptRequest(
            messages = listOf(
                Message(
                    "system",
                    """
                        You are tasked with paraphrasing English text into **natural, conversational Korean**. Follow these guidelines:
                        1. Avoid literal or machine-like translations. Instead, use fluent, idiomatic Korean expressions.
                        2. If the input includes slang, abbreviations, grammatical errors, or typos, **infer the intended meaning from context** and render it naturally in Korean.
                        3. When the input contains sexual innuendo, jokes, or profanity, **reflect the tone and intent appropriately in Korean**, even if it requires bold or explicit language.
                        4. Use expressions familiar to younger Korean speakers. It's okay to be casual, direct, or even coarse if needed.
                        5. You may break sentences or add line breaks for better readability or emotional effect.
                        6. **Do not add your own interpretation, commentary, or explanation. Translate only the meaning and tone intended by the original speaker.**
                        7. **If any part of the input contains language that could violate OpenAI's content policies** (e.g., excessive violence, explicit sexual content, hate speech), 
                            **paraphrase it in a more toned-down or euphemistic way without losing the original intent or tone.**
                        """
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