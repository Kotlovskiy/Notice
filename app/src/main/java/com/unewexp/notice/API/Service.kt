package com.unewexp.notice.API

object Service {
    private var service: RetrofitServices? = null
    fun getService(): RetrofitServices{
        if(service != null){
            return service!!
        }else{
            service = Common.retrofitService
            return service!!
        }
    }
}