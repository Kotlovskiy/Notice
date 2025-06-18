package com.unewexp.notice

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.Date
import java.util.TimeZone

data class Calendar(
    val id: Long,
    val name: String,
    val account: String,
    val displayName: String
)

data class CalendarEvent(
    val id: Long,
    val title: String,
    val description: String?,
    val startTime: Long,
    val endTime: Long,
    val location: String? = null
)

fun getCalendars(context: Context): List<Calendar> {
    val calendars = mutableListOf<Calendar>()
    val uri = CalendarContract.Calendars.CONTENT_URI

    val projection = arrayOf(
        CalendarContract.Calendars._ID,
        CalendarContract.Calendars.NAME,
        CalendarContract.Calendars.ACCOUNT_NAME,
        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
    )

    context.contentResolver.query(
        uri,
        projection,
        null,
        null,
        null
    )?.use { cursor ->
        while (cursor.moveToNext()) {
            calendars.add(
                Calendar(
                    id = cursor.getLong(0),
                    name = cursor.getString(1) ?: "",
                    account = cursor.getString(2) ?: "",
                    displayName = cursor.getString(3) ?: ""
                )
            )
        }
    }

    return calendars
}

fun addEventToCalendar(
    context: Context,
    calendarId: Long,
    title: String,
    description: String,
    startTime: Long,
    endTime: Long,
    reminderMinutes: Int = 10
): Long? {
    val values = ContentValues().apply {
        put(CalendarContract.Events.CALENDAR_ID, calendarId)
        put(CalendarContract.Events.TITLE, title)
        put(CalendarContract.Events.DESCRIPTION, description)
        put(CalendarContract.Events.DTSTART, startTime)
        put(CalendarContract.Events.DTEND, endTime)
        put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        put(CalendarContract.Events.HAS_ALARM, 1)
    }

    return try {
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        uri?.lastPathSegment?.toLong()?.also { eventId ->
            addReminderToEvent(context, eventId, reminderMinutes)
        }
    } catch (e: Exception) {
        null
    }
}

private fun addReminderToEvent(context: Context, eventId: Long, minutes: Int) {
    val values = ContentValues().apply {
        put(CalendarContract.Reminders.EVENT_ID, eventId)
        put(CalendarContract.Reminders.MINUTES, minutes)
        put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
    }

    context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
}

@Composable
fun CalendarContentScreen() {
    val context = LocalContext.current
    var calendars by remember { mutableStateOf<List<Calendar>>(emptyList()) }
    var selectedCalendar by remember { mutableStateOf<Calendar?>(null) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        calendars = getCalendars(context)
        selectedCalendar = calendars.firstOrNull()
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedCalendar?.displayName ?: "Выберите календарь")
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                calendars.forEach { calendar ->
                    DropdownMenuItem(
                        text = {Text(calendar.displayName)},
                        onClick = {
                            selectedCalendar = calendar
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                selectedCalendar?.let { calendar ->
                    val now = System.currentTimeMillis()
                    val inOneHour = now + 60 * 60 * 1000
                    addEventToCalendar(
                        context = context,
                        calendarId = calendar.id,
                        title = "Новое напоминание",
                        description = "Описание напоминания",
                        startTime = now,
                        endTime = inOneHour,
                        reminderMinutes = 15
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Добавить напоминание")
        }
    }
}

@Composable
fun RequestCalendarPermission(
    onPermissionsGranted: () -> Unit,
    onPermissionsDenied: () -> Unit
) {
    val context = LocalContext.current
    val permissions = remember {
        arrayOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.values.all { it }
        if (allGranted) {
            onPermissionsGranted()
        } else {
            onPermissionsDenied()
            Toast.makeText(
                context,
                "Разрешения необходимы для работы с календарём",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val permissionCheck = {
        val hasReadPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        val hasWritePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (hasReadPermission && hasWritePermission) {
            onPermissionsGranted()
        } else {
            launcher.launch(permissions)
        }
    }

    LaunchedEffect(Unit) {
        permissionCheck()
    }
}

@Composable
fun CalendarIntegrationScreen() {
    var hasPermissions by remember { mutableStateOf(false) }

    RequestCalendarPermission(
        onPermissionsGranted = { hasPermissions = true },
        onPermissionsDenied = { hasPermissions = false }
    )

    if (hasPermissions) {
        // Основной контент с календарём
        CalendarContentScreen()
    } else {
        // Сообщение о необходимости разрешений
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Для работы с календарём нужны разрешения")
        }
    }
}