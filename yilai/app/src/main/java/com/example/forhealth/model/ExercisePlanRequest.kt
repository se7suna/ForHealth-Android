package com.example.forhealth.model

data class ExercisePlanRequest(
    val model: String = "deepseek-chat",  // 使用 DeepSeek 的 chat 模型
    val messages: List<Message>,
    val stream: Boolean = false           // 如果需要流式输出设置为 true
)

data class Message(
    val role: String,       // 角色，system 或 user
    val content: String     // 消息内容
)
