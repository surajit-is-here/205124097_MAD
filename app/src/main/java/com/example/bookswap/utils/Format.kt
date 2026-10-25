package com.example.bookswap.utils

import com.example.bookswap.data.model.Book
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatPrice(price: Double): String =
    if (price <= 0.0) "Free" else "₹" + String.format(Locale.US, "%.0f", price)

fun formatDate(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))

fun formatTime(millis: Long): String =
    SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(millis))

/** Conversation id = both user ids (sorted) + book id, joined with "_". UUIDs contain no "_". */
fun conversationId(userA: String, userB: String, bookId: String): String =
    listOf(userA, userB).sorted().joinToString("_") + "_" + bookId

fun Book.shortInfo(): String = "$title · $author"
