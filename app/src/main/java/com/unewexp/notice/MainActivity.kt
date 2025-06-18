package com.unewexp.notice

import android.Manifest
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.util.Log
import android.view.animation.OvershootInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.unewexp.notice.API.Common
import com.unewexp.notice.API.RetrofitClient
import com.unewexp.notice.API.RetrofitServices
import com.unewexp.notice.API.Service
import com.unewexp.notice.ui.theme.NoticeTheme
import com.unewexp.notice.ui.theme.Typography
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class Test(
    val result: String
)

class MainActivity : ComponentActivity() {

    private val recordController = RecordController(this)
    private var countDownTimer: CountDownTimer? = null
    lateinit var service: RetrofitServices

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            777,
        )
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            777,
        )
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_NOTIFICATION_POLICY),
            777,
        )
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE),
            777,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }

        service = Service.getService()

        val intent = Intent(this, ServerCheckService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        val context = getApplicationContext()
        val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        var userId = sharedPreferences.getInt("userId", -1)

        enableEdgeToEdge()
        setContent {
            NoticeTheme {
                val navController = rememberNavController()
                var startDestination = if (userId == -1) "Registration" else "NotificationsScreen"

                //CalendarIntegrationScreen()
//                Box(
//                    modifier = Modifier.fillMaxSize(),
//                    contentAlignment = Alignment.Center
//                ){
//                    NotificationScheduler()
//                }

                NavHost(navController = navController, startDestination = startDestination) {
                    composable("NotificationsScreen") { NotificationsScreen(navController) }
                    composable("NotificationScreen/{id}") { backStackEntry ->
                        val notificationId = backStackEntry.arguments?.getString("id")
                        NotificationScreen(notificationId!!, navController)
                    }
                    composable("NotificationEditScreen/{id}") { backStackEntry ->
                        val notificationId = backStackEntry.arguments?.getString("id")
                        NotificationEditScreen(
                            notificationId!!,
                            navController,
                            { startEndRecord() },
                            { endRecord() }
                        )
                    }
                    composable("CreateNotificationScreen") { backStackEntry ->
                        CreateNotificationScreen(
                            navController,
                            { startEndRecord() },
                            { endRecord() }
                        )
                    }
                    composable("Registration") { backStackEntry ->
                        RegistrationScreen(
                            service,
                            navController
                        )
                    }
                    composable("Profile") { backStackEntry ->
                        UserProfileScreen()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        NotificationManager.saveNotifications(this)
    }

    private suspend fun getNotifications() {
        lateinit var notifications: List<Notification>
        val context = getApplicationContext()
        val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        var userId = sharedPreferences.getInt("userId", -1)

        try {
            val response = service.getNotifications(userId)
            val body = response.body()
            if(body != null){
                NotificationManager.initNotifications(body)
            }
        } catch (exception: Exception) {
            Log.i("MyTag", exception.message.toString())
        }
    }

    private suspend fun postAudio(file: File){
        try {
            val requestBody = file.asRequestBody("audio/*".toMediaType())
            val audioPart = MultipartBody.Part.createFormData("audio", file.name, requestBody)
            val response = service.uploadAudio(audioPart)
            val body = response.body()
            if(response.isSuccessful && body != null){
                Log.i("Response", body.result)
            }else if(response.isSuccessful && response.body() == null){
                Log.i("Response", "Body null")
            }else{
                Log.i("Response", "error " + response.code())
            }
        } catch (exception: Exception) {
            Log.i("MyTag", exception.message.toString())
        }
        file.delete()
    }

    private suspend fun postTg(tg: String){
        try {
            val response = service.boundTg(1, tg)
            val body = response.body()
            if(response.isSuccessful && body != null){
                Log.i("Response", body.toString())
            }else if(response.isSuccessful && response.body() == null){
                Log.i("Response", "Body null")
            }else{
                Log.i("Response", "error " + response.code())
            }
        }catch (exception: Exception) {
            Log.i("MyTag", exception.message.toString())
        }
    }

    private suspend fun checkUser(name: String, password: String){
        try {
            val response = service.checkUser(name, password)
            val body = response.body()
            if(response.isSuccessful && body != null){
                Log.i("Response", body.toString())
            }else if(response.isSuccessful && response.body() == null){
                Log.i("Response", "Body null")
            }else{
                Log.i("Response", "error " + response.code())
            }
        }catch (exception: Exception) {
            Log.i("MyTag", exception.message.toString())
        }
    }

    private fun endRecord(): Boolean{
        if (!recordController.isAudioRecording()) return false
        val path = recordController.stop()
        countDownTimer?.cancel()
        GlobalScope.launch {
            postAudio(getFileFromPath(path)!!)
        }
        countDownTimer = null
        return true
    }

    private fun startEndRecord() {
        if (recordController.isAudioRecording()) {
            endRecord()
        } else {
            recordController.start()
            countDownTimer = object : CountDownTimer(60_000, VOLUME_UPDATE_DURATION) {
                override fun onTick(p0: Long) {
                    val volume = recordController.getVolume()
                    Log.d(TAG, "Volume = $volume")
                }

                override fun onFinish() {
                }
            }.apply {
                start()
            }
        }
    }

    private fun getFileFromPath(path: String): File? {
        return try {
            val fileName = path.substringAfterLast("/")
            val file = File(Environment.getExternalStorageDirectory().absolutePath + "/AudioBooks", fileName)
            file
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        private val TAG = MainActivity::class.java.name
        private const val MAX_RECORD_AMPLITUDE = 32768.0
        private const val VOLUME_UPDATE_DURATION = 100L
        private val interpolator = OvershootInterpolator()
    }

}

@Composable
fun RegistrationScreen(service: RetrofitServices, navController: NavHostController){
    Scaffold { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center
        ){

            var login by remember { mutableStateOf(TextFieldValue("")) }
            var password by remember { mutableStateOf(TextFieldValue("")) }
            var textFieldSize by remember { mutableStateOf(Size.Zero) }
            val density = LocalDensity.current

            Column {

                OutlinedTextField(
                    login,
                    { newValue ->
                        if (newValue.text.length <= 21){
                            login = newValue
                        }
                    },
                    placeholder = {
                        Text("Login")
                    },
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    password,
                    { newValue ->
                        if (newValue.text.length <= 21){
                            password = newValue
                        }
                    },
                    placeholder = {
                        Text("Password")
                    },
                    maxLines = 1,
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            textFieldSize = coordinates.size.toSize()
                        }
                )

                Box(
                    modifier = Modifier
                        .height(with(density) { textFieldSize.height.toDp() })
                        .width(with(density) { textFieldSize.width.toDp() }),
                    contentAlignment = Alignment.CenterEnd
                ){
                    val context = LocalContext.current
                    val composableScope = rememberCoroutineScope()
                    Button(
                        {
                            composableScope.launch {
                                try {
                                    val response = service.addUser(login.text, password.text)
                                    val body = response.body()
                                    if (response.isSuccessful && body != null) {
                                        val sharedPreferences =
                                            context.getSharedPreferences("AppPrefs", MODE_PRIVATE)
                                        sharedPreferences.edit { putInt("userId", body) }
                                        Log.i("Response", body.toString())
                                    } else {
                                        Log.i("Response", "error " + response.code())
                                    }
                                } catch (exception: Exception) {
                                    Log.i("MyTag", exception.message.toString())
                                }
                            }
                            navController.navigate("NotificationsScreen"){
                                popUpTo("Registration") { inclusive = true }
                            }
                        }
                    ) {
                        Text("Зарегистрироваться")
                    }
                }
            }
        }
    }
}

@Composable
fun AuthorizationScreen(){
    Scaffold { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center
        ){

            var login by remember { mutableStateOf(TextFieldValue("")) }
            var password by remember { mutableStateOf(TextFieldValue("")) }
            var textFieldSize by remember { mutableStateOf(Size.Zero) }
            val density = LocalDensity.current

            Column {

                OutlinedTextField(
                    login,
                    { newValue ->
                        if (newValue.text.length <= 21){
                            login = newValue
                        }
                    },
                    placeholder = {
                        Text("Login")
                    },
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    password,
                    { newValue ->
                        if (newValue.text.length <= 21){
                            password = newValue
                        }
                    },
                    placeholder = {
                        Text("Password")
                    },
                    maxLines = 1,
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            textFieldSize = coordinates.size.toSize()
                        }
                )

                Box(
                    modifier = Modifier
                        .height(with(density) { textFieldSize.height.toDp() })
                        .width(with(density) { textFieldSize.width.toDp() }),
                    contentAlignment = Alignment.CenterEnd
                ){
                    Button(
                        {

                        }
                    ) {
                        Text("Войти")
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationScheduler() {
    val context = LocalContext.current

    Button(onClick = {
        val triggerTime = System.currentTimeMillis() + 10_000
        scheduleNotification(context, triggerTime, 1)
    }) {
        Text("Запланировать уведомление")
    }
}

@Composable
fun UserProfileScreen(){
    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding() + 10.dp,
                    bottom = paddingValues.calculateBottomPadding()
                ),
            contentAlignment = Alignment.TopCenter
        ){
            Text(
                "User",
                style = Typography.bodyMedium.copy(fontSize = 24.sp)
            )
        }
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center
        ){

            var tg by remember { mutableStateOf(TextFieldValue("")) }
            var textFieldSize by remember { mutableStateOf(Size.Zero) }
            val density = LocalDensity.current

            Column {

                OutlinedTextField(
                    tg,
                    { newValue ->
                        if (newValue.text.length <= 21){
                            tg = newValue
                        }
                    },
                    placeholder = {
                        Text("telegram: @sample")
                    },
                    maxLines = 1,
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            textFieldSize = coordinates.size.toSize()
                        }
                )

                Box(
                    modifier = Modifier
                        .height(with(density) { textFieldSize.height.toDp() })
                        .width(with(density) { textFieldSize.width.toDp() }),
                    contentAlignment = Alignment.CenterEnd
                ){
                    Button(
                        {

                        }
                    ) {
                        Text("Привязать")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navController: NavHostController){
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar({
                Text("Напоминания", style = Typography.titleMedium)
            },
                actions = {
                    FloatingActionButton({
                        navController.navigate("Profile")
                    }){
                        Icon(Icons.Default.Person, "Profile")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton({
                navController.navigate("CreateNotificationScreen")
            }){
                Icon(Icons.Default.Add, "Add Notification")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var value by remember { mutableStateOf(TextFieldValue("")) }
            val items by remember { mutableStateOf(NotificationManager.notifications) }
            OutlinedTextField(
                value,
                { newValue ->
                    value = newValue
                }
            )
            LazyColumn() {
                items.forEach {
                    item {
                        NotificationCard(it.date, it.text, navController, it.id)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(time: String, body: String, navController: NavHostController, id: String){
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp).clickable {
            navController.navigate("NotificationScreen/${id}")
        },
        shape = RectangleShape,
        border = BorderStroke(2.dp, Color.Black)
    ){
        Column(
            modifier = Modifier.padding(horizontal = 5.dp)
        ) {
            Text(time)
            Text(body)
        }
    }
}

@Composable
fun NotificationScreen(id: String, navController: NavHostController){
    var ntf = NotificationManager.search(id)
    val sharedPreferences = LocalContext.current.getSharedPreferences("AppPrefs", MODE_PRIVATE)
    if(ntf == null){
        ntf = Notification(
            userId = sharedPreferences.getInt("userId", 0),
            text = "",
            category = "",
            date = "",
            time = "",
            place = "",
            isCompleted = false,
            activationCondition = ""
        )
    }
    Scaffold(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 20.dp),
        floatingActionButton = {
            FloatingActionButton({
                navController.navigate("NotificationEditScreen/${id}")
            }){
                Icon(Icons.Default.Edit, "edit button")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ){
            item {
                Text("date: ${ntf.date}", style = Typography.bodyMedium.copy(fontSize = 20.sp))
                Spacer(modifier = Modifier.fillMaxWidth().height(10.dp))
            }
            item {
                Text("time: ${ntf.time}", style = Typography.bodyMedium.copy(fontSize = 20.sp))
                Spacer(modifier = Modifier.fillMaxWidth().height(10.dp))
            }
            item {
                Text("text: ${ntf.text}", style = Typography.bodyMedium.copy(fontSize = 20.sp))
                Spacer(modifier = Modifier.fillMaxWidth().height(10.dp))
            }
            item {
                Text("category: ${ntf.category}", style = Typography.bodyMedium.copy(fontSize = 20.sp))
                Spacer(modifier = Modifier.fillMaxWidth().height(10.dp))
            }
            item {
                Text("place: ${ntf.place}", style = Typography.bodyMedium.copy(fontSize = 20.sp))
                Spacer(modifier = Modifier.fillMaxWidth().height(10.dp))
            }
            item {
                Text("category: ${ntf.activationCondition}", style = Typography.bodyMedium.copy(fontSize = 20.sp))
                Spacer(modifier = Modifier.fillMaxWidth().height(10.dp))
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("is completed: ", style = Typography.bodyMedium.copy(fontSize = 20.sp))
                    Checkbox(
                        ntf.isCompleted,
                        {
                            ntf.isCompleted = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationEditScreen(
    id: String,
    navController: NavHostController,
    startEndRecord: () -> Unit,
    endRecord: () -> Unit
){
    val ntf = NotificationManager.search(id)
    val newNtf = ntf!!.copy()
    var date by remember { mutableStateOf(TextFieldValue(newNtf.date)) }
    var time by remember { mutableStateOf(TextFieldValue(newNtf.time)) }
    var text by remember { mutableStateOf(TextFieldValue(newNtf.text)) }
    var category by remember { mutableStateOf(TextFieldValue(newNtf.category)) }
    var place by remember { mutableStateOf(TextFieldValue(newNtf.place)) }
    var activationCondition by remember { mutableStateOf(TextFieldValue(newNtf.activationCondition)) }

    Scaffold(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 20.dp),
        floatingActionButton = {
            Box(modifier = Modifier.fillMaxSize()) {
                FloatingActionButton(
                    onClick = {
                        NotificationManager.replace(id, newNtf.copy(
                            date = date.text,
                            time = time.text,
                            text = text.text,
                            category = category.text,
                            place = place.text,
                            activationCondition = activationCondition.text
                        ))
                        endRecord()
                        navController.popBackStack()
                    },
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Icon(Icons.Default.Check, "Confirm button")
                }

                FloatingActionButton(
                    onClick = { startEndRecord() },
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Icon(Icons.Default.Call, "Voice redactor")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ){
            item {
                OutlinedTextField(
                    date,
                    { newValue ->
                        date = newValue
                    },
                    placeholder = {
                        Text("Date")
                    }
                )
                Spacer(modifier = Modifier.fillMaxWidth().height(10.dp))
            }
            item {
                OutlinedTextField(
                    time,
                    { newValue ->
                        time = newValue
                    },
                    placeholder = {
                        Text("Time")
                    }
                )
                Spacer(modifier = Modifier.fillMaxWidth().height(10.dp))
            }
            item {
                OutlinedTextField(
                    text,
                    { newValue ->
                        text = newValue
                    },
                    placeholder = {
                        Text("Text")
                    }
                )
                Spacer(modifier = Modifier.fillMaxWidth().height(10.dp))
            }
            item {
                OutlinedTextField(
                    category,
                    { newValue ->
                        category = newValue
                    },
                    placeholder = {
                        Text("Categories")
                    }
                )
                Spacer(modifier = Modifier.fillMaxWidth().height(10.dp))
            }
            item {
                OutlinedTextField(
                    place,
                    { newValue ->
                        place = newValue
                    },
                    placeholder = {
                        Text("Place")
                    }
                )
                Spacer(modifier = Modifier.fillMaxWidth().height(10.dp))
            }
            item {
                OutlinedTextField(
                    activationCondition,
                    { newValue ->
                        activationCondition = newValue
                    },
                    placeholder = {
                        Text("activation conditions")
                    }
                )
                Spacer(modifier = Modifier.fillMaxWidth().height(10.dp))
            }
        }
    }
}

@Composable
fun CreateNotificationScreen(
    navController: NavHostController,
    startEndRecord: () -> Unit,
    endRecord: () -> Boolean
){
    var date by remember { mutableStateOf(TextFieldValue("")) }
    var time by remember { mutableStateOf(TextFieldValue("")) }
    var text by remember { mutableStateOf(TextFieldValue("")) }
    var category by remember { mutableStateOf(TextFieldValue("")) }
    var place by remember { mutableStateOf(TextFieldValue("")) }
    var activationCondition by remember { mutableStateOf(TextFieldValue("")) }
    val sharedPreferences = LocalContext.current.getSharedPreferences("AppPrefs", MODE_PRIVATE)

    Scaffold(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 20.dp),
        floatingActionButton = {
            Box(modifier = Modifier.fillMaxSize()) {
                val context = LocalContext.current
                val composableScope = rememberCoroutineScope()
                FloatingActionButton(
                    onClick = {
                        if(endRecord()){

                        }else{
                            NotificationManager.add(Notification(
                                userId = sharedPreferences.getInt("userId", 0),
                                text = text.text,
                                category = category.text,
                                date = date.text,
                                time = time.text,
                                place = place.text,
                                isCompleted = false,
                                activationCondition = activationCondition.text
                            ))
                        }
                        composableScope.launch {
                            try {
                                val sharedPreferences =
                                    context.getSharedPreferences("AppPrefs", MODE_PRIVATE)
                                val userId = sharedPreferences.getInt("userId", -1)
                                Service.getService().addNotifications(userId, NotificationManager.notifications.last())
                            } catch (exception: Exception) {
                                Log.i("MyTag", exception.message.toString())
                            }
                        }
                        navController.popBackStack()
                    },
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Icon(Icons.Default.Check, "Confirm button")
                }

                FloatingActionButton(
                    onClick = { startEndRecord() },
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Icon(Icons.Default.Call, "Voice redactor")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ){
            item {
                OutlinedTextField(
                    date,
                    { newValue ->
                        date = newValue
                    },
                    placeholder = {
                        Text("Date")
                    }
                )
                Spacer(modifier = Modifier.fillMaxWidth().height(10.dp))
            }
            item {
                OutlinedTextField(
                    time,
                    { newValue ->
                        time = newValue
                    },
                    placeholder = {
                        Text("Time")
                    }
                )
                Spacer(modifier = Modifier.fillMaxWidth().height(10.dp))
            }
            item {
                OutlinedTextField(
                    text,
                    { newValue ->
                        text = newValue
                    },
                    placeholder = {
                        Text("Text")
                    }
                )
                Spacer(modifier = Modifier.fillMaxWidth().height(10.dp))
            }
            item {
                OutlinedTextField(
                    category,
                    { newValue ->
                        category = newValue
                    },
                    placeholder = {
                        Text("Categories")
                    }
                )
                Spacer(modifier = Modifier.fillMaxWidth().height(10.dp))
            }
            item {
                OutlinedTextField(
                    place,
                    { newValue ->
                        place = newValue
                    },
                    placeholder = {
                        Text("Place")
                    }
                )
                Spacer(modifier = Modifier.fillMaxWidth().height(10.dp))
            }
            item {
                OutlinedTextField(
                    activationCondition,
                    { newValue ->
                        activationCondition = newValue
                    },
                    placeholder = {
                        Text("activation conditions")
                    }
                )
                Spacer(modifier = Modifier.fillMaxWidth().height(10.dp))
            }
        }
    }
}