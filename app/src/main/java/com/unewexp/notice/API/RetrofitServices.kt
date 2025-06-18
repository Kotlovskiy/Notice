package com.unewexp.notice.API

import com.unewexp.notice.Notification
import com.unewexp.notice.Test
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface RetrofitServices {
    @GET("tasks")
    suspend fun getNotifications(@Body userId: Int): Response<MutableList<Notification>>

    @POST("tasks")
    suspend fun addNotifications(@Body user_id: Int, @Body notification: Notification)

    @POST("user")
    suspend fun addUser(@Body userName: String, @Body password: String): Response<Int>

    @POST("user")
    suspend fun checkUser(@Body userName: String, @Body password: String): Response<Int>

    @POST("user")
    suspend fun boundTg(@Body userId: Int, @Body tg: String): Response<Boolean>

    @Multipart
    @POST("transcribe")
    suspend fun uploadAudio(
        @Part file: MultipartBody.Part
    ): Response<Test>
}