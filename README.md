# READRELAY

**ReadRelay** is an Android-based college book exchange application designed to help students buy, sell, and exchange academic books within their student community.

The application allows users to create book listings, discover available books, save favorites, send exchange requests, communicate through in-app chat, and receive notifications.

ReadRelay is designed as an **offline-first application**. All application data is stored locally on the device using Room Database and app-private file storage. No backend server or Firebase service is required.

---

## Project Overview

Students often have textbooks and reference books that they no longer need after completing a semester or course. At the same time, other students may need the same books but prefer purchasing or exchanging them at a lower cost.

ReadRelay provides a simple platform where students can:

- List books they want to sell or exchange
- Discover books listed by other users
- Search and filter books according to their requirements
- Send and manage exchange requests
- Communicate with other users through in-app chat
- Keep track of favorites and notifications

Since this project does not use a backend, multiple-user functionality is demonstrated by switching between accounts on the same device.

---

## Objectives

The main objectives of ReadRelay are:

- To provide a simple platform for exchanging academic books.
- To reduce the cost of acquiring textbooks for students.
- To encourage reuse of books within the student community.
- To demonstrate Android application development using modern Kotlin technologies.
- To implement local data persistence using Room Database.
- To implement application architecture using ViewModel, StateFlow, and Repository patterns.
- To provide an offline-first user experience.

---

## Features

### Accounts

- User registration
- Login and logout
- Password reset
- Persistent login session

### Profile

- Edit user details
- Set and update profile photo

### Book Listings

- Create book listings
- Add multiple photos
- Edit listings
- Delete listings
- Mark books as sold or exchanged

### Discovery

- Browse available books
- Search for books
- Filter by:
  - Category
  - Department
  - Semester
  - Condition

### Favorites

- Add books to favorites
- Remove books from favorites
- View saved books

### Exchange Requests

- Send exchange requests
- Accept requests
- Reject requests
- Cancel requests
- Complete exchanges
- Optionally offer one of your own books

### Chat

- In-app messaging between users
- Read and unread message state

### Notifications

- In-app notifications
- Unread notification badge

### Reports

- Report suspicious or inappropriate listings

### Theme

- Light mode
- Dark mode

---

## Technology Stack

| Area | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Design System | Material 3 |
| Navigation | Navigation Compose |
| State Management | ViewModel, StateFlow |
| Asynchronous Programming | Kotlin Coroutines |
| Database | Room / SQLite |
| Image Loading | Coil |
| Image Storage | App-private file storage |
| Preferences | SharedPreferences |
| Build System | Gradle Kotlin DSL |
| Annotation Processing | KSP |
| Testing | JUnit |

---

## Architecture

ReadRelay follows a layered architecture based on the MVVM and Repository patterns.

```text
UI (Jetpack Compose)
        ↓
ViewModel
(AuthViewModel / BookViewModel / SocialViewModel)
        ↓
AppRepository
        ↓
Room DAO / Local File Storage
```

### Project Structure

```text
app/src/main/java/com/example/readrelay/

├── data/
│   ├── model/          # Entities and enums
│   ├── local/          # Room database and DAOs
│   └── repository/     # AppRepository and business rules
│
├── viewmodel/          # Application ViewModels
│
├── ui/
│   ├── theme/          # Colors, shapes and themes
│   ├── components/     # Reusable Compose components
│   ├── screens/        # Application screens
│   └── navigation/     # Routes, NavHost and bottom navigation
│
└── utils/              # Validators, formatting and dropdown options
```

---

## Requirements

- Android Studio Koala (2024.1.1) or newer
- JDK 17
- Android device or emulator running Android 7.0 (API 24) or higher
- Internet connection during the initial Gradle sync to download dependencies

After the required dependencies have been downloaded, the application itself does not require an internet connection.

---

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/surajit-is-here/205124097_MAD.git
```

### 2. Open the Project

Open the cloned project in Android Studio:

```text
File → Open
```

Select the project directory.

### 3. Gradle Sync

Allow Android Studio to complete the Gradle synchronization and download the required dependencies.

### 4. Run the Application

Connect an Android device or start an Android emulator, then press **Run** in Android Studio.

---

## Building the APK

To build a debug APK from the command line:

```bash
./gradlew assembleDebug
```

The generated APK will typically be located at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## Demonstrating an Exchange

Because ReadRelay does not use a backend, multiple-user functionality can be demonstrated using multiple accounts on the same device.

### Student A

1. Register an account as Student A.
2. Create and publish a book listing.

### Student B

3. Log out from Student A.
4. Register or log in as Student B.
5. Browse the available books.
6. Open Student A's listing.
7. Send an exchange request.

### Student A

8. Log out from Student B.
9. Log back in as Student A.
10. View the incoming request.
11. Accept the request.
12. Use the chat functionality to communicate with Student B.

This demonstrates the exchange request, notification, and chat functionality using the locally stored accounts.

---

## Testing

### Automated Tests

Unit tests for input validation are located in:

```text
app/src/test
```

Run the tests using:

```bash
./gradlew test
```

### Manual Testing Checklist

- [ ] Register with invalid email
- [ ] Register with a short password
- [ ] Register with mismatched passwords
- [ ] Log in and log out
- [ ] Verify persistent login session
- [ ] Create a book listing
- [ ] Add multiple book photos
- [ ] Edit a listing
- [ ] Delete a listing
- [ ] Search for books
- [ ] Apply category, department, semester and condition filters
- [ ] Add and remove favorites
- [ ] Send an exchange request
- [ ] Accept an exchange request
- [ ] Reject an exchange request
- [ ] Cancel an exchange request
- [ ] Complete an exchange
- [ ] Chat between two accounts
- [ ] Verify unread message state
- [ ] Verify notification badge
- [ ] Mark a book as sold or exchanged
- [ ] Report a listing
- [ ] Switch between light and dark themes
- [ ] Verify application data persists after restarting the app

---

## Security and Data Storage

- User data is stored locally inside the application's private sandbox.
- Ownership and authorization rules are enforced through the application repository layer.
- Only authorized users can modify their own listings.
- Exchange request actions are restricted according to the request state and participating users.
- Passwords are salted and hashed using SHA-256 in the current academic implementation.

> **Note:** SHA-256 is used for demonstration purposes in this academic project. A production application should use a password-specific hashing algorithm such as Argon2id, bcrypt, or scrypt.

---

## Limitations

The current version of ReadRelay has the following limitations:

- No cloud synchronization
- User accounts and application data are restricted to the local device
- Images are stored locally

---

## Future Enhancements

Planned improvements include:

- Cloud synchronization using Firebase or Supabase
- Image compression and optimization
- ISBN/barcode scanner
- Advanced search and recommendation features
- University-specific authentication
- Location-based book discovery

---

## Project Information

**Project Name:** ReadRelay  
**Platform:** Android  
**Language:** Kotlin  
**UI Framework:** Jetpack Compose  
**Database:** Room / SQLite  
**Architecture:** MVVM + Repository Pattern  
**Storage:** Local Device Storage  

---

## Contributors

**Surajit Das**

---

## License

This project was developed as an academic/college project.

It is intended for educational and demonstration purposes.