package com.example.bookswap.data.repository

import android.content.Context
import android.net.Uri
import com.example.bookswap.data.local.AppDatabase
import com.example.bookswap.data.model.AppNotification
import com.example.bookswap.data.model.Book
import com.example.bookswap.data.model.BookStatus
import com.example.bookswap.data.model.ExchangeRequest
import com.example.bookswap.data.model.Favorite
import com.example.bookswap.data.model.FavoriteCount
import com.example.bookswap.data.model.Message
import com.example.bookswap.data.model.Report
import com.example.bookswap.data.model.RequestStatus
import com.example.bookswap.data.model.User
import com.example.bookswap.data.model.statusEnum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

/**
 * Single source of truth. All data is stored locally on the device:
 *  - Room (SQLite) for structured data
 *  - the app's private files directory for images
 *  - SharedPreferences for the logged-in session and theme choice
 */
class AppRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val dao = AppDatabase.get(appContext).dao()
    private val prefs = appContext.getSharedPreferences("bookswap_prefs", Context.MODE_PRIVATE)

    private val _currentUserId = MutableStateFlow(prefs.getString(KEY_UID, null))
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME, "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private fun now() = System.currentTimeMillis()
    private fun newId() = UUID.randomUUID().toString()
    private fun requireUid(): String = _currentUserId.value ?: throw IllegalStateException("Please log in first")

    // ------------------------------------------------------------------ Auth

    private fun hash(password: String, salt: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest((salt + password).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun register(
        name: String, email: String, password: String,
        college: String, department: String, year: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanEmail = email.trim().lowercase()
            if (dao.findUserByEmail(cleanEmail) != null) {
                throw IllegalArgumentException("An account with this email already exists")
            }
            val salt = newSalt()
            val user = User(
                uid = newId(), name = name.trim(), email = cleanEmail,
                passwordHash = hash(password, salt), salt = salt,
                college = college.trim(), department = department, year = year,
                createdAt = now()
            )
            dao.insertUser(user)
            setSession(user.uid)
        }
    }

    suspend fun login(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val user = dao.findUserByEmail(email.trim().lowercase())
                ?: throw IllegalArgumentException("No account found for this email")
            if (user.passwordHash != hash(password, user.salt)) {
                throw IllegalArgumentException("Incorrect password")
            }
            setSession(user.uid)
        }
    }

    /** Local-only "forgot password": there is no email server, so the account email is the proof. */
    suspend fun resetPassword(email: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val user = dao.findUserByEmail(email.trim().lowercase())
                ?: throw IllegalArgumentException("No account found for this email")
            val salt = newSalt()
            dao.updateUser(user.copy(salt = salt, passwordHash = hash(newPassword, salt)))
        }
    }

    fun logout() {
        prefs.edit().remove(KEY_UID).apply()
        _currentUserId.value = null
    }

    private fun setSession(uid: String) {
        prefs.edit().putString(KEY_UID, uid).apply()
        _currentUserId.value = uid
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME, mode).apply()
        _themeMode.value = mode
    }

    fun observeUser(uid: String): Flow<User?> = dao.observeUser(uid)
    fun observeUsers(): Flow<List<User>> = dao.observeUsers()

    suspend fun updateProfile(
        name: String, college: String, department: String, year: String, newImage: Uri?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val user = dao.findUser(requireUid()) ?: throw IllegalStateException("User not found")
            var imagePath = user.profileImagePath
            if (newImage != null) {
                imagePath = copyImagesInternal(listOf(newImage)) { }.first()
            }
            dao.updateUser(
                user.copy(
                    name = name.trim(), college = college.trim(),
                    department = department, year = year, profileImagePath = imagePath
                )
            )
        }
    }

    // ----------------------------------------------------------------- Books

    fun observeBooks(): Flow<List<Book>> = dao.observeBooks()
    fun observeBook(id: String): Flow<Book?> = dao.observeBook(id)
    suspend fun getBook(id: String): Book? = withContext(Dispatchers.IO) { dao.findBook(id) }

    private fun copyImagesInternal(uris: List<Uri>, onProgress: (Float) -> Unit): List<String> {
        val dir = File(appContext.filesDir, "images").apply { mkdirs() }
        return uris.mapIndexed { index, uri ->
            val out = File(dir, "${UUID.randomUUID()}.jpg")
            val input = appContext.contentResolver.openInputStream(uri)
                ?: throw IOException("Could not read the selected image")
            input.use { stream -> out.outputStream().use { stream.copyTo(it) } }
            onProgress((index + 1f) / uris.size)
            out.absolutePath
        }
    }

    suspend fun copyImages(uris: List<Uri>, onProgress: (Float) -> Unit): Result<List<String>> =
        withContext(Dispatchers.IO) { runCatching { copyImagesInternal(uris, onProgress) } }

    suspend fun saveBook(book: Book): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { dao.upsertBook(book) }
    }

    fun newBookId(): String = newId()

    suspend fun deleteBook(book: Book): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (book.sellerId != requireUid()) throw SecurityException("You can only delete your own listings")
            dao.deleteBook(book.bookId)
            dao.removeFavoritesForBook(book.bookId)
            book.imagePaths.split("|").filter { it.isNotBlank() }.forEach { File(it).delete() }
        }
    }

    suspend fun setBookStatus(bookId: String, status: BookStatus): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val book = dao.findBook(bookId) ?: throw IllegalStateException("Book not found")
            if (book.sellerId != requireUid()) throw SecurityException("You can only change your own listings")
            dao.setBookStatus(bookId, status.name, now())
            if (status == BookStatus.SOLD || status == BookStatus.EXCHANGED) {
                dao.openRequestsForBook(bookId).forEach { r ->
                    notify(
                        r.senderId, "BOOK", "Book no longer available",
                        "\"${book.title}\" was marked as ${status.label.lowercase()}.", bookId
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------- Favorites

    fun observeFavoriteIds(userId: String): Flow<List<String>> = dao.observeFavoriteIds(userId)
    fun observeFavoriteCounts(): Flow<List<FavoriteCount>> = dao.observeFavoriteCounts()

    suspend fun toggleFavorite(bookId: String, currentlyFavorite: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uid = requireUid()
                if (currentlyFavorite) dao.removeFavorite(uid, bookId)
                else dao.addFavorite(Favorite(uid, bookId, now()))
            }
        }

    // ------------------------------------------------------ Exchange requests

    fun observeRequests(userId: String): Flow<List<ExchangeRequest>> = dao.observeRequestsFor(userId)
    fun observeRequest(id: String): Flow<ExchangeRequest?> = dao.observeRequest(id)

    suspend fun sendRequest(book: Book, offeredBookId: String?, message: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uid = requireUid()
                val fresh = dao.findBook(book.bookId) ?: throw IllegalStateException("This listing no longer exists")
                if (fresh.sellerId == uid) throw IllegalArgumentException("You cannot request your own book")
                if (fresh.statusEnum() != BookStatus.AVAILABLE) throw IllegalStateException("This book is no longer available")
                if (offeredBookId != null) {
                    val offered = dao.findBook(offeredBookId) ?: throw IllegalArgumentException("Offered book not found")
                    if (offered.sellerId != uid) throw IllegalArgumentException("You can only offer your own book")
                }
                val request = ExchangeRequest(
                    requestId = newId(), bookId = fresh.bookId, senderId = uid,
                    receiverId = fresh.sellerId, offeredBookId = offeredBookId,
                    message = message.trim(), status = RequestStatus.PENDING.name,
                    createdAt = now(), updatedAt = now()
                )
                dao.insertRequest(request)
                val senderName = dao.findUser(uid)?.name ?: "A student"
                notify(
                    fresh.sellerId, "REQUEST", "New exchange request",
                    "$senderName is interested in \"${fresh.title}\".", request.requestId
                )
            }
        }

    suspend fun updateRequestStatus(requestId: String, newStatus: RequestStatus): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val me = requireUid()
                val r = dao.findRequest(requestId) ?: throw IllegalStateException("Request not found")
                val current = r.status
                val isParticipant = me == r.senderId || me == r.receiverId
                when (newStatus) {
                    RequestStatus.ACCEPTED, RequestStatus.REJECTED ->
                        require(me == r.receiverId && current == "PENDING") { "Only the book owner can respond to a pending request" }
                    RequestStatus.CANCELLED ->
                        require((me == r.senderId && current == "PENDING") || (isParticipant && current == "ACCEPTED")) { "This request cannot be cancelled" }
                    RequestStatus.COMPLETED ->
                        require(isParticipant && current == "ACCEPTED") { "Only an accepted request can be completed" }
                    else -> throw IllegalArgumentException("Invalid status")
                }
                dao.updateRequest(r.copy(status = newStatus.name, updatedAt = now()))

                val other = if (me == r.senderId) r.receiverId else r.senderId
                val book = dao.findBook(r.bookId)
                val title = book?.title ?: "a book"
                when (newStatus) {
                    RequestStatus.ACCEPTED -> {
                        dao.setBookStatus(r.bookId, BookStatus.RESERVED.name, now())
                        notify(other, "REQUEST", "Request accepted", "Your request for \"$title\" was accepted.", r.requestId)
                    }
                    RequestStatus.REJECTED ->
                        notify(other, "REQUEST", "Request rejected", "Your request for \"$title\" was rejected.", r.requestId)
                    RequestStatus.CANCELLED -> {
                        if (current == "ACCEPTED") dao.setBookStatus(r.bookId, BookStatus.AVAILABLE.name, now())
                        notify(other, "REQUEST", "Request cancelled", "The request for \"$title\" was cancelled.", r.requestId)
                    }
                    RequestStatus.COMPLETED -> {
                        val finalStatus = if (r.offeredBookId != null) BookStatus.EXCHANGED else BookStatus.SOLD
                        dao.setBookStatus(r.bookId, finalStatus.name, now())
                        r.offeredBookId?.let { dao.setBookStatus(it, BookStatus.EXCHANGED.name, now()) }
                        notify(other, "BOOK", "Deal completed", "\"$title\" was marked as ${finalStatus.label.lowercase()}.", r.requestId)
                    }
                    else -> Unit
                }
            }
        }

    // ------------------------------------------------------------------ Chat

    fun observeMessages(conversationId: String): Flow<List<Message>> = dao.observeMessages(conversationId)
    fun observeMessagesFor(userId: String): Flow<List<Message>> = dao.observeMessagesFor(userId)

    suspend fun sendMessage(conversationId: String, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val me = requireUid()
            val parts = conversationId.split("_")
            if (parts.size != 3) throw IllegalArgumentException("Invalid conversation")
            val other = if (parts[0] == me) parts[1] else parts[0]
            dao.insertMessage(
                Message(newId(), conversationId, me, other, text.trim(), now(), false)
            )
            val myName = dao.findUser(me)?.name ?: "A student"
            notify(other, "MESSAGE", "New message", "$myName: ${text.trim().take(60)}", conversationId)
        }
    }

    suspend fun markMessagesRead(conversationId: String) = withContext(Dispatchers.IO) {
        runCatching { dao.markMessagesRead(conversationId, requireUid()) }
        Unit
    }

    // --------------------------------------------------------- Notifications

    fun observeNotifications(userId: String): Flow<List<AppNotification>> = dao.observeNotifications(userId)
    fun observeUnreadCount(userId: String): Flow<Int> = dao.observeUnreadCount(userId)

    suspend fun markAllNotificationsRead() = withContext(Dispatchers.IO) {
        runCatching { dao.markAllNotificationsRead(requireUid()) }
        Unit
    }

    private suspend fun notify(userId: String, type: String, title: String, body: String, relatedId: String) {
        dao.insertNotification(
            AppNotification(newId(), userId, type, title, body, relatedId, false, now())
        )
    }

    // --------------------------------------------------------------- Reports

    suspend fun report(bookId: String, reportedUserId: String, reason: String, description: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                dao.insertReport(
                    Report(newId(), requireUid(), reportedUserId, bookId, reason, description.trim(), now(), "OPEN")
                )
            }
        }

    companion object {
        private const val KEY_UID = "uid"
        private const val KEY_THEME = "theme"

        @Volatile
        private var instance: AppRepository? = null

        fun get(context: Context): AppRepository =
            instance ?: synchronized(this) {
                instance ?: AppRepository(context).also { instance = it }
            }
    }
}
