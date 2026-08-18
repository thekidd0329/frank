package com.thekidd0329.frank.model

enum class AssistantState {
    IDLE,
    LISTENING,
    THINKING,
    WORKING,
    SPEAKING,
    WAITING,
    PAUSED,
    OFFLINE,
    ERROR
}

enum class TaskStatus {
    QUEUED,
    STARTING,
    OBSERVING,
    THINKING,
    ACTING,
    VERIFYING,
    WAITING_FOR_USER,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class StepStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    SKIPPED
}

enum class ActionStatus {
    PENDING,
    APPROVED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class MessageRole { USER, ASSISTANT, SYSTEM }
enum class MessageType { TEXT, VOICE, TASK, NOTICE, ERROR }
enum class LogLevel { DEBUG, INFO, WARNING, ERROR }
enum class HealthStatus { HEALTHY, WARNING, ERROR, OFFLINE }
