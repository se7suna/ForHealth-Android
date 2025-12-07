package com.example.forhealth.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.forhealth.R
import com.example.forhealth.databinding.FragmentAiChatBinding
import com.example.forhealth.databinding.ItemChatMessageBinding
import com.example.forhealth.models.ChatMessage
import com.example.forhealth.models.DailyStats
import com.example.forhealth.models.MessageRole
import com.example.forhealth.models.UserProfile
import com.example.forhealth.utils.AiService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class AiChatFragment : DialogFragment() {
    
    private var _binding: FragmentAiChatBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var messagesAdapter: MessagesAdapter
    private val messages = mutableListOf<ChatMessage>()
    
    private var userProfile: UserProfile? = null
    private var currentStats: DailyStats? = null
    private var initialContext: String? = null
    
    fun setUserProfile(profile: UserProfile) {
        userProfile = profile
    }
    
    fun setCurrentStats(stats: DailyStats) {
        currentStats = stats
    }
    
    fun setInitialContext(context: String) {
        initialContext = context
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_NoTitleBar_Fullscreen)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiChatBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupClickListeners()
        setupInputListener()
        
        // 添加初始消息
        val profile = userProfile ?: UserProfile.getInitial()
        val stats = currentStats ?: com.example.forhealth.models.DailyStats.getInitial()
        val firstName = profile.name.split(' ').firstOrNull() ?: profile.name
        val initialMessage = ChatMessage(
            id = "init",
            role = MessageRole.MODEL,
            text = "Hi $firstName! I'm your personal nutrition assistant. I see you've consumed ${Math.round(stats.calories.current)} calories today. How can I help you reach your goals?"
        )
        messages.add(initialMessage)
        messagesAdapter.notifyItemInserted(messages.size - 1)
        scrollToBottom()
        
        // 显示上下文提示（如果有）
        initialContext?.let { context ->
            if (context.isNotEmpty()) {
                binding.layoutContextBubble.visibility = View.VISIBLE
                binding.tvContextBubble.text = getString(R.string.talking_about, context)
            }
        }
    }
    
    private fun setupRecyclerView() {
        messagesAdapter = MessagesAdapter(messages)
        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = messagesAdapter
    }
    
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            dismiss()
        }
        
        binding.btnSend.setOnClickListener {
            sendMessage()
        }
    }
    
    private fun setupInputListener() {
        binding.etInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.btnSend.isEnabled = !s.isNullOrBlank()
                binding.btnSend.alpha = if (s.isNullOrBlank()) 0.5f else 1.0f
            }
        })
        
        binding.etInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }
    
    private fun sendMessage() {
        val inputText = binding.etInput.text.toString().trim()
        if (inputText.isEmpty()) return
        
        // 添加用户消息
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            text = inputText
        )
        messages.add(userMessage)
        messagesAdapter.notifyItemInserted(messages.size - 1)
        scrollToBottom()
        
        // 清空输入框
        binding.etInput.setText("")
        
        // 显示加载状态
        binding.progressLoading.visibility = View.VISIBLE
        binding.btnSend.isEnabled = false
        
        // 发送到AI
        lifecycleScope.launch {
            try {
                val profile = userProfile ?: UserProfile.getInitial()
                val stats = currentStats ?: com.example.forhealth.models.DailyStats.getInitial()
                
                val response = withContext(Dispatchers.IO) {
                    AiService.getChatResponse(
                        userMessage = inputText,
                        conversationHistory = messages.dropLast(1), // 不包括刚添加的用户消息
                        userProfile = profile,
                        currentStats = stats
                    )
                }
                
                // 添加AI回复
                val modelMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = MessageRole.MODEL,
                    text = response
                )
                messages.add(modelMessage)
                messagesAdapter.notifyItemInserted(messages.size - 1)
                scrollToBottom()
                
            } catch (e: Exception) {
                // 添加错误消息
                val errorMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = MessageRole.MODEL,
                    text = "Sorry, I encountered an error. Please try again."
                )
                messages.add(errorMessage)
                messagesAdapter.notifyItemInserted(messages.size - 1)
                scrollToBottom()
            } finally {
                binding.progressLoading.visibility = View.GONE
                binding.btnSend.isEnabled = true
            }
        }
    }
    
    private fun scrollToBottom() {
        binding.rvMessages.post {
            if (messages.isNotEmpty()) {
                binding.rvMessages.smoothScrollToPosition(messages.size - 1)
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    // Messages Adapter
    private class MessagesAdapter(
        private val messages: List<ChatMessage>
    ) : RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
            val binding = ItemChatMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return MessageViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
            holder.bind(messages[position])
        }
        
        override fun getItemCount() = messages.size
        
        class MessageViewHolder(
            private val binding: ItemChatMessageBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            
            fun bind(message: ChatMessage) {
                when (message.role) {
                    MessageRole.USER -> {
                        binding.layoutUserMessage.visibility = View.VISIBLE
                        binding.layoutModelMessage.visibility = View.GONE
                        binding.tvUserMessage.text = message.text
                    }
                    MessageRole.MODEL -> {
                        binding.layoutUserMessage.visibility = View.GONE
                        binding.layoutModelMessage.visibility = View.VISIBLE
                        binding.tvModelMessage.text = message.text
                    }
                }
            }
        }
    }
}

