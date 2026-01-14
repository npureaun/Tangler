package com.example.tangler.service.aiapi.gpt.dto

data class GptResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)