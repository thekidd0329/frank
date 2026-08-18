package com.thekidd0329.frank.controller

import com.thekidd0329.frank.model.*
import com.thekidd0329.frank.runtime.*
import com.thekidd0329.frank.storage.Repository

/**
 * FrAnK's central coordinator.
 *
 * UI talks to this controller. The controller talks to reasoning, runtime,
 * speech, health, and persistence. None of those layers need to know about UI.
 */
class AgentController(
    private val runtime: RuntimeAdapter,
    private val reasoning: ReasoningAdapter,
    private val speech: SpeechAdapter,
    private val health: HealthAdapter,
    private val repository: Repository
) {
    data class State(
        val assistantState: AssistantState = AssistantState.IDLE,
        val conversation: Conversation? = null,
        val messages: List<Message> = emptyList(),
        val currentTask: FrankTask? = null,
        val pendingConfirmation: AgentAction? = null,
        val error: String? = null,
        val bootReady: Boolean = false
    )

    private var state = State()
    private val listeners = linkedSetOf<(State) -> Unit>()

    fun getState(): State = state

    fun subscribe(listener: (State) -> Unit): () -> Unit {
        listeners += listener
        listener(state)
        return { listeners -= listener }
    }

    private fun update(transform: (State) -> State) {
        state = transform(state)
        listeners.forEach { it(state) }
    }

    suspend fun boot() {
        try {
            reasoning.initialize()
            val conversation = repository.listConversations().firstOrNull() ?: Conversation().also {
                repository.saveConversation(it)
            }
            val messages = repository.listMessages(conversation.id)
            update { it.copy(conversation = conversation, messages = messages, bootReady = true) }
            repository.addLog(LogLevel.INFO, "FrAnK core booted")
        } catch (t: Throwable) {
            fail(t)
        }
    }

    suspend fun handleUserInput(text: String) {
        val conversation = state.conversation ?: return
        val userMessage = Message(
            conversationId = conversation.id,
            role = MessageRole.USER,
            content = text.trim()
        )
        repository.addMessage(userMessage)
        update { it.copy(messages = it.messages + userMessage, assistantState = AssistantState.THINKING) }

        try {
            val context = runtime.getCurrentContext()
            val result = reasoning.processUserInput(text, context)

            result.responseText?.let { reply ->
                val message = Message(
                    conversationId = conversation.id,
                    role = MessageRole.ASSISTANT,
                    content = reply
                )
                repository.addMessage(message)
                update { it.copy(messages = it.messages + message) }
            }

            result.task?.let { task ->
                repository.saveTask(task)
                update { it.copy(currentTask = task, assistantState = AssistantState.WORKING) }
                health.taskStarted(task)
            } ?: update { it.copy(assistantState = AssistantState.IDLE) }
        } catch (t: Throwable) {
            fail(t)
        }
    }

    suspend fun runNextAction() {
        val task = state.currentTask ?: return
        try {
            val context = runtime.getCurrentContext()
            val action = reasoning.planNextAction(task, context)
            if (action == null) {
                val done = task.copy(status = TaskStatus.COMPLETED, updatedAt = System.currentTimeMillis())
                repository.saveTask(done)
                health.taskCompleted(done)
                update { it.copy(currentTask = null, assistantState = AssistantState.IDLE) }
                return
            }

            if (action.requiresConfirmation) {
                update { it.copy(pendingConfirmation = action, assistantState = AssistantState.WAITING) }
                return
            }

            execute(action)
        } catch (t: Throwable) {
            fail(t)
        }
    }

    suspend fun resolveConfirmation(approved: Boolean) {
        val action = state.pendingConfirmation ?: return
        update { it.copy(pendingConfirmation = null) }
        if (approved) execute(action.copy(status = ActionStatus.APPROVED))
        else update { it.copy(assistantState = AssistantState.IDLE) }
    }

    private suspend fun execute(action: AgentAction) {
        update { it.copy(assistantState = AssistantState.WORKING) }
        runtime.executeAction(action)
        runNextAction()
    }

    suspend fun pauseTask() {
        runtime.pause()
        update { it.copy(assistantState = AssistantState.PAUSED) }
    }

    suspend fun resumeTask() {
        runtime.resume()
        update { it.copy(assistantState = AssistantState.WORKING) }
    }

    suspend fun cancelTask() {
        reasoning.cancel()
        runtime.cancelCurrentAction()
        state.currentTask?.let {
            repository.saveTask(it.copy(status = TaskStatus.CANCELLED, updatedAt = System.currentTimeMillis()))
        }
        update { it.copy(currentTask = null, pendingConfirmation = null, assistantState = AssistantState.IDLE) }
    }

    suspend fun startListening() {
        update { it.copy(assistantState = AssistantState.LISTENING) }
        val heard = speech.startListening()
        if (!heard.isNullOrBlank()) handleUserInput(heard)
        else update { it.copy(assistantState = AssistantState.IDLE) }
    }

    suspend fun speak(text: String) {
        update { it.copy(assistantState = AssistantState.SPEAKING) }
        speech.speak(text)
        update { it.copy(assistantState = AssistantState.IDLE) }
    }

    private suspend fun fail(t: Throwable) {
        repository.addLog(LogLevel.ERROR, t.message ?: t::class.simpleName.orEmpty())
        state.currentTask?.let { health.taskFailed(it, t) }
        update { it.copy(assistantState = AssistantState.ERROR, error = t.message ?: "Unknown error") }
    }
}
