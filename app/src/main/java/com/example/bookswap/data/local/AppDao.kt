package com.example.bookswap.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.bookswap.data.model.AppNotification
import com.example.bookswap.data.model.Book
import com.example.bookswap.data.model.ExchangeRequest
import com.example.bookswap.data.model.Favorite
import com.example.bookswap.data.model.FavoriteCount
import com.example.bookswap.data.model.Message
import com.example.bookswap.data.model.Report
import com.example.bookswap.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // ---- Users ----
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    suspend fun findUser(uid: String): User?

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    fun observeUser(uid: String): Flow<User?>

    @Query("SELECT * FROM users")
    fun observeUsers(): Flow<List<User>>

    // ---- Books ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBook(book: Book)

    @Query("SELECT * FROM books ORDER BY createdAt DESC")
    fun observeBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE bookId = :id LIMIT 1")
    fun observeBook(id: String): Flow<Book?>

    @Query("SELECT * FROM books WHERE bookId = :id LIMIT 1")
    suspend fun findBook(id: String): Book?

    @Query("DELETE FROM books WHERE bookId = :id")
    suspend fun deleteBook(id: String)

    @Query("UPDATE books SET status = :status, updatedAt = :time WHERE bookId = :id")
    suspend fun setBookStatus(id: String, status: String, time: Long)

    // ---- Favorites ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE userId = :userId AND bookId = :bookId")
    suspend fun removeFavorite(userId: String, bookId: String)

    @Query("DELETE FROM favorites WHERE bookId = :bookId")
    suspend fun removeFavoritesForBook(bookId: String)

    @Query("SELECT bookId FROM favorites WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeFavoriteIds(userId: String): Flow<List<String>>

    @Query("SELECT bookId, COUNT(*) AS total FROM favorites GROUP BY bookId")
    fun observeFavoriteCounts(): Flow<List<FavoriteCount>>

    // ---- Exchange requests ----
    @Insert
    suspend fun insertRequest(request: ExchangeRequest)

    @Update
    suspend fun updateRequest(request: ExchangeRequest)

    @Query("SELECT * FROM exchange_requests WHERE senderId = :userId OR receiverId = :userId ORDER BY updatedAt DESC")
    fun observeRequestsFor(userId: String): Flow<List<ExchangeRequest>>

    @Query("SELECT * FROM exchange_requests WHERE requestId = :id LIMIT 1")
    fun observeRequest(id: String): Flow<ExchangeRequest?>

    @Query("SELECT * FROM exchange_requests WHERE requestId = :id LIMIT 1")
    suspend fun findRequest(id: String): ExchangeRequest?

    @Query("SELECT * FROM exchange_requests WHERE bookId = :bookId AND status IN ('PENDING','ACCEPTED')")
    suspend fun openRequestsForBook(bookId: String): List<ExchangeRequest>

    // ---- Messages ----
    @Insert
    suspend fun insertMessage(message: Message)

    @Query("SELECT * FROM messages WHERE conversationId = :cid ORDER BY timestamp ASC")
    fun observeMessages(cid: String): Flow<List<Message>>

    @Query("UPDATE messages SET isRead = 1 WHERE conversationId = :cid AND receiverId = :userId AND isRead = 0")
    suspend fun markMessagesRead(cid: String, userId: String)

    @Query("SELECT * FROM messages WHERE senderId = :userId OR receiverId = :userId ORDER BY timestamp DESC")
    fun observeMessagesFor(userId: String): Flow<List<Message>>

    // ---- Notifications ----
    @Insert
    suspend fun insertNotification(notification: AppNotification)

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeNotifications(userId: String): Flow<List<AppNotification>>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    fun observeUnreadCount(userId: String): Flow<Int>

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId AND isRead = 0")
    suspend fun markAllNotificationsRead(userId: String)

    // ---- Reports ----
    @Insert
    suspend fun insertReport(report: Report)
}
