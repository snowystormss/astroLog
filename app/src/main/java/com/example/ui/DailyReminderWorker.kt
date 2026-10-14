package com.example.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.AstrologyRepository
import com.example.data.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class DailyReminderWorker(private val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val dao = DatabaseProvider.database.astrologyDao()
            val repo = AstrologyRepository(dao)
            val profile = dao.getUserProfileSync()
            val settings = dao.getUserSettingsSync()

            if (profile != null) {
                // Background update: generate the daily feed and affirmation so it's up to date
                val dateParts = profile.birthDate.split("/")
                val month = dateParts.getOrNull(0)?.toIntOrNull() ?: 1
                val day = dateParts.getOrNull(1)?.toIntOrNull() ?: 1
                val year = dateParts.getOrNull(2)?.toIntOrNull() ?: 2000
                val planets = PlanetCalculator.calculatePlanets(year, month, day, 12.0, profile.latitude, profile.longitude)
                
                val sun = planets["Sun"]?.sign ?: "Aries"
                val moon = planets["Moon"]?.sign ?: "Aries"
                val venus = planets["Venus"]?.sign ?: "Aries"
                val mars = planets["Mars"]?.sign ?: "Aries"
                val todayStr = java.time.LocalDate.now().toString()

                val feedPrompt = "" +
                    "The user has Sun in $sun, Moon in $moon, Venus in $venus, and Mars in $mars (Birth Date: ${profile.birthDate}). " +
                    "Today's date is $todayStr. " +
                    "First, use your Google Search tool to fetch the current, real-time planetary transits for $todayStr. " +
                    "Second, compare these current real-time transits against the user's natal placements to suggest potential upcoming life events and shifts. " +
                    "Generate between 4 to 8 profound, psychological and astrological insights for them today based on these transits. " +
                    "Keep the tone like 'The Pattern' or 'Co-Star' app: direct, psychological, slightly mystical, empathetic, not too conversational. " +
                    "For each insight, provide:\n" +
                    "1. A short title (max 4 words).\n" +
                    "2. A short general advice sentence.\n" +
                    "3. A multi-sentence 'dive deeper' explanation detailing what this transit means for the person.\n" +
                    "Format EACH insight strictly as follows, separating distinct insights with the string '|||':\n" +
                    "TITLE: [title]\n" +
                    "ADVICE: [advice]\n" +
                    "DEEP_DIVE: [deep dive]\n" +
                    "|||"
                
                repo.generateAndSaveInsight("DAILY_FEED", feedPrompt, replace = true)

                val affPrompt = "The user has Sun in $sun and Moon in $moon. Today is $todayStr. Provide a beautiful, one-sentence empowering daily affirmation."
                repo.generateAndSaveInsight("DAILY_AFFIRMATION", affPrompt, replace = true)
            }

            // Only show notification if enabled
            if (settings?.dailyReminderEnabled == true) {
                val quotes = listOf(
                    "The stars incline, they do not bind.",
                    "A new moon brings new beginnings. Embrace the unknown.",
                    "Your intuition is your inner compass. Trust it today.",
                    "As above, so below; as within, so without.",
                    "Retrogrades are times of reflection, not fear. Take a moment to breathe.",
                    "Check your planetary weather for the day!",
                    "Your daily astrology insights are ready. See what the cosmos have in store."
                )
                val todayQuote = quotes.random()
                showNotification("Daily Astrology Insight", todayQuote, 1) // ID 1 for daily
            }

            if (settings?.notifyLunarPhases == true && profile != null) {
                val y = java.time.LocalDate.now().year
                val m = java.time.LocalDate.now().monthValue
                val d = java.time.LocalDate.now().dayOfMonth
                
                val planetsYesterday = PlanetCalculator.calculatePlanets(y, m, d - 1)
                val diffYesterday = (planetsYesterday["Moon"]!!.longitude - planetsYesterday["Sun"]!!.longitude + 360.0) % 360.0
                val planetsToday = PlanetCalculator.calculatePlanets(y, m, d)
                val diffToday = (planetsToday["Moon"]!!.longitude - planetsToday["Sun"]!!.longitude + 360.0) % 360.0
                
                val isFullMoon = (diffYesterday < 180 && diffToday >= 180)
                val isNewMoon = (diffYesterday in 250.0..359.0 && diffToday in 0.0..60.0)
                
                if (isFullMoon || isNewMoon) {
                    val phaseName = if (isFullMoon) "Full Moon" else "New Moon"
                    val sign = planetsToday["Moon"]?.sign ?: "Aries"
                    val prompt = "Today is a $phaseName in $sign. The user is a ${profile.sunSign}. Give a 2-3 sentence personalized notification message about how this lunar phase affects them."
                    val reading = repo.generateShortReading(prompt)
                    if (!reading.isNullOrBlank()) {
                        showNotification("$phaseName in $sign", reading, 2) // ID 2 for lunar
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun showNotification(title: String, message: String, notificationId: Int = System.currentTimeMillis().toInt()) {
        val channelId = "astrology_daily_reminder"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Astrology Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
