package com.example.tangler.gptapi.dto

data class GptResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)