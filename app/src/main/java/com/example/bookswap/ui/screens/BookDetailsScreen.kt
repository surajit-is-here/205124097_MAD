package com.example.bookswap.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bookswap.data.model.Book
import com.example.bookswap.data.model.BookStatus
import com.example.bookswap.data.model.conditionEnum
import com.example.bookswap.data.model.imageList
import com.example.bookswap.data.model.statusEnum
import com.example.bookswap.ui.components.BookImage
import com.example.bookswap.ui.components.ConfirmDialog
import com.example.bookswap.ui.components.DropdownField
import com.example.bookswap.ui.components.EmptyState
import com.example.bookswap.ui.components.LoadingBox
import com.example.bookswap.ui.components.Pill
import com.example.bookswap.ui.components.StatusPill
import com.example.bookswap.utils.Options
import com.example.bookswap.utils.conversationId
import com.example.bookswap.utils.formatDate
import com.example.bookswap.utils.formatPrice
import com.example.bookswap.viewmodel.AuthViewModel
import com.example.bookswap.viewmodel.BookViewModel
import com.example.bookswap.viewmodel.SocialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailsScreen(
    bookId: String,
    bookVm: BookViewModel,
    socialVm: SocialViewModel,
    authVm: AuthViewModel,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onChat: (String) -> Unit
) {
    val book by remember(bookId) { bookVm.observeBook(bookId) }.collectAsState(initial = null)
    val allBooks by bookVm.allBooks.collectAsStateWithLifecycle()
    val users by bookVm.users.collectAsStateWithLifecycle()
    val favorites by bookVm.favoriteIds.collectAsStateWithLifecycle()
    val me by authVm.currentUserId.collectAsStateWithLifecycle()

    var showDelete by remember { mutableStateOf(false) }
    var showRequest by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book details") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    val b = book
                    if (b != null) {
                        val fav = favorites.contains(b.bookId)
                        IconButton(onClick = { bookVm.toggleFavorite(b.bookId) }) {
                            Icon(
                                if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Favorite",
                                tint = if (fav) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        val b = book
        if (b == null) {
            // Either still loading the first emission or the listing was deleted.
            if (allBooks == null) LoadingBox(Modifier.padding(padding))
            else EmptyState(Icons.Filled.Book, "Listing not found", "This book may have been removed by the seller.", Modifier.padding(padding))
            return@Scaffold
        }
        val seller = users[b.sellerId]
        val isOwner = me == b.sellerId

        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            val images = b.imageList()
            if (images.isEmpty()) {
                BookImage(null, Modifier.fillMaxWidth().height(260.dp))
            } else {
                val pagerState = rememberPagerState(pageCount = { images.size })
                Box {
                    HorizontalPager(state = pagerState) { page ->
                        BookImage(images[page], Modifier.fillMaxWidth().height(300.dp), ContentScale.Crop)
                    }
                    if (images.size > 1) {
                        Pill(
                            "${pagerState.currentPage + 1} / ${images.size}",
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), MaterialTheme.colorScheme.onSurface,
                            Modifier.align(Alignment.BottomEnd).padding(12.dp)
                        )
                    }
                }
            }

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(b.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    StatusPill(b.statusEnum())
                }
                Text("by ${b.author}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatPrice(b.price), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(b.conditionEnum().label, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    if (b.exchangeAllowed) Pill("Open to exchange", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                InfoRow("ISBN", b.isbn.ifBlank { "—" })
                InfoRow("Category", b.category)
                InfoRow("Department", b.department)
                InfoRow("Semester", b.semester)
                InfoRow("Listed on", formatDate(b.createdAt))
                if (b.description.isNotBlank()) {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Text("Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(b.description)
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Seller", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(seller?.name ?: "Unknown student", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${b.college} · ${seller?.department ?: b.department}", style = MaterialTheme.typography.bodyMedium)
                        seller?.year?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                Spacer(Modifier.height(4.dp))

                if (isOwner) {
                    Button(onClick = { onEdit(b.bookId) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Edit, null, Modifier.size(18.dp)); Text("  Edit listing")
                    }
                    if (b.statusEnum() == BookStatus.AVAILABLE || b.statusEnum() == BookStatus.RESERVED) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { bookVm.setStatus(b.bookId, BookStatus.SOLD) }, modifier = Modifier.weight(1f)) { Text("Mark sold") }
                            OutlinedButton(onClick = { bookVm.setStatus(b.bookId, BookStatus.EXCHANGED) }, modifier = Modifier.weight(1f)) { Text("Mark exchanged") }
                        }
                    } else {
                        OutlinedButton(onClick = { bookVm.setStatus(b.bookId, BookStatus.AVAILABLE) }, modifier = Modifier.fillMaxWidth()) { Text("Relist as available") }
                    }
                    OutlinedButton(
                        onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.Delete, null, Modifier.size(18.dp)); Text("  Delete listing")
                    }
                } else {
                    val available = b.statusEnum() == BookStatus.AVAILABLE
                    Button(
                        onClick = { me?.let { onChat(conversationId(it, b.sellerId, b.bookId)) } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, null, Modifier.size(18.dp)); Text("  Contact seller")
                    }
                    OutlinedButton(onClick = { showRequest = true }, enabled = available, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.SwapHoriz, null, Modifier.size(18.dp))
                        Text(if (available) "  Request exchange / buy" else "  No longer available")
                    }
                    TextButton(onClick = { showReport = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Icon(Icons.Filled.Flag, null, Modifier.size(18.dp)); Text("  Report listing")
                    }
                }
            }
        }

        if (showDelete) {
            ConfirmDialog(
                "Delete listing?", "This permanently removes \"${b.title}\" and its photos.", "Delete",
                onConfirm = { bookVm.deleteBook(b) { onBack() } }, onDismiss = { showDelete = false }
            )
        }
        if (showRequest) {
            val myAvailable = allBooks.orEmpty().filter { it.sellerId == me && it.status == BookStatus.AVAILABLE.name }
            RequestDialog(
                book = b, myBooks = myAvailable,
                onSend = { offered, msg ->
                    socialVm.sendRequest(b, offered, msg) { showRequest = false }
                },
                onDismiss = { showRequest = false }
            )
        }
        if (showReport) {
            ReportDialog(
                onSubmit = { reason, desc -> socialVm.report(b.bookId, b.sellerId, reason, desc) { showReport = false } },
                onDismiss = { showReport = false }
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.4f))
        Text(value, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.6f))
    }
}

@Composable
private fun RequestDialog(
    book: Book, myBooks: List<Book>,
    onSend: (offeredBookId: String?, message: String) -> Unit, onDismiss: () -> Unit
) {
    var offered by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request \"${book.title}\"") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (book.exchangeAllowed && myBooks.isNotEmpty()) {
                    Text("Offer one of your books in exchange (optional):", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = offered == null, onClick = { offered = null })
                        Text("No book — I want to buy it")
                    }
                    myBooks.forEach { mine ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = offered == mine.bookId, onClick = { offered = mine.bookId })
                            Text(mine.title, maxLines = 1)
                        }
                    }
                } else if (book.exchangeAllowed) {
                    Text("You have no available books to offer, so this will be a purchase request.", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    message, { message = it }, label = { Text("Message to seller") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSend(offered, message) }) { Text("Send request") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ReportDialog(onSubmit: (String, String) -> Unit, onDismiss: () -> Unit) {
    var reason by remember { mutableStateOf(Options.reportReasons.first()) }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report listing") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField("Reason", Options.reportReasons, reason, { reason = it }, Modifier.fillMaxWidth())
                OutlinedTextField(
                    description, { description = it }, label = { Text("Details (optional)") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSubmit(reason, description) }) { Text("Submit") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
