package com.example.bookswap.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bookswap.data.model.Book
import com.example.bookswap.data.model.BookStatus
import com.example.bookswap.ui.components.BookCardCompact
import com.example.bookswap.ui.components.EmptyState
import com.example.bookswap.ui.components.LoadingBox
import com.example.bookswap.ui.components.SectionTitle
import com.example.bookswap.utils.Options
import com.example.bookswap.viewmodel.AuthViewModel
import com.example.bookswap.viewmodel.BookViewModel
import com.example.bookswap.viewmodel.SocialViewModel

@Composable
fun HomeScreen(
    bookVm: BookViewModel,
    authVm: AuthViewModel,
    socialVm: SocialViewModel,
    onBook: (String) -> Unit,
    onSearch: () -> Unit,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
    onCategory: (String) -> Unit
) {
    val books by bookVm.allBooks.collectAsStateWithLifecycle()
    val user by authVm.currentUser.collectAsStateWithLifecycle()
    val counts by bookVm.favoriteCounts.collectAsStateWithLifecycle()
    val unread by socialVm.unreadCount.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Book, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Text(
                        "  BookSwap", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onNotifications) {
                        BadgedBox(badge = { if (unread > 0) Badge { Text(unread.toString()) } }) {
                            Icon(Icons.Filled.Notifications, "Notifications")
                        }
                    }
                    IconButton(onClick = onProfile) { Icon(Icons.Filled.Person, "Profile") }
                }
                Spacer(Modifier.height(8.dp))
                Surface(
                    onClick = onSearch, shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "  Search title, author or ISBN", color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { padding ->
        val list = books
        when {
            list == null -> LoadingBox(Modifier.padding(padding))
            list.none { it.status == BookStatus.AVAILABLE.name } -> EmptyState(
                Icons.Filled.Book, "No books yet",
                "Be the first to list a book! Tap Sell below to get started.", Modifier.padding(padding)
            )
            else -> {
                val available = list.filter { it.status == BookStatus.AVAILABLE.name }
                val recent = available.take(10)
                val popular = available.filter { (counts[it.bookId] ?: 0) > 0 }
                    .sortedByDescending { counts[it.bookId] ?: 0 }.take(10)
                val nearby = available.filter {
                    user != null && it.college.equals(user?.college, ignoreCase = true) && it.sellerId != user?.uid
                }.take(10)

                LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
                    item {
                        SectionTitle("Categories")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(Options.categories) { c ->
                                AssistChip(onClick = { onCategory(c) }, label = { Text(c) })
                            }
                        }
                    }
                    item { BookRow("Recently listed", recent, onBook) }
                    if (popular.isNotEmpty()) item { BookRow("Popular books", popular, onBook) }
                    if (nearby.isNotEmpty()) item { BookRow("From your college", nearby, onBook) }
                }
            }
        }
    }
}

@Composable
private fun BookRow(title: String, books: List<Book>, onBook: (String) -> Unit) {
    Column {
        SectionTitle(title, Modifier.padding(top = 8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(books, key = { it.bookId }) { book -> BookCardCompact(book, onClick = { onBook(book.bookId) }) }
        }
    }
}
