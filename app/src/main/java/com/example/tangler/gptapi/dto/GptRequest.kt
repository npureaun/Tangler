package com.example.tangler.gptapi.dto


data class GptRequest(
    val model: String = "gpt-4o-mini",  // 또는 gpt-4
    val messages: List<Message>
)

data class Message(
    val role: String,  // "user" or "system" or "assistant"
    val content: String
)