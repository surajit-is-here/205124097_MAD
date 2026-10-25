package com.example.bookswap.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class BookStatus(val label: String) {
    AVAILABLE("Available"), RESERVED("Reserved"), SOLD("Sold"), EXCHANGED("Exchanged")
}

enum class BookCondition(val label: String) {
    NEW("New"), LIKE_NEW("Like new"), GOOD("Good"), FAIR("Fair"), POOR("Poor")
}

enum class RequestStatus(val label: String) {
    PENDING("Pending"), ACCEPTED("Accepted"), REJECTED("Rejected"),
    CANCELLED("Cancelled"), COMPLETED("Completed")
}

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class User(
    @PrimaryKey val uid: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val salt: String,
    val college: String,
    val department: String,
    val year: String,
    val profileImagePath: String = "",
    val createdAt: Long
)

@Entity(tableName = "books")
data class Book(
    @PrimaryKey val bookId: String,
    val sellerId: String,
    val title: String,
    val author: String,
    val isbn: String,
    val category: String,
    val description: String,
    val condition: String,
    val price: Double,
    val exchangeAllowed: Boolean,
    /** Absolute file paths of images, joined with "|" */
    val imagePaths: String,
    val college: String,
    val department: String,
    val semester: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "favorites", primaryKeys = ["userId", "bookId"])
data class Favorite(
    val userId: String,
    val bookId: String,
    val createdAt: Long
)

@Entity(tableName = "exchange_requests")
data class ExchangeRequest(
    @PrimaryKey val requestId: String,
    val bookId: String,
    val senderId: String,
    val receiverId: String,
    val offeredBookId: String?,
    val message: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val messageId: String,
    val conversationId: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val timestamp: Long,
    val isRead: Boolean
)

@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey val notificationId: String,
    val userId: String,
    /** REQUEST, MESSAGE or BOOK */
    val type: String,
    val title: String,
    val body: String,
    val relatedId: String,
    val isRead: Boolean,
    val createdAt: Long
)

@Entity(tableName = "reports")
data class Report(
    @PrimaryKey val reportId: String,
    val reporterId: String,
    val reportedUserId: String,
    val bookId: String,
    val reason: String,
    val description: String,
    val createdAt: Long,
    val status: String
)

/** Result row for the "popular books" query. */
data class FavoriteCount(val bookId: String, val total: Int)

/** UI-only summary of a chat. */
data class Conversation(
    val id: String,
    val otherUserId: String,
    val bookId: String,
    val lastMessage: String,
    val lastTime: Long,
    val unread: Int
)

fun Book.imageList(): List<String> =
    if (imagePaths.isBlank()) emptyList() else imagePaths.split("|").filter { it.isNotBlank() }

fun Book.statusEnum(): BookStatus =
    runCatching { BookStatus.valueOf(status) }.getOrDefault(BookStatus.AVAILABLE)

fun Book.conditionEnum(): BookCondition =
    runCatching { BookCondition.valueOf(condition) }.getOrDefault(BookCondition.GOOD)

fun ExchangeRequest.statusEnum(): RequestStatus =
    runCatching { RequestStatus.valueOf(status) }.getOrDefault(RequestStatus.PENDING)
