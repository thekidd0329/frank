package com.thekidd0329.frank.runtime

import com.thekidd0329.frank.model.*

interface RuntimeAdapter {
    suspend fun getStatus(): RuntimeStatus
    suspend fun getCurrentContext(): RuntimeContext
    suspend fun executeAction(action: AgentAction): AgentAction
    suspend fun pause()
    suspend fun resume()
    suspend fun cancelCurrentAction()
}

interface ReasoningAdapter {
    suspend fun initialize()
    suspend fun processUserInput(input: String, context: RuntimeContext): ReasoningResult
    suspend fun planNextAction(task: FrankTask, context: RuntimeContext): AgentAction?
    suspend fun cancel()
}

interface SpeechAdapter {
    suspend fun startListening(): String?
    suspend fun stopListening()
    suspend fun speak(text: String)
    suspend fun stopSpeaking()
}

interface HealthAdapter {
    suspend fun snapshot(): HealthSnapshot
    suspend fun taskStarted(task: FrankTask)
    suspend fun taskCompleted(task: FrankTask)
    suspend fun taskFailed(task: FrankTask, error: Throwable)
}
