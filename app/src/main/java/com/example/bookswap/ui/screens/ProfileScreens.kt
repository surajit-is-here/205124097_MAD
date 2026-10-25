package com.example.bookswap.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.bookswap.data.model.User
import com.example.bookswap.ui.components.ConfirmDialog
import com.example.bookswap.ui.components.DropdownField
import com.example.bookswap.ui.components.LoadingBox
import com.example.bookswap.utils.Options
import com.example.bookswap.viewmodel.AuthViewModel
import java.io.File

@Composable
private fun Avatar(user: User, size: Int, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (user.profileImagePath.isNotBlank()) {
            AsyncImage(File(user.profileImagePath), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(
                user.name.trim().firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit, tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint)
        Text(label, Modifier.weight(1f).padding(start = 16.dp), color = tint)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ProfileScreen(
    authVm: AuthViewModel,
    onMyListings: () -> Unit,
    onFavorites: () -> Unit,
    onMyRequests: () -> Unit,
    onEditProfile: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val user by authVm.currentUser.collectAsStateWithLifecycle()
    var confirmLogout by remember { mutableStateOf(false) }

    Scaffold { padding ->
        val u = user
        if (u == null) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(u, 96)
                Spacer(Modifier.height(12.dp))
                Text(u.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(u.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("${u.college} · ${u.department}", style = MaterialTheme.typography.bodyMedium)
                Text(u.year, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
            MenuRow(Icons.Filled.Book, "My listings", onMyListings)
            MenuRow(Icons.Filled.Favorite, "Favorites", onFavorites)
            MenuRow(Icons.Filled.SwapHoriz, "My requests", onMyRequests)
            MenuRow(Icons.Filled.Edit, "Edit profile", onEditProfile)
            MenuRow(Icons.Filled.Settings, "Settings", onSettings)
            HorizontalDivider()
            MenuRow(Icons.AutoMirrored.Filled.Logout, "Log out", { confirmLogout = true }, MaterialTheme.colorScheme.error)
        }
        if (confirmLogout) {
            ConfirmDialog("Log out?", "You can log back in any time with your email and password.", "Log out",
                onConfirm = onLogout, onDismiss = { confirmLogout = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(authVm: AuthViewModel, onBack: () -> Unit) {
    val user by authVm.currentUser.collectAsStateWithLifecycle()
    val u = user
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Edit profile") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
        )
    }) { padding ->
        if (u == null) LoadingBox(Modifier.padding(padding))
        else EditProfileForm(u, authVm, onBack, Modifier.padding(padding))
    }
}

@Composable
private fun EditProfileForm(user: User, authVm: AuthViewModel, onDone: () -> Unit, modifier: Modifier) {
    var name by remember { mutableStateOf(user.name) }
    var college by remember { mutableStateOf(user.college) }
    var department by remember { mutableStateOf(user.department) }
    var year by remember { mutableStateOf(user.year) }
    var newImage by remember { mutableStateOf<android.net.Uri?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) newImage = uri }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
            if (newImage != null) {
                AsyncImage(newImage, null, Modifier.size(96.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            } else {
                Avatar(user, 96)
            }
        }
        TextButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Change photo") }
        OutlinedTextField(name, { name = it; authVm.clearError() }, label = { Text("Full name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(user.email, {}, label = { Text("Email (cannot be changed)") }, enabled = false, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(college, { college = it; authVm.clearError() }, label = { Text("College") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        DropdownField("Department", Options.departments, department, { department = it }, Modifier.fillMaxWidth())
        DropdownField("Year of study", Options.years, year, { year = it }, Modifier.fillMaxWidth())
        authVm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = { authVm.updateProfile(name, college, department, year, newImage, onDone) },
            enabled = !authVm.loading, modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (authVm.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else Text("Save changes")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(authVm: AuthViewModel, onBack: () -> Unit) {
    val mode by authVm.themeMode.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
        )
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            listOf("system" to "Follow system", "light" to "Light", "dark" to "Dark").forEach { (value, label) ->
                Row(
                    Modifier.fillMaxWidth().clickable { authVm.setTheme(value) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = mode == value, onClick = { authVm.setTheme(value) })
                    Text(label)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "BookSwap 1.0 — all data is stored on this device only.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
