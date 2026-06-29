package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.security.MessageDigest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ChargeGuardianAppScreen()
            }
        }
    }
}

fun hashString(input: String): String {
    return MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray())
        .joinToString("") { "%02x".format(it) }
}

@Composable
fun ChargeGuardianAppScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as ChargeGuardianApp
    val viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory(app.container.dataStoreManager, app.container.batteryRepository))
    
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()
    val isAlarmRinging by viewModel.isAlarmRinging.collectAsState()
    val isProtectionEnabled by viewModel.isProtectionEnabled.collectAsState()
    
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (isAlarmRinging) "alarm" else if (isFirstLaunch) "onboarding" else "home",
        modifier = Modifier.background(DarkBackground)
    ) {
        composable("onboarding") {
            OnboardingScreen(viewModel, navController)
        }
        composable("home") {
            HomeScreen(viewModel, navController)
        }
        composable("settings") {
            SettingsScreen(viewModel, navController)
        }
        composable("alarm") {
            AlarmScreen(viewModel, navController)
        }
        composable("history") {
            HistoryScreen(viewModel, navController)
        }
    }

    LaunchedEffect(isAlarmRinging) {
        if (isAlarmRinging) {
            navController.navigate("alarm") {
                popUpTo(0)
            }
        } else if (navController.currentDestination?.route == "alarm") {
             navController.navigate("home") {
                 popUpTo(0)
             }
        }
    }
    
    LaunchedEffect(isProtectionEnabled) {
        if (isProtectionEnabled) {
             val serviceIntent = Intent(context, ChargeMonitorService::class.java)
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                 context.startForegroundService(serviceIntent)
             } else {
                 context.startService(serviceIntent)
             }
        }
    }
}

// -----------------------------------------------------------------------------------------
// ONBOARDING SCREEN
// -----------------------------------------------------------------------------------------
@Composable
fun OnboardingScreen(viewModel: AppViewModel, navController: NavController) {
    var step by remember { mutableStateOf(1) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var securityAnswer by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.Security,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Keep Your Device Safe",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Set up protection to prevent premature unplugging.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            if (step == 1) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Create PIN/Password") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm PIN/Password") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { if (password.isNotEmpty() && password == confirmPassword) step = 2 },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TextPrimary, contentColor = DarkBackground)
                ) {
                    Text("Next", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            } else if (step == 2) {
                Text("Security Question", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("What is your favorite pet's name?", color = TextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = securityAnswer,
                    onValueChange = { securityAnswer = it },
                    label = { Text("Answer") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        if (securityAnswer.isNotEmpty()) {
                            viewModel.setPassword(hashString(password))
                            viewModel.setSecurityData(0, hashString(securityAnswer.lowercase().trim()))
                            step = 3
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TextPrimary, contentColor = DarkBackground)
                ) {
                    Text("Next", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                val context = LocalContext.current
                val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { }
                
                LaunchedEffect(Unit) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                
                Text("Target Charge Level", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Select when you want the alarm to stop protecting. Default is 80%.", color = TextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                
                var target by remember { mutableStateOf(80f) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = target,
                        onValueChange = { target = it },
                        valueRange = 50f..100f,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("${target.toInt()}%", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        viewModel.setTargetPercentage(target.toInt())
                        viewModel.completeFirstLaunch()
                        navController.navigate("home") { popUpTo(0) }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TextPrimary, contentColor = DarkBackground)
                ) {
                    Text("Complete Setup", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// HOME SCREEN
// -----------------------------------------------------------------------------------------
@Composable
fun HomeScreen(viewModel: AppViewModel, navController: NavController) {
    val isProtectionEnabled by viewModel.isProtectionEnabled.collectAsState()
    val targetPercentage by viewModel.targetPercentage.collectAsState()
    
    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .windowInsetsPadding(WindowInsets.statusBars),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Bolt, contentDescription = null, tint = TextPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Charge Guardian", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                IconButton(onClick = { navController.navigate("settings") }) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = TextPrimary)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            // Status Circle
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(120.dp))
                    .background(
                        brush = Brush.radialGradient(
                            colors = if (isProtectionEnabled) listOf(Success.copy(alpha = 0.2f), DarkBackground)
                            else listOf(AccentGradientStart.copy(alpha = 0.2f), DarkBackground)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (isProtectionEnabled) Icons.Filled.Shield else Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = if (isProtectionEnabled) Success else TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (isProtectionEnabled) "Protected" else "Unprotected",
                        color = if (isProtectionEnabled) Success else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Info Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurface)
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Target Charge", color = TextSecondary)
                        Text("$targetPercentage%", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Battery History", color = TextSecondary)
                        IconButton(onClick = { navController.navigate("history") }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "History", tint = TextPrimary)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action Button
            Button(
                onClick = { viewModel.setProtectionEnabled(!isProtectionEnabled) },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isProtectionEnabled) DarkSurfaceVariant else TextPrimary,
                    contentColor = if (isProtectionEnabled) Danger else DarkBackground
                ),
                shape = RoundedCornerShape(32.dp)
            ) {
                Text(
                    if (isProtectionEnabled) "Disable Protection" else "Enable Protection",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// -----------------------------------------------------------------------------------------
// SETTINGS SCREEN
// -----------------------------------------------------------------------------------------
@Composable
fun SettingsScreen(viewModel: AppViewModel, navController: NavController) {
    val targetPercentage by viewModel.targetPercentage.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val autoStart by viewModel.autoStartOnReboot.collectAsState()
    val alarmSound by viewModel.alarmSound.collectAsState()
    
    val context = LocalContext.current
    
    fun testAlarmSound(soundId: Int) {
        if (soundId == 0) {
            val alarmUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
            val ringtone = android.media.RingtoneManager.getRingtone(context, alarmUri)
            ringtone.play()
            // Stop after a short duration
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ ringtone.stop() }, 2000)
        } else if (soundId == 1) {
            val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
            toneGen.startTone(android.media.ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1500)
        } else if (soundId == 2) {
            val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
            toneGen.startTone(android.media.ToneGenerator.TONE_CDMA_ABBR_ALERT, 1500)
        }
    }
    
    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 24.dp)
                    .windowInsetsPadding(WindowInsets.statusBars),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text("Settings", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Text("Target Charge Level", color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = targetPercentage.toFloat(),
                    onValueChange = { viewModel.setTargetPercentage(it.toInt()) },
                    valueRange = 50f..100f,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text("$targetPercentage%", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text("Alarm Sound", color = TextSecondary, modifier = Modifier.padding(bottom = 16.dp))
            val sounds = listOf("System Default", "Loud Siren", "Urgent Beeps")
            sounds.forEachIndexed { index, name ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { viewModel.setAlarmSound(index) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = (alarmSound == index),
                            onClick = { viewModel.setAlarmSound(index) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(name, color = TextPrimary)
                    }
                    IconButton(onClick = { testAlarmSound(index) }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Test Sound", tint = TextPrimary)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vibration", color = TextPrimary, fontSize = 18.sp)
                Switch(checked = vibrationEnabled, onCheckedChange = { viewModel.setVibration(it) })
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Auto-start on Reboot", color = TextPrimary, fontSize = 18.sp)
                Switch(checked = autoStart, onCheckedChange = { viewModel.setAutoStart(it) })
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// ALARM SCREEN
// -----------------------------------------------------------------------------------------
@Composable
fun AlarmScreen(viewModel: AppViewModel, navController: NavController) {
    val passwordHash by viewModel.passwordHash.collectAsState()
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    
    Scaffold(
        containerColor = Danger,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(96.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text("UNPLUGGED!", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Enter PIN to stop alarm", color = TextPrimary, fontSize = 18.sp)
            
            Spacer(modifier = Modifier.height(48.dp))
            
            OutlinedTextField(
                value = input,
                onValueChange = { 
                    input = it
                    error = false
                },
                isError = error,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { 
                    if (hashString(input) == passwordHash) {
                        viewModel.setAlarmRinging(false)
                        viewModel.setProtectionEnabled(false) // Disable protection after alarm
                    } else {
                        error = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TextPrimary, contentColor = Danger),
                shape = RoundedCornerShape(32.dp)
            ) {
                Text("DISMISS", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// HISTORY SCREEN
// -----------------------------------------------------------------------------------------
@Composable
fun HistoryScreen(viewModel: AppViewModel, navController: NavController) {
    val history by viewModel.batteryHistory.collectAsState()
    
    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 24.dp)
                    .windowInsetsPadding(WindowInsets.statusBars),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text("Battery History", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }
    ) { padding ->
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No history yet.", color = TextSecondary)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) {
                
                // Simple Compose Canvas Chart for History
                Box(modifier = Modifier.fillMaxWidth().height(200.dp).padding(vertical = 16.dp)) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxPercent = 100f
                        val minPercent = 0f
                        val graphWidth = size.width
                        val graphHeight = size.height
                        
                        // draw grid
                        val gridLines = 5
                        for (i in 0..gridLines) {
                            val y = graphHeight * (i.toFloat() / gridLines)
                            drawLine(
                                color = DarkSurfaceVariant,
                                start = androidx.compose.ui.geometry.Offset(0f, y),
                                end = androidx.compose.ui.geometry.Offset(graphWidth, y),
                                strokeWidth = 2f
                            )
                        }
                        
                        if (history.size > 1) {
                            val path = androidx.compose.ui.graphics.Path()
                            val sortedHistory = history.sortedBy { it.timestamp }
                            
                            val firstTime = sortedHistory.first().timestamp
                            val lastTime = sortedHistory.last().timestamp
                            val timeSpan = (lastTime - firstTime).coerceAtLeast(1L)
                            
                            sortedHistory.forEachIndexed { index, item ->
                                val x = graphWidth * ((item.timestamp - firstTime).toFloat() / timeSpan.toFloat())
                                val y = graphHeight - (graphHeight * (item.percentage / 100f))
                                
                                if (index == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }
                            
                            drawPath(
                                path = path,
                                color = Success,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 6f,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                )
                            )
                            
                            // Draw dots for events
                            sortedHistory.forEach { item ->
                                val x = graphWidth * ((item.timestamp - firstTime).toFloat() / timeSpan.toFloat())
                                val y = graphHeight - (graphHeight * (item.percentage / 100f))
                                
                                val dotColor = when (item.status) {
                                    "ALARM_TRIGGERED" -> Danger
                                    "POWER_OUTAGE" -> AccentGradientStart
                                    else -> Success
                                }
                                
                                drawCircle(
                                    color = dotColor,
                                    radius = 12f,
                                    center = androidx.compose.ui.geometry.Offset(x, y)
                                )
                            }
                        }
                    }
                }
                
                Text("Recent Events", color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(history.sortedByDescending { it.timestamp }) { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurface)
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    val statusText = when (item.status) {
                                        "POWER_OUTAGE" -> "Power Outage (No Alarm)"
                                        "ALARM_TRIGGERED" -> "Alarm Triggered!"
                                        "PLUGGED_IN" -> "Plugged In"
                                        "UNPLUGGED" -> "Unplugged"
                                        else -> item.status
                                    }
                                    val statusColor = when (item.status) {
                                        "ALARM_TRIGGERED" -> Danger
                                        "POWER_OUTAGE" -> AccentGradientStart
                                        else -> TextPrimary
                                    }
                                    Text(statusText, color = statusColor, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val time = java.text.SimpleDateFormat("MMM dd, HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(item.timestamp))
                                    Text(time, color = TextSecondary, fontSize = 12.sp)
                                }
                                Text("${item.percentage}%", color = Success, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
