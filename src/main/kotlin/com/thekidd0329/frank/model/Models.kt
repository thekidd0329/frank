package com.thekidd0329.frank.model

import java.util.UUID

private fun id(): String = UUID.randomUUID().toString()
private fun now(): Long = System.currentTimeMillis()

data class TaskStep(
    val id: String = id(),
    val title: String,
    val status: StepStatus = StepStatus.PENDING,
    val result: String? = null,
    val error: String? = null
)

data class FrankTask(
    val id: String = id(),
    val title: String,
    val status: TaskStatus = TaskStatus.QUEUED,
    val steps: List<TaskStep> = emptyList(),
    val createdAt: Long = now(),
    val updatedAt: Long = createdAt,
    val error: String? = null
)

data class AgentAction(
    val id: String = id(),
    val type: String,
    val target: String? = null,
    val payload: Map<String, String> = emptyMap(),
    val status: ActionStatus = ActionStatus.PENDING,
    val requiresConfirmation: Boolean = false
)

data class Conversation(
    val id: String = id(),
    val title: String = "FrAnK",
    val createdAt: Long = now(),
    val updatedAt: Long = createdAt
)

data class Message(
    val id: String = id(),
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val type: MessageType = MessageType.TEXT,
    val createdAt: Long = now(),
    val taskId: String? = null
)

data class RuntimeContext(
    val timestamp: Long = now(),
    val locale: String = "en-US",
    val foregroundPackage: String? = null,
    val screenSummary: String? = null,
    val networkAvailable: Boolean = false
)

data class RuntimeStatus(
    val online: Boolean,
    val offlineMode: Boolean,
    val allowNetwork: Boolean,
    val preferLocal: Boolean,
    val capabilities: Set<String>
)

data class HealthSnapshot(
    val overall: HealthStatus,
    val uptimeMs: Long,
    val currentTaskId: String? = null,
    val tasksCompleted: Long = 0,
    val tasksFailed: Long = 0,
    val lastError: String? = null
)

data class ReasoningResult(
    val responseText: String? = null,
    val task: FrankTask? = null
)
