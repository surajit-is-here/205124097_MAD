package com.example.bookswap.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.example.bookswap.data.model.Book
import com.example.bookswap.data.model.BookStatus
import com.example.bookswap.data.model.imageList
import com.example.bookswap.data.model.statusEnum
import com.example.bookswap.ui.components.BookCard
import com.example.bookswap.ui.components.BookImage
import com.example.bookswap.ui.components.ConfirmDialog
import com.example.bookswap.ui.components.EmptyState
import com.example.bookswap.ui.components.LoadingBox
import com.example.bookswap.ui.components.StatusPill
import com.example.bookswap.utils.formatPrice
import com.example.bookswap.viewmodel.BookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(bookVm: BookViewModel, onBook: (String) -> Unit, onBack: () -> Unit) {
    val books by bookVm.allBooks.collectAsStateWithLifecycle()
    val favoriteIds by bookVm.favoriteIds.collectAsStateWithLifecycle()
    val users by bookVm.users.collectAsStateWithLifecycle()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Favorites") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
        )
    }) { padding ->
        val all = books
        if (all == null) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        val byId = all.associateBy { it.bookId }
        val saved = favoriteIds.mapNotNull { byId[it] }
        val missing = favoriteIds.size - saved.size

        if (saved.isEmpty()) {
            EmptyState(Icons.Filled.Favorite, "No favorites yet", "Tap the heart on any book to save it here.", Modifier.padding(padding))
        } else {
            LazyColumn(
                Modifier.padding(padding), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (missing > 0) {
                    item {
                        Text(
                            "$missing saved book${if (missing == 1) " is" else "s are"} no longer available.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(saved, key = { it.bookId }) { book ->
                    BookCard(
                        book, users[book.sellerId]?.name ?: "Student", true,
                        onClick = { onBook(book.bookId) }, onFavorite = { bookVm.toggleFavorite(book.bookId) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListingsScreen(
    bookVm: BookViewModel,
    onBook: (String) -> Unit,
    onEdit: (String) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit
) {
    val mine by bookVm.myBooks.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(0) }
    var toDelete by remember { mutableStateOf<Book?>(null) }
    val tabs = listOf("Active", "Sold", "Exchanged")
    val shown = when (tab) {
        0 -> mine.filter { it.statusEnum() == BookStatus.AVAILABLE || it.statusEnum() == BookStatus.RESERVED }
        1 -> mine.filter { it.statusEnum() == BookStatus.SOLD }
        else -> mine.filter { it.statusEnum() == BookStatus.EXCHANGED }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My listings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAdd, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("List a book") })
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
            }
            if (shown.isEmpty()) {
                EmptyState(Icons.Filled.Book, "Nothing here", "Books you mark as ${tabs[tab].lowercase()} will appear in this tab.")
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(shown, key = { it.bookId }) { book ->
                        ListingRow(
                            book, onClick = { onBook(book.bookId) }, onEdit = { onEdit(book.bookId) },
                            onSold = { bookVm.setStatus(book.bookId, BookStatus.SOLD) },
                            onExchanged = { bookVm.setStatus(book.bookId, BookStatus.EXCHANGED) },
                            onRelist = { bookVm.setStatus(book.bookId, BookStatus.AVAILABLE) },
                            onDelete = { toDelete = book }
                        )
                    }
                }
            }
        }
    }

    toDelete?.let { book ->
        ConfirmDialog(
            "Delete listing?", "This permanently removes \"${book.title}\".", "Delete",
            onConfirm = { bookVm.deleteBook(book) }, onDismiss = { toDelete = null }
        )
    }
}

@Composable
private fun ListingRow(
    book: Book, onClick: () -> Unit, onEdit: () -> Unit, onSold: () -> Unit,
    onExchanged: () -> Unit, onRelist: () -> Unit, onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val open = book.statusEnum() == BookStatus.AVAILABLE || book.statusEnum() == BookStatus.RESERVED
    Card(
        onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BookImage(book.imageList().firstOrNull(), Modifier.size(88.dp))
            Column(Modifier.weight(1f).padding(12.dp)) {
                Text(book.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(formatPrice(book.price), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                StatusPill(book.statusEnum())
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Actions") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onEdit() })
                    if (open) {
                        DropdownMenuItem(text = { Text("Mark as sold") }, onClick = { menu = false; onSold() })
                        DropdownMenuItem(text = { Text("Mark as exchanged") }, onClick = { menu = false; onExchanged() })
                    } else {
                        DropdownMenuItem(text = { Text("Relist as available") }, onClick = { menu = false; onRelist() })
                    }
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}
