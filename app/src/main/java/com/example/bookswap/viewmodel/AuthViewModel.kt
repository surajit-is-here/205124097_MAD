package com.example.bookswap.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookswap.data.model.User
import com.example.bookswap.data.repository.AppRepository
import com.example.bookswap.utils.Validators
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppRepository.get(app)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val currentUserId: StateFlow<String?> = repo.currentUserId
    val themeMode: StateFlow<String> = repo.themeMode

    val currentUser: StateFlow<User?> = repo.currentUserId
        .flatMapLatest { id -> if (id == null) flowOf(null) else repo.observeUser(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun clearError() {
        error = null
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        when {
            email.isBlank() || password.isBlank() -> error = "Please fill in all fields"
            !Validators.isValidEmail(email) -> error = "Enter a valid email address"
            else -> viewModelScope.launch {
                loading = true
                error = null
                repo.login(email, password)
                    .onSuccess { onSuccess() }
                    .onFailure { error = it.message ?: "Login failed" }
                loading = false
            }
        }
    }

    fun register(
        name: String, email: String, password: String, confirm: String,
        college: String, department: String, year: String, onSuccess: () -> Unit
    ) {
        when {
            name.isBlank() || email.isBlank() || password.isBlank() || confirm.isBlank() ||
                college.isBlank() || department.isBlank() || year.isBlank() ->
                error = "Please fill in all fields"
            !Validators.isValidEmail(email) -> error = "Enter a valid email address"
            !Validators.isValidPassword(password) ->
                error = "Password must be at least ${Validators.MIN_PASSWORD_LENGTH} characters"
            password != confirm -> error = "Passwords do not match"
            else -> viewModelScope.launch {
                loading = true
                error = null
                repo.register(name, email, password, college, department, year)
                    .onSuccess { onSuccess() }
                    .onFailure { error = it.message ?: "Registration failed" }
                loading = false
            }
        }
    }

    fun resetPassword(email: String, newPassword: String, confirm: String, onSuccess: () -> Unit) {
        when {
            email.isBlank() || newPassword.isBlank() || confirm.isBlank() -> error = "Please fill in all fields"
            !Validators.isValidEmail(email) -> error = "Enter a valid email address"
            !Validators.isValidPassword(newPassword) ->
                error = "Password must be at least ${Validators.MIN_PASSWORD_LENGTH} characters"
            newPassword != confirm -> error = "Passwords do not match"
            else -> viewModelScope.launch {
                loading = true
                error = null
                repo.resetPassword(email, newPassword)
                    .onSuccess {
                        _messages.tryEmit("Password updated. Please log in.")
                        onSuccess()
                    }
                    .onFailure { error = it.message ?: "Could not reset password" }
                loading = false
            }
        }
    }

    fun logout() = repo.logout()

    fun setTheme(mode: String) = repo.setThemeMode(mode)

    fun updateProfile(
        name: String, college: String, department: String, year: String,
        newImage: Uri?, onSuccess: () -> Unit
    ) {
        if (name.isBlank() || college.isBlank()) {
            error = "Name and college cannot be empty"
            return
        }
        viewModelScope.launch {
            loading = true
            error = null
            repo.updateProfile(name, college, department, year, newImage)
                .onSuccess {
                    _messages.tryEmit("Profile updated")
                    onSuccess()
                }
                .onFailure { error = it.message ?: "Could not update profile" }
            loading = false
        }
    }
}
