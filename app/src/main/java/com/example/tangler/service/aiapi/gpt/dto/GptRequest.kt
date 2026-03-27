package com.example.tangler.service.aiapi.gpt.dto

import com.example.tangler.service.aiapi.gpt.GptConfig


data class GptRequest(
    val model: String = GptConfig.getModel(),  //
    val messages: List<Message>
)

data class Message(
    val role: String,  // "user" or "system" or "assistant"
    val content: String
)