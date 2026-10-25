package com.example.bookswap.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bookswap.data.model.ExchangeRequest
import com.example.bookswap.data.model.RequestStatus
import com.example.bookswap.data.model.statusEnum
import com.example.bookswap.ui.components.EmptyState
import com.example.bookswap.ui.components.LoadingBox
import com.example.bookswap.ui.components.Pill
import com.example.bookswap.utils.conversationId
import com.example.bookswap.utils.formatDate
import com.example.bookswap.utils.formatTime
import com.example.bookswap.viewmodel.AuthViewModel
import com.example.bookswap.viewmodel.BookViewModel
import com.example.bookswap.viewmodel.SocialViewModel

@Composable
private fun RequestStatusPill(status: RequestStatus) {
    val s = MaterialTheme.colorScheme
    when (status) {
        RequestStatus.PENDING -> Pill(status.label, s.secondaryContainer, s.onSecondaryContainer)
        RequestStatus.ACCEPTED, RequestStatus.COMPLETED -> Pill(status.label, s.tertiary.copy(alpha = 0.15f), s.tertiary)
        else -> Pill(status.label, s.surfaceVariant, s.onSurfaceVariant)
    }
}

@Composable
fun RequestsScreen(
    socialVm: SocialViewModel,
    bookVm: BookViewModel,
    authVm: AuthViewModel,
    onRequest: (String) -> Unit,
    onChat: (String) -> Unit
) {
    val requests by socialVm.requests.collectAsStateWithLifecycle()
    val conversations by socialVm.conversations.collectAsStateWithLifecycle()
    val books by bookVm.allBooks.collectAsStateWithLifecycle()
    val users by bookVm.users.collectAsStateWithLifecycle()
    val me by authVm.currentUserId.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(0) }
    val titles = listOf("Received", "Sent", "Chats")
    val bookById = books.orEmpty().associateBy { it.bookId }

    Scaffold(topBar = {
        Column {
            Text("Requests", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
            TabRow(selectedTabIndex = tab) {
                titles.forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = {
                        if (i == 2 && conversations.sumOf { it.unread } > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) { Text(t); Text(" "); Badge { Text(conversations.sumOf { it.unread }.toString()) } }
                        } else Text(t)
                    })
                }
            }
        }
    }) { padding ->
        if (tab < 2) {
            val shown = requests.filter { if (tab == 0) it.receiverId == me else it.senderId == me }
            if (shown.isEmpty()) {
                EmptyState(
                    Icons.Filled.SwapHoriz,
                    if (tab == 0) "No requests received" else "No requests sent",
                    if (tab == 0) "When someone asks for one of your books, it will show up here."
                    else "Open a book and tap “Request exchange” to get started.",
                    Modifier.padding(padding)
                )
            } else {
                LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(shown, key = { it.requestId }) { r ->
                        val otherId = if (r.senderId == me) r.receiverId else r.senderId
                        Card(
                            onClick = { onRequest(r.requestId) }, modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(bookById[r.bookId]?.title ?: "Deleted book", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    RequestStatusPill(r.statusEnum())
                                }
                                Text(
                                    (if (tab == 0) "From " else "To ") + (users[otherId]?.name ?: "student"),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                r.offeredBookId?.let { Text("Offers: ${bookById[it]?.title ?: "a book"}", style = MaterialTheme.typography.bodySmall) }
                                Text(formatDate(r.updatedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        } else {
            if (conversations.isEmpty()) {
                EmptyState(Icons.AutoMirrored.Filled.Chat, "No chats yet", "Use “Contact seller” on a book to start a conversation.", Modifier.padding(padding))
            } else {
                LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(conversations, key = { it.id }) { c ->
                        Card(
                            onClick = { onChat(c.id) }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(users[c.otherUserId]?.name ?: "Student", fontWeight = FontWeight.SemiBold)
                                    Text("About: ${bookById[c.bookId]?.title ?: "deleted book"}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                                    Text(c.lastMessage, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (c.unread > 0) Badge { Text(c.unread.toString()) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailsScreen(
    requestId: String,
    socialVm: SocialViewModel,
    bookVm: BookViewModel,
    authVm: AuthViewModel,
    onBack: () -> Unit,
    onBook: (String) -> Unit,
    onChat: (String) -> Unit
) {
    val request by remember(requestId) { socialVm.observeRequest(requestId) }.collectAsState(initial = null)
    val books by bookVm.allBooks.collectAsStateWithLifecycle()
    val users by bookVm.users.collectAsStateWithLifecycle()
    val me by authVm.currentUserId.collectAsStateWithLifecycle()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Request details") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
        )
    }) { padding ->
        val r = request
        if (r == null) {
            if (books == null) LoadingBox(Modifier.padding(padding))
            else EmptyState(Icons.Filled.SwapHoriz, "Request not found", "It may have been removed.", Modifier.padding(padding))
            return@Scaffold
        }
        val byId = books.orEmpty().associateBy { it.bookId }
        val book = byId[r.bookId]
        val offered = r.offeredBookId?.let { byId[it] }
        val isReceiver = me == r.receiverId
        val otherId = if (isReceiver) r.senderId else r.receiverId
        val status = r.statusEnum()

        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Status", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                RequestStatusPill(status)
            }
            DetailCard("Requested book", book?.title ?: "This book was deleted", book?.author) { book?.let { onBook(it.bookId) } }
            if (r.offeredBookId != null) {
                DetailCard("Offered in exchange", offered?.title ?: "This book is no longer available", offered?.author) { offered?.let { onBook(it.bookId) } }
            } else {
                Text("No book offered — this is a purchase request.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                (if (isReceiver) "From: " else "To: ") + (users[otherId]?.name ?: "student") +
                    " · " + formatTime(r.createdAt),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (r.message.isNotBlank()) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(r.message, Modifier.padding(14.dp).fillMaxWidth())
                }
            }

            if (isReceiver && status == RequestStatus.PENDING) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { socialVm.updateRequest(r.requestId, RequestStatus.ACCEPTED) }, modifier = Modifier.weight(1f)) { Text("Accept") }
                    OutlinedButton(onClick = { socialVm.updateRequest(r.requestId, RequestStatus.REJECTED) }, modifier = Modifier.weight(1f)) { Text("Reject") }
                }
            }
            if (!isReceiver && status == RequestStatus.PENDING) {
                OutlinedButton(onClick = { socialVm.updateRequest(r.requestId, RequestStatus.CANCELLED) }, modifier = Modifier.fillMaxWidth()) { Text("Cancel request") }
            }
            if (status == RequestStatus.ACCEPTED) {
                Button(onClick = { socialVm.updateRequest(r.requestId, RequestStatus.COMPLETED) }, modifier = Modifier.fillMaxWidth()) { Text("Mark deal completed") }
                OutlinedButton(onClick = { socialVm.updateRequest(r.requestId, RequestStatus.CANCELLED) }, modifier = Modifier.fillMaxWidth()) { Text("Cancel deal") }
            }
            me?.let { uid ->
                OutlinedButton(onClick = { onChat(conversationId(uid, otherId, r.bookId)) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Filled.Chat, null, Modifier.size(18.dp)); Text("  Message ${users[otherId]?.name ?: "student"}")
                }
            }
        }
    }
}

@Composable
private fun DetailCard(label: String, title: String, subtitle: String?, onClick: () -> Unit) {
    Card(
        onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    socialVm: SocialViewModel,
    bookVm: BookViewModel,
    authVm: AuthViewModel,
    onBack: () -> Unit
) {
    val messages by remember(conversationId) { socialVm.observeMessages(conversationId) }.collectAsState(initial = emptyList())
    val users by bookVm.users.collectAsStateWithLifecycle()
    val books by bookVm.allBooks.collectAsStateWithLifecycle()
    val me by authVm.currentUserId.collectAsStateWithLifecycle()
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val parts = conversationId.split("_")
    val otherId = if (parts.getOrNull(0) == me) parts.getOrNull(1).orEmpty() else parts.getOrNull(0).orEmpty()
    val bookTitle = books.orEmpty().firstOrNull { it.bookId == parts.getOrNull(2) }?.title

    // Mark incoming messages as read whenever the list changes while this screen is open.
    LaunchedEffect(messages.size) {
        socialVm.markMessagesRead(conversationId)
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(users[otherId]?.name ?: "Chat", style = MaterialTheme.typography.titleMedium)
                        if (bookTitle != null) Text(bookTitle, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().imePadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    text, { text = it }, placeholder = { Text("Message") },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp), maxLines = 4
                )
                IconButton(onClick = { socialVm.sendMessage(conversationId, text); text = "" }, enabled = text.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    ) { padding ->
        if (messages.isEmpty()) {
            EmptyState(Icons.AutoMirrored.Filled.Chat, "Say hello!", "Ask about the book's condition or arrange a meeting on campus.", Modifier.padding(padding))
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(), state = listState,
                contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages, key = { it.messageId }) { m ->
                    val mine = m.senderId == me
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                                Text(m.text, color = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                Text(
                                    formatTime(m.timestamp) + if (mine && m.isRead) " · Seen" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = (if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
