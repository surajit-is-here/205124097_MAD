package com.example.bookswap.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookswap.data.model.Book
import com.example.bookswap.data.model.BookStatus
import com.example.bookswap.data.model.User
import com.example.bookswap.data.model.imageList
import com.example.bookswap.data.repository.AppRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class BookFilters(
    val query: String = "",
    val category: String? = null,
    val condition: String? = null,
    val minPrice: String = "",
    val maxPrice: String = "",
    val department: String? = null,
    val semester: String? = null,
    val exchangeOnly: Boolean = false,
    val availableOnly: Boolean = true
)

fun BookFilters.activeCount(): Int = listOf(
    category != null, condition != null, minPrice.isNotBlank(), maxPrice.isNotBlank(),
    department != null, semester != null, exchangeOnly, !availableOnly
).count { it }

fun BookFilters.matches(book: Book): Boolean {
    val q = query.trim()
    if (q.isNotEmpty() &&
        !book.title.contains(q, true) && !book.author.contains(q, true) && !book.isbn.contains(q, true)
    ) return false
    if (category != null && book.category != category) return false
    if (condition != null && book.condition != condition) return false
    if (department != null && book.department != department) return false
    if (semester != null && book.semester != semester) return false
    if (exchangeOnly && !book.exchangeAllowed) return false
    if (availableOnly && book.status != BookStatus.AVAILABLE.name) return false
    minPrice.toDoubleOrNull()?.let { if (book.price < it) return false }
    maxPrice.toDoubleOrNull()?.let { if (book.price > it) return false }
    return true
}

@OptIn(ExperimentalCoroutinesApi::class)
class BookViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppRepository.get(app)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** null = still loading */
    val allBooks: StateFlow<List<Book>?> = repo.observeBooks()
        .map<List<Book>, List<Book>?> { it }
        .catch {
            _messages.tryEmit("Could not load books")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val users: StateFlow<Map<String, User>> = repo.observeUsers()
        .map { list -> list.associateBy { it.uid } }
        .catch { emit(emptyMap()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val favoriteIds: StateFlow<List<String>> = repo.currentUserId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.observeFavoriteIds(id) }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favoriteCounts: StateFlow<Map<String, Int>> = repo.observeFavoriteCounts()
        .map { list -> list.associate { it.bookId to it.total } }
        .catch { emit(emptyMap()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val filters = MutableStateFlow(BookFilters())

    val filteredBooks: StateFlow<List<Book>?> = combine(allBooks, filters) { books, f ->
        books?.filter { f.matches(it) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val myBooks: StateFlow<List<Book>> = combine(allBooks, repo.currentUserId) { books, uid ->
        books.orEmpty().filter { it.sellerId == uid }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun updateFilters(f: BookFilters) {
        filters.value = f
    }

    fun observeBook(id: String): Flow<Book?> = repo.observeBook(id)

    suspend fun getBook(id: String): Book? = repo.getBook(id)

    fun toggleFavorite(bookId: String) {
        val isFav = favoriteIds.value.contains(bookId)
        viewModelScope.launch {
            repo.toggleFavorite(bookId, isFav)
                .onFailure { _messages.tryEmit(it.message ?: "Could not update favorites") }
        }
    }

    // ------------------------------------------------------- Listing form

    var saving by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
        private set

    fun saveBook(
        existing: Book?, sellerCollege: String,
        title: String, author: String, isbn: String, category: String, description: String,
        condition: String, price: Double, exchangeAllowed: Boolean,
        department: String, semester: String,
        keptImages: List<String>, newImages: List<Uri>,
        onDone: () -> Unit
    ) {
        val sellerId = repo.currentUserId.value
        if (sellerId == null) {
            _messages.tryEmit("Please log in first")
            return
        }
        viewModelScope.launch {
            saving = true
            progress = 0f
            val copied = repo.copyImages(newImages) { progress = it }
            if (copied.isFailure) {
                _messages.tryEmit("Image upload failed: ${copied.exceptionOrNull()?.message}")
                saving = false
                return@launch
            }
            val allPaths = keptImages + copied.getOrThrow()
            val now = System.currentTimeMillis()
            val book = Book(
                bookId = existing?.bookId ?: repo.newBookId(),
                sellerId = existing?.sellerId ?: sellerId,
                title = title.trim(), author = author.trim(), isbn = isbn.trim(),
                category = category, description = description.trim(),
                condition = condition, price = price, exchangeAllowed = exchangeAllowed,
                imagePaths = allPaths.joinToString("|"),
                college = existing?.college ?: sellerCollege,
                department = department, semester = semester,
                status = existing?.status ?: BookStatus.AVAILABLE.name,
                createdAt = existing?.createdAt ?: now, updatedAt = now
            )
            repo.saveBook(book)
                .onSuccess {
                    // delete image files that were removed during editing
                    existing?.imageList()?.filter { it !in keptImages }?.forEach { File(it).delete() }
                    _messages.tryEmit(if (existing == null) "Book listed successfully" else "Listing updated")
                    onDone()
                }
                .onFailure { _messages.tryEmit("Could not save listing: ${it.message}") }
            saving = false
        }
    }

    fun deleteBook(book: Book, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repo.deleteBook(book)
                .onSuccess {
                    _messages.tryEmit("Listing deleted")
                    onDone()
                }
                .onFailure { _messages.tryEmit(it.message ?: "Could not delete listing") }
        }
    }

    fun setStatus(bookId: String, status: BookStatus) {
        viewModelScope.launch {
            repo.setBookStatus(bookId, status)
                .onSuccess { _messages.tryEmit("Marked as ${status.label.lowercase()}") }
                .onFailure { _messages.tryEmit(it.message ?: "Could not update listing") }
        }
    }
}
