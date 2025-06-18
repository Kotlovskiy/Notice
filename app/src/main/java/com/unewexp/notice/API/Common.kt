package com.unewexp.notice.API

object Common {
    private val BASE_URL = "http://158.160.185.255:3000/"
    val retrofitService: RetrofitServices
        get() = RetrofitClient.getClient(BASE_URL).create(RetrofitServices::class.java)
}