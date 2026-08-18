package com.thekidd0329.frank.storage

import com.thekidd0329.frank.model.*

interface Repository {
    suspend fun saveConversation(conversation: Conversation)
    suspend fun listConversations(): List<Conversation>

    suspend fun addMessage(message: Message)
    suspend fun listMessages(conversationId: String): List<Message>

    suspend fun saveTask(task: FrankTask)
    suspend fun getTask(id: String): FrankTask?
    suspend fun listTasks(): List<FrankTask>

    suspend fun addLog(level: LogLevel, message: String)
}
