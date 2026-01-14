package com.example.tangler.service.aiapi.gpt

interface AIManager {
    fun requestGptResponse(userInput: String, onResult: (String?) -> Unit)
}