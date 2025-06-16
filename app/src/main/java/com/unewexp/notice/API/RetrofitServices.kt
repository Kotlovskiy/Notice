package com.unewexp.notice.API

import com.unewexp.notice.Notification
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface RetrofitServices {
    @GET("Notifications")
    fun getNotifications(): Call<MutableList<Notification>>

    @POST("Notifications")
    fun addNotifications(@Body notification: Notification)
}