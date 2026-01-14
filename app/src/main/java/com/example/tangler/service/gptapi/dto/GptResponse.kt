package com.example.tangler.service.gptapi.dto

data class GptResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)