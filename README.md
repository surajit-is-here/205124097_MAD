# BookSwap

A college book exchange app for Android, built with Kotlin, Jetpack Compose and Material 3.
Students can list books they no longer need, browse what others have posted, and arrange a sale or swap through in-app requests and chat.

Everything is stored on the device, so there is **no backend, no Firebase and no internet connection required** once the project is built.

> **About this repository:** This is a practice project that builds on an earlier Android lab project (BookSwap). It is used for learning and experimenting, not as a new graded submission.

## Features

- **Accounts:** register, log in, log out, reset password, and a persistent session
- **Profile:** edit details and set a profile photo
- **Listings:** create, edit and delete book listings with multiple photos
- **Discovery:** browse, search and filter by category, department, semester and condition
- **Favorites:** save books to a favorites list
- **Exchange requests:** send, accept, reject, cancel and complete requests, optionally offering one of your own books
- **Chat:** in-app messaging between users, with read state
- **Notifications:** in-app notifications with an unread badge
- **Reports:** flag suspicious or inappropriate listings
- **Theme:** light and dark mode

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation Compose |
| State | ViewModel + StateFlow, Coroutines |
| Database | Room (SQLite) |
| Images | Coil, app-private file storage |
| Preferences | SharedPreferences (session and theme) |
| Build | Gradle (Kotlin DSL), KSP |
| Testing | JUnit |

## Architecture

```
UI (Compose screens) -> ViewModel (Auth / Book / Social) -> AppRepository -> Room DAO / files
```

```
app/src/main/java/com/example/bookswap/
  data/model        entities and enums
  data/local        Room database and DAO
  data/repository   AppRepository (single source of truth, business rules)
  viewmodel         AuthViewModel, BookViewModel, SocialViewModel
  ui/theme          colors, shapes, light/dark theme
  ui/components     reusable composables
  ui/screens        all screens
  ui/navigation     routes, NavHost, bottom bar
  utils             validators, formatting, dropdown options
```

## Requirements

- Android Studio Koala (2024.1.1) or newer
- JDK 17
- Android device or emulator running Android 7.0 (API 24) or higher
- Internet access on the first Gradle sync, to download dependencies

## Getting Started

1. Clone or download this repository.
2. Open the project folder in Android Studio (`File > Open`).
3. Wait for Gradle to finish syncing.
4. Select an emulator or connected phone and press **Run**.

To build a debug APK from the command line:

```bash
./gradlew assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/`.

## Trying Out an Exchange

Because all data lives on a single device, "multi-user" behaviour is demonstrated by switching accounts:

1. Register **Student A** and list a book.
2. Log out, then register **Student B**.
3. As Student B, open the book and send an exchange request.
4. Log out and log back in as Student A, then accept the request.
5. Use chat and notifications between the two accounts the same way.

## Testing

Unit tests for input validation are in `app/src/test`. Run them with:

```bash
./gradlew test
```

Manual checklist:

- [ ] Register with a bad email, a short password and mismatched passwords
- [ ] Log in and log out
- [ ] List a book with photos, then edit and delete it
- [ ] Search and apply filters
- [ ] Add and remove favorites
- [ ] Send, accept, reject, cancel and complete an exchange request
- [ ] Chat between two accounts and check the unread state
- [ ] Check the notification badge
- [ ] Mark a book as sold or exchanged
- [ ] Report a listing
- [ ] Switch between light and dark theme

## Security Notes

- Passwords are salted and hashed with SHA-256. This is fine for a practice project; use bcrypt or Argon2 for anything in production.
- Ownership rules (only owners can edit or delete, only receivers can accept, and so on) are enforced in `AppRepository`.
- Data is stored in the app's private sandbox.

## Roadmap

- Cloud sync (Firebase or Supabase)
- Push notifications
- Image compression
- ISBN barcode scanner
- Ratings and reviews for sellers

## Contributors

- Your Name
- Friend's Name