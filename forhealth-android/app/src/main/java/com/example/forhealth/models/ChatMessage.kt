package com.example.forhealth.models

data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val text: String
)

enum class MessageRole {
    USER,
    MODEL
}

