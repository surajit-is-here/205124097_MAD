package com.example.bookswap.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookswap.data.model.AppNotification
import com.example.bookswap.data.model.Book
import com.example.bookswap.data.model.Conversation
import com.example.bookswap.data.model.ExchangeRequest
import com.example.bookswap.data.model.Message
import com.example.bookswap.data.model.RequestStatus
import com.example.bookswap.data.repository.AppRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SocialViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppRepository.get(app)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val uid = repo.currentUserId

    val requests: StateFlow<List<ExchangeRequest>> = uid
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.observeRequests(id) }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val notifications: StateFlow<List<AppNotification>> = uid
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.observeNotifications(id) }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val unreadCount: StateFlow<Int> = uid
        .flatMapLatest { id -> if (id == null) flowOf(0) else repo.observeUnreadCount(id) }
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val conversations: StateFlow<List<Conversation>> = uid
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repo.observeMessagesFor(id).map { list -> buildConversations(id, list) }
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun buildConversations(me: String, list: List<Message>): List<Conversation> =
        list.groupBy { it.conversationId }.map { (cid, msgs) ->
            val last = msgs.first() // list is ordered newest first
            val parts = cid.split("_")
            Conversation(
                id = cid,
                otherUserId = if (last.senderId == me) last.receiverId else last.senderId,
                bookId = parts.getOrElse(2) { "" },
                lastMessage = last.text,
                lastTime = last.timestamp,
                unread = msgs.count { !it.isRead && it.receiverId == me }
            )
        }.sortedByDescending { it.lastTime }

    fun observeRequest(id: String): Flow<ExchangeRequest?> = repo.observeRequest(id)
    fun observeMessages(conversationId: String): Flow<List<Message>> = repo.observeMessages(conversationId)

    fun sendRequest(book: Book, offeredBookId: String?, message: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.sendRequest(book, offeredBookId, message)
                .onSuccess {
                    _messages.tryEmit("Exchange request sent")
                    onDone()
                }
                .onFailure { _messages.tryEmit(it.message ?: "Could not send request") }
        }
    }

    fun updateRequest(requestId: String, status: RequestStatus) {
        viewModelScope.launch {
            repo.updateRequestStatus(requestId, status)
                .onSuccess { _messages.tryEmit("Request ${status.label.lowercase()}") }
                .onFailure { _messages.tryEmit(it.message ?: "Could not update request") }
        }
    }

    fun sendMessage(conversationId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repo.sendMessage(conversationId, text)
                .onFailure { _messages.tryEmit(it.message ?: "Could not send message") }
        }
    }

    fun markMessagesRead(conversationId: String) {
        viewModelScope.launch { repo.markMessagesRead(conversationId) }
    }

    fun markNotificationsRead() {
        viewModelScope.launch { repo.markAllNotificationsRead() }
    }

    fun report(bookId: String, reportedUserId: String, reason: String, description: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.report(bookId, reportedUserId, reason, description)
                .onSuccess {
                    _messages.tryEmit("Report submitted. Thank you!")
                    onDone()
                }
                .onFailure { _messages.tryEmit(it.message ?: "Could not submit report") }
        }
    }
}
