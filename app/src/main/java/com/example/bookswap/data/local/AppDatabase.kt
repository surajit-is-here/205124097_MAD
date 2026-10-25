package com.example.bookswap.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.bookswap.data.model.AppNotification
import com.example.bookswap.data.model.Book
import com.example.bookswap.data.model.ExchangeRequest
import com.example.bookswap.data.model.Favorite
import com.example.bookswap.data.model.Message
import com.example.bookswap.data.model.Report
import com.example.bookswap.data.model.User

@Database(
    entities = [
        User::class, Book::class, Favorite::class, ExchangeRequest::class,
        Message::class, AppNotification::class, Report::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bookswap.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
