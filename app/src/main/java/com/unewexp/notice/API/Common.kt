package com.unewexp.notice.API

object Common {
    private val BASE_URL = "95.182.120.75"
    val retrofitService: RetrofitServices
        get() = RetrofitClient.getClient(BASE_URL).create(RetrofitServices::class.java)
}