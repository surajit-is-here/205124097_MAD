package com.example.bookswap.ui.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bookswap.ui.screens.BookDetailsScreen
import com.example.bookswap.ui.screens.ChatScreen
import com.example.bookswap.ui.screens.EditProfileScreen
import com.example.bookswap.ui.screens.ExploreScreen
import com.example.bookswap.ui.screens.FavoritesScreen
import com.example.bookswap.ui.screens.ForgotPasswordScreen
import com.example.bookswap.ui.screens.HomeScreen
import com.example.bookswap.ui.screens.ListingFormScreen
import com.example.bookswap.ui.screens.LoginScreen
import com.example.bookswap.ui.screens.MyListingsScreen
import com.example.bookswap.ui.screens.NotificationsScreen
import com.example.bookswap.ui.screens.ProfileScreen
import com.example.bookswap.ui.screens.RegisterScreen
import com.example.bookswap.ui.screens.RequestDetailsScreen
import com.example.bookswap.ui.screens.RequestsScreen
import com.example.bookswap.ui.screens.SettingsScreen
import com.example.bookswap.ui.theme.BookSwapTheme
import com.example.bookswap.viewmodel.AuthViewModel
import com.example.bookswap.viewmodel.BookViewModel
import com.example.bookswap.viewmodel.SocialViewModel
import kotlinx.coroutines.flow.merge

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgotPassword"
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val BOOK_DETAILS = "bookDetails/{bookId}"
    const val CREATE_LISTING = "createListing"
    const val EDIT_LISTING = "editListing/{bookId}"
    const val FAVORITES = "favorites"
    const val REQUESTS = "requests"
    const val REQUEST_DETAILS = "requestDetails/{requestId}"
    const val CHAT = "chat/{conversationId}"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "editProfile"
    const val MY_LISTINGS = "myListings"
    const val SETTINGS = "settings"

    fun bookDetails(id: String) = "bookDetails/$id"
    fun editListing(id: String) = "editListing/$id"
    fun requestDetails(id: String) = "requestDetails/$id"
    fun chat(id: String) = "chat/$id"
}

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

private val bottomItems = listOf(
    BottomItem(Routes.HOME, "Home", Icons.Filled.Home),
    BottomItem(Routes.EXPLORE, "Explore", Icons.Filled.Explore),
    BottomItem(Routes.CREATE_LISTING, "Sell", Icons.Filled.AddCircle),
    BottomItem(Routes.REQUESTS, "Requests", Icons.Filled.SwapHoriz),
    BottomItem(Routes.PROFILE, "Profile", Icons.Filled.Person)
)

private val authRoutes = setOf(Routes.LOGIN, Routes.REGISTER, Routes.FORGOT_PASSWORD)

@Composable
fun BookSwapApp() {
    val authVm: AuthViewModel = viewModel()
    val bookVm: BookViewModel = viewModel()
    val socialVm: SocialViewModel = viewModel()

    val themeMode by authVm.themeMode.collectAsStateWithLifecycle()
    val dark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    BookSwapTheme(darkTheme = dark) {
        AppRoot(authVm, bookVm, socialVm)
    }
}

private fun NavHostController.goToTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AppRoot(authVm: AuthViewModel, bookVm: BookViewModel, socialVm: SocialViewModel) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val userId by authVm.currentUserId.collectAsStateWithLifecycle()
    val startDestination = remember { if (authVm.currentUserId.value != null) Routes.HOME else Routes.LOGIN }

    LaunchedEffect(Unit) {
        merge(authVm.messages, bookVm.messages, socialVm.messages).collect { snackbar.showSnackbar(it) }
    }

    // If the session ends (logout), go back to the login screen.
    LaunchedEffect(userId) {
        val route = nav.currentDestination?.route
        if (userId == null && route != null && route !in authRoutes) {
            nav.navigate(Routes.LOGIN) { popUpTo(nav.graph.id) { inclusive = true } }
        }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in bottomItems.map { it.route }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = { if (currentRoute != item.route) nav.goToTab(item.route) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { inner ->
        NavHost(nav, startDestination = startDestination, modifier = Modifier.padding(inner)) {

            composable(Routes.LOGIN) {
                LoginScreen(
                    authVm,
                    onLoggedIn = { nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                    onRegister = { nav.navigate(Routes.REGISTER) },
                    onForgot = { nav.navigate(Routes.FORGOT_PASSWORD) }
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    authVm,
                    onRegistered = { nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                    onBack = { nav.popBackStack() }
                )
            }
            composable(Routes.FORGOT_PASSWORD) {
                ForgotPasswordScreen(authVm, onDone = { nav.popBackStack() }, onBack = { nav.popBackStack() })
            }

            composable(Routes.HOME) {
                HomeScreen(
                    bookVm, authVm, socialVm,
                    onBook = { nav.navigate(Routes.bookDetails(it)) },
                    onSearch = { nav.goToTab(Routes.EXPLORE) },
                    onNotifications = { nav.navigate(Routes.NOTIFICATIONS) },
                    onProfile = { nav.goToTab(Routes.PROFILE) },
                    onCategory = { category ->
                        bookVm.updateFilters(bookVm.filters.value.copy(category = category))
                        nav.goToTab(Routes.EXPLORE)
                    }
                )
            }
            composable(Routes.EXPLORE) {
                ExploreScreen(bookVm, onBook = { nav.navigate(Routes.bookDetails(it)) })
            }
            composable(Routes.BOOK_DETAILS) { entry ->
                val bookId = entry.arguments?.getString("bookId").orEmpty()
                BookDetailsScreen(
                    bookId, bookVm, socialVm, authVm,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.editListing(it)) },
                    onChat = { nav.navigate(Routes.chat(it)) }
                )
            }
            composable(Routes.CREATE_LISTING) {
                ListingFormScreen(
                    bookId = null, bookVm = bookVm, authVm = authVm,
                    onBack = { nav.goToTab(Routes.HOME) },
                    onDone = { nav.navigate(Routes.MY_LISTINGS) { popUpTo(Routes.HOME) } }
                )
            }
            composable(Routes.EDIT_LISTING) { entry ->
                val bookId = entry.arguments?.getString("bookId").orEmpty()
                ListingFormScreen(
                    bookId = bookId, bookVm = bookVm, authVm = authVm,
                    onBack = { nav.popBackStack() },
                    onDone = { nav.popBackStack() }
                )
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(bookVm, onBook = { nav.navigate(Routes.bookDetails(it)) }, onBack = { nav.popBackStack() })
            }
            composable(Routes.REQUESTS) {
                RequestsScreen(
                    socialVm, bookVm, authVm,
                    onRequest = { nav.navigate(Routes.requestDetails(it)) },
                    onChat = { nav.navigate(Routes.chat(it)) }
                )
            }
            composable(Routes.REQUEST_DETAILS) { entry ->
                val id = entry.arguments?.getString("requestId").orEmpty()
                RequestDetailsScreen(
                    id, socialVm, bookVm, authVm,
                    onBack = { nav.popBackStack() },
                    onBook = { nav.navigate(Routes.bookDetails(it)) },
                    onChat = { nav.navigate(Routes.chat(it)) }
                )
            }
            composable(Routes.CHAT) { entry ->
                val id = entry.arguments?.getString("conversationId").orEmpty()
                ChatScreen(id, socialVm, bookVm, authVm, onBack = { nav.popBackStack() })
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(
                    socialVm,
                    onBack = { nav.popBackStack() },
                    onOpen = { type, relatedId ->
                        when (type) {
                            "REQUEST" -> nav.navigate(Routes.requestDetails(relatedId))
                            "MESSAGE" -> nav.navigate(Routes.chat(relatedId))
                            else -> nav.navigate(Routes.bookDetails(relatedId))
                        }
                    }
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    authVm,
                    onMyListings = { nav.navigate(Routes.MY_LISTINGS) },
                    onFavorites = { nav.navigate(Routes.FAVORITES) },
                    onMyRequests = { nav.goToTab(Routes.REQUESTS) },
                    onEditProfile = { nav.navigate(Routes.EDIT_PROFILE) },
                    onSettings = { nav.navigate(Routes.SETTINGS) },
                    onLogout = { authVm.logout() }
                )
            }
            composable(Routes.EDIT_PROFILE) {
                EditProfileScreen(authVm, onBack = { nav.popBackStack() })
            }
            composable(Routes.MY_LISTINGS) {
                MyListingsScreen(
                    bookVm,
                    onBook = { nav.navigate(Routes.bookDetails(it)) },
                    onEdit = { nav.navigate(Routes.editListing(it)) },
                    onAdd = { nav.goToTab(Routes.CREATE_LISTING) },
                    onBack = { nav.popBackStack() }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(authVm, onBack = { nav.popBackStack() })
            }
        }
    }
}
