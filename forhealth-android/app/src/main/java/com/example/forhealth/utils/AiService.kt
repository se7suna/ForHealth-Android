package com.example.forhealth.utils

import com.example.forhealth.models.ChatMessage
import com.example.forhealth.models.DailyStats
import com.example.forhealth.models.UserProfile

/**
 * AI服务类 - 处理与Gemini AI的交互
 * 注意：这是一个简化版本，实际使用时需要集成Google Gemini SDK
 */
object AiService {
    
    /**
     * 获取聊天回复
     */
    suspend fun getChatResponse(
        userMessage: String,
        conversationHistory: List<ChatMessage>,
        userProfile: UserProfile,
        currentStats: DailyStats
    ): String {
        // TODO: 集成Google Gemini SDK
        // 这里是一个模拟实现，实际应该调用Gemini API
        
        // 构建系统提示
        val systemPrompt = buildSystemPrompt(userProfile, currentStats)
        
        // 构建对话历史
        val chatHistory = conversationHistory.joinToString("\n") { msg ->
            "${if (msg.role == com.example.forhealth.models.MessageRole.USER) "User" else "Model"}: ${msg.text}"
        }
        
        val fullPrompt = "$systemPrompt\n\n$chatHistory\nUser: $userMessage\nModel:"
        
        // 模拟AI响应（实际应该调用Gemini API）
        return simulateAiResponse(userMessage, currentStats)
    }
    
    private fun buildSystemPrompt(
        userProfile: UserProfile,
        currentStats: DailyStats
    ): String {
        return """
            You are a friendly, encouraging, and knowledgeable nutritionist AI.
            User Profile: ${userProfile.age} years old, ${userProfile.height}cm, ${userProfile.gender}.
            Current Daily Stats: 
            - Calories: ${currentStats.calories.current} / ${currentStats.calories.target}
            - Protein: ${currentStats.protein.current}g
            - Carbs: ${currentStats.carbs.current}g
            - Fat: ${currentStats.fat.current}g
            
            Keep answers concise (under 80 words if possible) unless asked for a detailed plan. 
            Be empathetic and scientific.
        """.trimIndent()
    }
    
    private fun simulateAiResponse(
        userMessage: String,
        currentStats: DailyStats
    ): String {
        // 简单的关键词匹配模拟响应
        val message = userMessage.lowercase()
        
        return when {
            message.contains("protein") || message.contains("蛋白质") -> {
                "Great question! To increase protein intake, try adding lean meats, eggs, Greek yogurt, or legumes to your meals. Your current protein is ${Math.round(currentStats.protein.current)}g, which is ${if (currentStats.protein.current < currentStats.protein.target * 0.8) "below" else "close to"} your target."
            }
            message.contains("calorie") || message.contains("卡路里") -> {
                "You've consumed ${Math.round(currentStats.calories.current)} calories today, with a target of ${currentStats.calories.target}. ${if (currentStats.calories.current < currentStats.calories.target * 0.8) "You have room for more nutritious foods!" else "You're doing well with your calorie intake."}"
            }
            message.contains("weight") || message.contains("体重") -> {
                "Maintaining a healthy weight involves balancing calories in and out. Focus on nutrient-dense foods and regular exercise. Would you like specific meal suggestions?"
            }
            message.contains("meal") || message.contains("meal suggestion") || message.contains("食物") -> {
                "Based on your current stats, I'd suggest adding more vegetables, lean proteins, and whole grains. Try a balanced plate: 1/2 vegetables, 1/4 protein, and 1/4 whole grains."
            }
            else -> {
                "I understand you're asking about nutrition. Based on your current daily stats, I'd recommend focusing on balanced meals with adequate protein, carbs, and healthy fats. Would you like more specific advice on any particular aspect?"
            }
        }
    }
}

