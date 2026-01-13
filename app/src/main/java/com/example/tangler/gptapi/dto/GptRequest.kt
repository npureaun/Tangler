package com.example.tangler.gptapi.dto


data class GptRequest(
    val model: String = "gpt-5.2-chat-latest",  //
    val messages: List<Message>
)

data class Message(
    val role: String,  // "user" or "system" or "assistant"
    val content: String
)