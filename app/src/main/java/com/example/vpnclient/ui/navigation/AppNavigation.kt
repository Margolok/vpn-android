package com.example.vpnclient.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.vpnclient.VpnClientApp
import com.example.vpnclient.ui.screens.auth.LoginScreen
import com.example.vpnclient.ui.screens.home.HomeScreen
import com.example.vpnclient.ui.screens.profiles.ProfilesScreen
import com.example.vpnclient.ui.screens.settings.SettingsScreen

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val PROFILES = "profiles"
    const val SETTINGS = "settings"
}

/**
 * Корневая навигация LoudVPN. Стартовый экран зависит от того, авторизован ли
 * пользователь ([com.example.vpnclient.data.auth.AuthRepository.currentUser]):
 * без сессии показывается [LoginScreen], иначе — сразу [HomeScreen].
 * Пока идёт первая проверка DataStore, показывается индикатор загрузки, чтобы
 * не мигнуть экраном входа для уже авторизованного пользователя.
 */
@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val authRepository = (context.applicationContext as VpnClientApp).authRepository
    val currentUser by authRepository.currentUser.collectAsState(initial = UNKNOWN_USER)

    if (currentUser === UNKNOWN_USER) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var hasNavigatedAfterLogin by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = if (currentUser != null) Routes.HOME else Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen()
        }
        composable(Routes.HOME) {
            HomeScreen(onNavigateToProfiles = { navController.navigate(Routes.PROFILES) })
        }
        composable(Routes.PROFILES) {
            ProfilesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen()
        }
    }

    // Как только пользователь успешно вошёл (в т.ч. вернувшись из OAuth Custom
    // Tab), переводим со стартового экрана логина на главный.
    LaunchedEffect(currentUser) {
        val onLoginScreen = navController.currentDestination?.route == Routes.LOGIN
        if (currentUser != null && onLoginScreen && !hasNavigatedAfterLogin) {
            hasNavigatedAfterLogin = true
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        }
        if (currentUser == null) {
            hasNavigatedAfterLogin = false
        }
    }
}

/** Сентинел, чтобы отличить "ещё не читали DataStore" от "точно нет пользователя" (null). */
private val UNKNOWN_USER = com.example.vpnclient.data.model.AuthUser(
    id = "__unknown__", displayName = "", email = null,
    provider = com.example.vpnclient.data.model.AuthProvider.EMAIL
)
