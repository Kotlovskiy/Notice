package com.unewexp.notice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.unewexp.notice.API.RetrofitServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ServerCheckService : Service() {
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var service: RetrofitServices = com.unewexp.notice.API.Service.getService()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createPersistentNotification("Служба проверки сервера"))

        scope.launch {
            while (true) {
                val newData = checkServer()

                if (newData.isNotEmpty()) {
                    showAdditionalNotification("Напоминания", "Появились новые напоминания, добавьте их в календарь")
                }

                delay(10 * 60 * 1000)
            }
        }
    }

    private suspend fun checkServer(): String {

        val context = getApplicationContext()
        val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("userId", -1)
        if(userId != -1) {
            try {
                val response = service.getNotifications(userId)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    if(body.size != com.unewexp.notice.NotificationManager.loadNotifications(context).size){
                        return "Появились новые напоминания, скорее добавь их в календарь, чтобы не забыть!"
                    }
                    Log.i("Response", body.toString())
                } else if (response.isSuccessful && response.body() == null) {
                    Log.i("Response", "Body null")
                } else {
                    Log.i("Response", "error " + response.code())
                }
            } catch (exception: Exception) {
                Log.i("MyTag", exception.message.toString())
            }
        }
        return ""
    }

    private fun showNotification(title: String, message: String) {
        val notification = createNotification(title, message)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(2, notification)
    }

    private fun createNotification(title: String, message: String): Notification {
        return NotificationCompat.Builder(this, "server_check_channel")
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun createPersistentNotification(title: String): Notification {
        return NotificationCompat.Builder(this, "server_check_channel")
            .setContentTitle(title)
            .setContentText("Работает в фоновом режиме")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Минимальный приоритет
            .setOngoing(true) // Уведомление нельзя свайпнуть
            .build()
    }

    private fun showAdditionalNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(this, "server_check_channel")
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(2, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "server_check_channel",
                "Server Check",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}