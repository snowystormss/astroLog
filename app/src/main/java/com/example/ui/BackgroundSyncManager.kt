package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A dedicated manager/hook that monitors the user authentication state and profile completion,
 * dynamically enabling or disabling the background data fetching process to conserve resources.
 */
@Composable
fun BackgroundSyncManager(viewModel: MainViewModel) {
    val context = LocalContext.current
    val profile = viewModel.userProfile.collectAsStateWithLifecycle().value
    val isProfileLoading = viewModel.isProfileLoading.collectAsStateWithLifecycle().value

    LaunchedEffect(profile, isProfileLoading) {
        if (!isProfileLoading) {
            withContext(Dispatchers.IO) {
                if (profile != null) {
                    val dao = DatabaseProvider.database.astrologyDao()
                    val settings = dao.getUserSettingsSync()
                    val timePref = settings?.dailyReminderTime ?: "Morning"
                    NotificationHelper.scheduleDailyReminder(context, timePref)
                } else {
                    NotificationHelper.cancelDailyReminder(context)
                }
            }
        }
    }
}
