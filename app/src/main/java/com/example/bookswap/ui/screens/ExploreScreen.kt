package com.example.bookswap.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bookswap.data.model.BookCondition
import com.example.bookswap.ui.components.BookCard
import com.example.bookswap.ui.components.DropdownField
import com.example.bookswap.ui.components.EmptyState
import com.example.bookswap.ui.components.LoadingBox
import com.example.bookswap.utils.Options
import com.example.bookswap.viewmodel.BookFilters
import com.example.bookswap.viewmodel.BookViewModel
import com.example.bookswap.viewmodel.activeCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(bookVm: BookViewModel, onBook: (String) -> Unit) {
    val books by bookVm.filteredBooks.collectAsStateWithLifecycle()
    val filters by bookVm.filters.collectAsStateWithLifecycle()
    val users by bookVm.users.collectAsStateWithLifecycle()
    val favorites by bookVm.favoriteIds.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = filters.query,
                    onValueChange = { bookVm.updateFilters(filters.copy(query = it)) },
                    placeholder = { Text("Title, author or ISBN") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = {
                        if (filters.query.isNotEmpty()) {
                            IconButton(onClick = { bookVm.updateFilters(filters.copy(query = "")) }) {
                                Icon(Icons.Filled.Close, "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showFilters = true }) {
                    BadgedBox(badge = { if (filters.activeCount() > 0) Badge { Text(filters.activeCount().toString()) } }) {
                        Icon(Icons.Filled.FilterList, "Filters")
                    }
                }
            }
        }
    ) { padding ->
        val list = books
        when {
            list == null -> LoadingBox(Modifier.padding(padding))
            list.isEmpty() -> EmptyState(
                Icons.Filled.Search, "No books found",
                "Try a different search or clear some filters.", Modifier.padding(padding)
            )
            else -> LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("${list.size} book${if (list.size == 1) "" else "s"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(list, key = { it.bookId }) { book ->
                    BookCard(
                        book = book,
                        sellerName = users[book.sellerId]?.name ?: "Student",
                        isFavorite = favorites.contains(book.bookId),
                        onClick = { onBook(book.bookId) },
                        onFavorite = { bookVm.toggleFavorite(book.bookId) }
                    )
                }
            }
        }
    }

    if (showFilters) {
        ModalBottomSheet(onDismissRequest = { showFilters = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            FilterSheet(
                initial = filters,
                onApply = { bookVm.updateFilters(it); showFilters = false },
                onReset = { bookVm.updateFilters(BookFilters(query = filters.query)); showFilters = false }
            )
        }
    }
}

@Composable
private fun FilterSheet(initial: BookFilters, onApply: (BookFilters) -> Unit, onReset: () -> Unit) {
    var f by remember { mutableStateOf(initial) }
    val any = Options.ANY
    val conditionOptions = listOf(any) + BookCondition.values().map { it.label }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Filters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        DropdownField("Category", listOf(any) + Options.categories, f.category ?: any, { f = f.copy(category = if (it == any) null else it) }, Modifier.fillMaxWidth())
        DropdownField(
            "Condition", conditionOptions,
            f.condition?.let { c -> BookCondition.valueOf(c).label } ?: any,
            { label -> f = f.copy(condition = BookCondition.values().firstOrNull { it.label == label }?.name) },
            Modifier.fillMaxWidth()
        )
        DropdownField("Department", listOf(any) + Options.departments, f.department ?: any, { f = f.copy(department = if (it == any) null else it) }, Modifier.fillMaxWidth())
        DropdownField("Semester", listOf(any) + Options.semesters, f.semester ?: any, { f = f.copy(semester = if (it == any) null else it) }, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                f.minPrice, { f = f.copy(minPrice = it.filter { c -> c.isDigit() }) }, label = { Text("Min ₹") },
                singleLine = true, modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                f.maxPrice, { f = f.copy(maxPrice = it.filter { c -> c.isDigit() }) }, label = { Text("Max ₹") },
                singleLine = true, modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Exchange available only", Modifier.weight(1f))
            Switch(f.exchangeOnly, { f = f.copy(exchangeOnly = it) })
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Available books only", Modifier.weight(1f))
            Switch(f.availableOnly, { f = f.copy(availableOnly = it) })
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("Reset") }
            Button(onClick = { onApply(f) }, modifier = Modifier.weight(1f)) { Text("Apply") }
        }
    }
}
