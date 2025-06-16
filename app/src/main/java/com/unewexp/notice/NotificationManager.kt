package com.unewexp.notice

import android.util.Log

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
}