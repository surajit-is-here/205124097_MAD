# BookSwap (local-storage edition)

A college book exchange app built with Kotlin, Jetpack Compose and Material 3.
**No Firebase, no backend, no internet needed** – everything is stored on the device.

## Features
Register / login / logout / reset password (persistent session) · profile + photo · browse, search
and filter books · list / edit / delete books with multiple photos · favorites · exchange requests
(accept / reject / cancel / complete) · in-app chat with read state · in-app notifications ·
report listings · light / dark theme.

## Tech stack
Kotlin, Jetpack Compose, Material 3, Navigation Compose, ViewModel + StateFlow, Coroutines,
Room (SQLite) for data, app-private files for images, SharedPreferences for session/theme, Coil.

## Architecture
`UI (Compose screens) -> ViewModel (Auth/Book/Social) -> AppRepository -> Room DAO / files`

```
app/src/main/java/com/example/bookswap/
  data/model       entities + enums
  data/local       Room database + DAO
  data/repository  AppRepository (single source of truth, business rules)
  viewmodel        AuthViewModel, BookViewModel, SocialViewModel
  ui/theme         colors, shapes, light/dark theme
  ui/components    reusable composables
  ui/screens       all screens
  ui/navigation    routes + NavHost + bottom bar
  utils            validators, formatting, dropdown options
```

## Run it
1. Install Android Studio (Koala 2024.1.1 or newer) and open this folder (`File > Open`).
2. Let Gradle sync (needs internet once to download libraries).
3. Pick an emulator or phone (Android 7.0+) and press Run.

## Important: how "multi-user" works
Data lives on one device, so to demo an exchange: register **Student A**, list a book, log out,
register **Student B**, request the book, log out, log in as A, accept. Chat and notifications work
the same way between the accounts on that device.

## Security notes
Passwords are salted + SHA-256 hashed (fine for a college project; use bcrypt/Argon2 for production).
Ownership rules (only owners edit/delete, only receivers accept, etc.) are enforced in `AppRepository`.
Data is private to the app's sandbox.

## Manual test checklist
Register (try bad email / short password / mismatch) · login/logout · list a book with photos ·
search & filters · favorite · request exchange · accept/reject/cancel/complete · chat between two
accounts · notifications badge · edit/delete listing · mark sold/exchanged · report · theme switch.

## Future improvements
Cloud sync (Firebase/Supabase), push notifications, payments, image compression, ISBN scanner.
