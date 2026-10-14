package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.UserProfile

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    profile: UserProfile?
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    if (profile == null) {
        OnboardingScreen(
            viewModel = viewModel,
            onProfileCreated = { n, d, t, l, lat, lon -> viewModel.createProfile(n, d, t, l, lat, lon) }
        )
        return
    }

    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Daily") },
                    label = { Text("Daily") },
                    selected = currentRoute == "home",
                    onClick = { navController.navigate("home") { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Calendar") },
                    label = { Text("Calendar") },
                    selected = currentRoute == "calendar",
                    onClick = { navController.navigate("calendar") { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = currentRoute == "profile",
                    onClick = { navController.navigate("profile") { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Compatibility") },
                    label = { Text("Compatibility") },
                    selected = currentRoute == "compatibility",
                    onClick = { navController.navigate("compatibility") { launchSingleTop = true } }
                )
            }
        }
    ) { padding ->
        val animSpec = tween<Float>(300)
        val offsetSpec = tween<androidx.compose.ui.unit.IntOffset>(300)
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(animSpec) + androidx.compose.animation.slideInVertically(initialOffsetY = { 50 }, animationSpec = offsetSpec) },
            exitTransition = { fadeOut(animSpec) + androidx.compose.animation.slideOutVertically(targetOffsetY = { -50 }, animationSpec = offsetSpec) },
            popEnterTransition = { fadeIn(animSpec) + androidx.compose.animation.slideInVertically(initialOffsetY = { -50 }, animationSpec = offsetSpec) },
            popExitTransition = { fadeOut(animSpec) + androidx.compose.animation.slideOutVertically(targetOffsetY = { 50 }, animationSpec = offsetSpec) }
        ) {
            composable("home") { HomeScreen(viewModel, profile) }
            composable("calendar") { CalendarScreen(viewModel, profile) }
            composable("profile") { 
                ProfileScreen(
                    viewModel = viewModel, 
                    profile = profile,
                    onNavigateToSettings = { navController.navigate("settings") { launchSingleTop = true } },
                    onNavigateToSyncSettings = { navController.navigate("sync_settings") { launchSingleTop = true } },
                    onNavigateToCalendar = { navController.navigate("calendar") { launchSingleTop = true } }
                ) 
            }
            composable("compatibility") { CompatibilityScreen(viewModel, profile) }
            composable("settings") { 
                SettingsScreen(
                    viewModel = viewModel, 
                    profile = profile, 
                    onBack = { navController.popBackStack() }
                ) 
            }
            composable("sync_settings") {
                SyncSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
