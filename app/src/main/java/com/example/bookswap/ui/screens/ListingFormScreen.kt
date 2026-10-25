package com.example.bookswap.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.bookswap.data.model.Book
import com.example.bookswap.data.model.BookCondition
import com.example.bookswap.data.model.imageList
import com.example.bookswap.ui.components.BookImage
import com.example.bookswap.ui.components.DropdownField
import com.example.bookswap.ui.components.LoadingBox
import com.example.bookswap.utils.Options
import com.example.bookswap.utils.Validators
import com.example.bookswap.viewmodel.AuthViewModel
import com.example.bookswap.viewmodel.BookViewModel

private const val MAX_IMAGES = 5

/** Used for both "create" (bookId == null) and "edit". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingFormScreen(
    bookId: String?,
    bookVm: BookViewModel,
    authVm: AuthViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    var existing by remember { mutableStateOf<Book?>(null) }
    var loaded by remember { mutableStateOf(bookId == null) }
    LaunchedEffect(bookId) {
        if (bookId != null) {
            existing = bookVm.getBook(bookId)
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (bookId == null) "List a book" else "Edit listing") },
                navigationIcon = {
                    if (bookId != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        if (!loaded) {
            LoadingBox(Modifier.padding(padding))
        } else {
            ListingForm(existing, bookVm, authVm, onDone, Modifier.padding(padding))
        }
    }
}

@Composable
private fun ListingForm(
    existing: Book?,
    bookVm: BookViewModel,
    authVm: AuthViewModel,
    onDone: () -> Unit,
    modifier: Modifier
) {
    val user by authVm.currentUser.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var author by remember { mutableStateOf(existing?.author ?: "") }
    var isbn by remember { mutableStateOf(existing?.isbn ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var price by remember { mutableStateOf(existing?.price?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var condition by remember { mutableStateOf(existing?.condition ?: "") }
    var department by remember { mutableStateOf(existing?.department ?: "") }
    var semester by remember { mutableStateOf(existing?.semester ?: "") }
    var exchange by remember { mutableStateOf(existing?.exchangeAllowed ?: true) }
    val keptImages = remember { mutableStateListOf<String>().apply { addAll(existing?.imageList().orEmpty()) } }
    val newImages = remember { mutableStateListOf<Uri>() }
    var showErrors by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_IMAGES)) { uris ->
        val room = MAX_IMAGES - keptImages.size - newImages.size
        newImages.addAll(uris.take(room.coerceAtLeast(0)))
    }

    val saving = bookVm.saving
    val totalImages = keptImages.size + newImages.size

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Photos ($totalImages/$MAX_IMAGES)", style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Box(
                    Modifier.size(96.dp).clip(MaterialTheme.shapes.medium)
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                        .clickable(enabled = totalImages < MAX_IMAGES && !saving) {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, "Add photos", tint = MaterialTheme.colorScheme.primary)
                }
            }
            items(keptImages.toList()) { path ->
                ImageThumb(onRemove = { keptImages.remove(path) }) {
                    BookImage(path, Modifier.size(96.dp))
                }
            }
            items(newImages.toList()) { uri ->
                ImageThumb(onRemove = { newImages.remove(uri) }) {
                    AsyncImage(model = uri, contentDescription = null, modifier = Modifier.size(96.dp), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                }
            }
        }
        if (showErrors && totalImages == 0) {
            Text("Add at least one photo", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        OutlinedTextField(title, { title = it }, label = { Text("Book title *") }, isError = showErrors && title.isBlank(), singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(author, { author = it }, label = { Text("Author *") }, isError = showErrors && author.isBlank(), singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(isbn, { isbn = it }, label = { Text("ISBN") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        DropdownField("Category *", Options.categories, category, { category = it }, Modifier.fillMaxWidth(), isError = showErrors && category.isBlank())
        DropdownField(
            "Condition *", BookCondition.values().map { it.label },
            BookCondition.values().firstOrNull { it.name == condition }?.label ?: "",
            { label -> condition = BookCondition.values().first { it.label == label }.name },
            Modifier.fillMaxWidth(), isError = showErrors && condition.isBlank()
        )
        DropdownField("Department *", Options.departments, department, { department = it }, Modifier.fillMaxWidth(), isError = showErrors && department.isBlank())
        DropdownField("Semester *", Options.semesters, semester, { semester = it }, Modifier.fillMaxWidth(), isError = showErrors && semester.isBlank())
        OutlinedTextField(
            price, { price = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Price in ₹ * (0 = free)") },
            isError = showErrors && !Validators.isValidPrice(price), singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        OutlinedTextField(description, { description = it }, label = { Text("Description *") }, isError = showErrors && description.isBlank(), minLines = 3, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Open to exchange", Modifier.weight(1f))
            Switch(exchange, { exchange = it })
        }

        if (saving) {
            Text("Saving photos…", style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(progress = { bookVm.progress }, modifier = Modifier.fillMaxWidth())
        }

        Button(
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            onClick = {
                val valid = title.isNotBlank() && author.isNotBlank() && category.isNotBlank() &&
                    condition.isNotBlank() && department.isNotBlank() && semester.isNotBlank() &&
                    description.isNotBlank() && Validators.isValidPrice(price) && totalImages > 0
                if (!valid) {
                    showErrors = true
                } else {
                    bookVm.saveBook(
                        existing, user?.college.orEmpty(), title, author, isbn, category, description,
                        condition, price.trim().toDouble(), exchange, department, semester,
                        keptImages.toList(), newImages.toList(), onDone
                    )
                }
            }
        ) { Text(if (existing == null) "Publish listing" else "Save changes") }
    }
}

@Composable
private fun ImageThumb(onRemove: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.size(96.dp).clip(MaterialTheme.shapes.medium)) {
        content()
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd).size(28.dp).padding(4.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
        ) {
            Icon(Icons.Filled.Close, "Remove", Modifier.size(14.dp))
        }
    }
}
