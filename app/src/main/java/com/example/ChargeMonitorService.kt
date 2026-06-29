package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.DataStoreManager
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ChargeMonitorService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var dataStore: DataStoreManager
    private lateinit var repository: com.example.data.BatteryRepository
    
    private var isProtectionEnabled = false
    private var targetPercentage = 80
    private var vibrationEnabled = true
    private var isAlarmRinging = false
    private var currentAlarmSound = 0
    
    private var mediaPlayer: MediaPlayer? = null
    private var toneGenerator: ToneGenerator? = null
    private var isPlayingTone = false
    private var vibrator: Vibrator? = null
    
    private var currentBatteryLevel = -1
    private var isPluggedIn = false
    
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastMovementTime = 0L

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]
                val acceleration = sqrt((x*x + y*y + z*z).toDouble())
                if (acceleration > 10.5) { // Threshold for movement
                    lastMovementTime = System.currentTimeMillis()
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    isPluggedIn = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                    if (level != -1 && scale != -1) {
                        currentBatteryLevel = (level * 100) / scale.toFloat().toInt()
                    }
                    checkAlarmCondition()
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    isPluggedIn = true
                    checkAlarmCondition()
                    serviceScope.launch {
                        repository.insert(currentBatteryLevel, "PLUGGED_IN")
                    }
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    isPluggedIn = false
                    checkAlarmCondition()
                    serviceScope.launch {
                        repository.insert(currentBatteryLevel, "UNPLUGGED")
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val container = (applicationContext as ChargeGuardianApp).container
        dataStore = container.dataStoreManager
        repository = container.batteryRepository
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager?.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        createNotificationChannel()
        startForeground(1, createNotification(), if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)

        // Observe settings
        combine(
            dataStore.isProtectionEnabled,
            dataStore.targetPercentage,
            dataStore.vibrationEnabled,
            dataStore.isAlarmRinging,
            dataStore.alarmSound
        ) { enabled, target, vibrate, ringing, sound ->
            isProtectionEnabled = enabled
            targetPercentage = target
            vibrationEnabled = vibrate
            currentAlarmSound = sound
            
            if (isAlarmRinging != ringing) {
                isAlarmRinging = ringing
                if (!ringing) {
                    stopAlarm()
                } else if (!isMediaPlayerPlaying()) {
                    playAlarm()
                }
            }
        }.launchIn(serviceScope)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(batteryReceiver, filter)
    }

    private fun isMediaPlayerPlaying() = mediaPlayer?.isPlaying == true

    private fun checkAlarmCondition() {
        if (!isProtectionEnabled) return

        if (!isPluggedIn && currentBatteryLevel < targetPercentage && currentBatteryLevel != -1) {
            // Unplugged before target
            val timeSinceMovement = System.currentTimeMillis() - lastMovementTime
            val wasMoved = timeSinceMovement < 5000 // 5 seconds window
            
            if (wasMoved) {
                if (!isAlarmRinging) {
                    serviceScope.launch {
                        dataStore.setAlarmRinging(true)
                        repository.insert(currentBatteryLevel, "ALARM_TRIGGERED")
                    }
                }
            } else {
                // Power outage detected (no movement), do not trigger alarm
                serviceScope.launch {
                    repository.insert(currentBatteryLevel, "POWER_OUTAGE")
                }
            }
        } else if (isPluggedIn || currentBatteryLevel >= targetPercentage) {
            // Plugged back in, or reached target
            if (isAlarmRinging) {
                serviceScope.launch {
                    dataStore.setAlarmRinging(false)
                }
            }
        }
    }

    private fun playAlarm() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                0
            )

            if (currentAlarmSound == 0) {
                // Default System Alarm
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

                mediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, alarmUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
            } else if (currentAlarmSound == 1) {
                // Loud Siren (ToneGenerator)
                isPlayingTone = true
                toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                serviceScope.launch {
                    while (isPlayingTone) {
                        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 500)
                        delay(500)
                        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_PBX_L, 500)
                        delay(500)
                    }
                }
            } else if (currentAlarmSound == 2) {
                // Urgent Beeps (ToneGenerator)
                isPlayingTone = true
                toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                serviceScope.launch {
                    while (isPlayingTone) {
                        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 200)
                        delay(300)
                        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 200)
                        delay(600)
                    }
                }
            }
            
            if (vibrationEnabled) {
                val pattern = longArrayOf(0, 500, 500)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            }

            // Bring app to foreground
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(launchIntent)
            
        } catch (e: Exception) {
            Log.e("ChargeMonitorService", "Error playing alarm", e)
        }
    }

    private fun stopAlarm() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        
        isPlayingTone = false
        toneGenerator?.release()
        toneGenerator = null
        
        vibrator?.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
        sensorManager?.unregisterListener(sensorEventListener)
        stopAlarm()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "charge_monitor",
                "Charge Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors charging state to protect your device"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        return NotificationCompat.Builder(this, "charge_monitor")
            .setContentTitle("Charge Guardian is Active")
            .setContentText("Monitoring battery state...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()
    }
}
