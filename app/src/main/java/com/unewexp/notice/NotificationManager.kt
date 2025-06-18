package com.unewexp.notice

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlin.collections.mutableListOf
import androidx.core.content.edit
import com.google.gson.reflect.TypeToken

object NotificationManager {
    private var _notifications = mutableListOf<Notification>()
    val notifications
        get() = _notifications.toList()

    fun add(ntf: Notification){
        _notifications.add(ntf)
    }
    fun delete(id: String){
        _notifications.removeIf {
            it.id == id
        }
    }
    fun search(id: String): Notification? {
        return _notifications.firstOrNull {
            it.id == id
        }
    }
    fun replace(id: String, ntf: Notification){
        for(i in 0.._notifications.size - 1){
            if(_notifications[i].id == id){
                Log.i("Find", (_notifications[i].id == ntf.id).toString())
                Log.i("Find", (_notifications[i].text == ntf.text).toString())
                _notifications[i] = ntf
                Log.i("Find", (_notifications[i].id == ntf.id).toString())
                Log.i("Find", (_notifications[i].text == ntf.text).toString())
            }
        }
    }
    fun initNotifications(list: MutableList<Notification>){
        _notifications = list
    }

    fun saveNotifications(context: Context) {
        val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit {

            val gson = Gson()
            val json = gson.toJson(notifications)

            putString("notifications", json)
        }
    }

    fun loadNotifications(context: Context): List<Notification> {
        val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("notifications", null)

        return if (json != null) {
            val gson = Gson()
            val type = object : TypeToken<List<Notification>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }
}