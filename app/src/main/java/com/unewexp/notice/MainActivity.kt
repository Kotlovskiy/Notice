package com.unewexp.notice

import android.Manifest
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.animation.OvershootInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.unewexp.notice.ui.theme.NoticeTheme
import com.unewexp.notice.ui.theme.Typography
import java.util.UUID

class MainActivity : ComponentActivity() {

    private val recordController = RecordController(this)
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            777,
        )

        val userId = UUID.randomUUID().toString()
        for (i in 1..30) {
            NotificationManager.add(
                Notification(
                    userId = userId,
                    text = "xdcgbkl;.m jgdrch",
                    category = "cattttttt",
                    date = "16.06.2025",
                    time = "23:33",
                    place = "gydgdshlfdhio",
                    isCompleted = false,
                    activationCondition = "asdfghjklkjgertyhbnmkkjhgfdrty"
                )
            )
        }

        enableEdgeToEdge()
        setContent {
            NoticeTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "NotificationsScreen") {
                    composable("NotificationsScreen") { NotificationsScreen(navController) }
                    composable("NotificationScreen/{id}") { backStackEntry ->
                        val notificationId = backStackEntry.arguments?.getString("id")
                        NotificationScreen(notificationId!!, navController)
                    }
                    composable("NotificationEditScreen/{id}") { backStackEntry ->
                        val notificationId = backStackEntry.arguments?.getString("id")
                        NotificationEditScreen(notificationId!!, navController)
                    }
                    composable("CreateNotificationScreen") { backStackEntry ->
                        CreateNotificationScreen(navController)
                    }
                }
            }
        }
    }

    private fun onButtonClicked() {
        if (recordController.isAudioRecording()) {
            recordController.stop()
            countDownTimer?.cancel()
            countDownTimer = null
        } else {
            recordController.start()
            countDownTimer = object : CountDownTimer(60_000, VOLUME_UPDATE_DURATION) {
                override fun onTick(p0: Long) {
                    val volume = recordController.getVolume()
                    Log.d(TAG, "Volume = $volume")
                    //handleVolume(volume)
                }

                override fun onFinish() {
                }
            }.apply {
                start()
            }
        }
    }

    /*
    private fun handleVolume(volume: Int) {
        val scale = min(8.0, volume / MAX_RECORD_AMPLITUDE + 1.0).toFloat()
        Log.d(TAG, "Scale = $scale")

        audioButton.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setInterpolator(interpolator)
            .duration = VOLUME_UPDATE_DURATION
    }

     */

    private companion object {
        private val TAG = MainActivity::class.java.name
        private const val MAX_RECORD_AMPLITUDE = 32768.0
        private const val VOLUME_UPDATE_DURATION = 100L
        private val interpolator = OvershootInterpolator()
    }

}

@Composable
fun TestBtn(
    onClick: () -> Unit
) {
    Button(
        onClick = { onClick() }
    ){}
}

@Composable
fun RegistrationScreen(){

}

@Composable
fun AuthorizationScreen(){

}

@Composable
fun UserProfileScreen(){

}

@Composable
fun NotificationsScreen(navController: NavHostController){
    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
    if(ntf == null){
        ntf = Notification(
            userId = "",
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
fun NotificationEditScreen(id: String, navController: NavHostController){
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
            FloatingActionButton({
                NotificationManager.replace(id, newNtf.copy(
                    date = date.text,
                    time = time.text,
                    text = text.text,
                    category = category.text,
                    place = place.text,
                    activationCondition = activationCondition.text
                ))
                navController.popBackStack()
            }){
                Icon(Icons.Default.Check, "edit button")
            }
        },
        floatingActionButtonPosition = FabPosition.End
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
fun CreateNotificationScreen(navController: NavHostController){
    var date by remember { mutableStateOf(TextFieldValue("")) }
    var time by remember { mutableStateOf(TextFieldValue("")) }
    var text by remember { mutableStateOf(TextFieldValue("")) }
    var category by remember { mutableStateOf(TextFieldValue("")) }
    var place by remember { mutableStateOf(TextFieldValue("")) }
    var activationCondition by remember { mutableStateOf(TextFieldValue("")) }

    Scaffold(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 20.dp),
        floatingActionButton = {
            FloatingActionButton({
                NotificationManager.add(Notification(
                    userId = UUID.randomUUID().toString(),
                    text = text.text,
                    category = category.text,
                    date = date.text,
                    time = time.text,
                    place = place.text,
                    isCompleted = false,
                    activationCondition = activationCondition.text
                ))
                navController.popBackStack()
            }){
                Icon(Icons.Default.Check, "Confirm button")
            }
        },
        floatingActionButtonPosition = FabPosition.End
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