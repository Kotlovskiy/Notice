package com.unewexp.notice

import java.util.UUID

data class Notification(
    val id: String = UUID.randomUUID().toString(),
    var userId: String,
    var text: String,
    var category: String,
    var date: String,
    var time: String,
    var place: String,
    var isCompleted: Boolean,
    var activationCondition: String
)
