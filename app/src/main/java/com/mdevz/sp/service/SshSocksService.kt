package com.mdevz.sp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.Context
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import android.content.pm.ServiceInfo
import com.mdevz.sp.core.config.ProfileUriCodec
import com.mdevz.sp.core.model.TunnelState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Runs the SSH transport as a local SOCKS5 upstream. It intentionally does not
 * create a VpnService; Xray's existing TProxyService owns the sole TUN interface.
 */
class SshSocksService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var runtime: TunnelRuntime

    override fun onCreate() {
        super.onCreate()
        runtime = TunnelRuntime.get(this)
        ensureChannel()
        val notification = notification("Preparing SSH upstream")
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                scope.launch {
                    runCatching { runtime.engine.disconnect() }
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf(startId)
                }
            }
            ACTION_START -> {
                val uri = intent.getStringExtra(EXTRA_PROFILE_URI)
                if (uri.isNullOrBlank()) {
                    stopSelf(startId)
                    return START_NOT_STICKY
                }
                scope.launch {
                    try {
                        val profile = ProfileUriCodec.decode(uri)
                        runCatching { runtime.engine.disconnect() }
                        delay(250)
                        runtime.engine.connect(profile, vpnGeneration = 0L)
                        val result = withTimeout(60_000) {
                            runtime.engine.state.first {
                                it == TunnelState.CONNECTED || it == TunnelState.ERROR
                            }
                        }
                        if (result != TunnelState.CONNECTED) {
                            throw IllegalStateException(runtime.engine.failure.value?.message ?: "SSH connection failed")
                        }
                        updateNotification("SSH SOCKS5 upstream connected")
                        sendStatus(ACTION_CONNECTED)
                    } catch (failure: Throwable) {
                        runCatching { runtime.engine.disconnect() }
                        updateNotification("SSH upstream failed: ${failure.message ?: "unknown error"}")
                        sendStatus(ACTION_FAILED, failure.message)
                        stopSelf(startId)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun sendStatus(action: String, message: String? = null) {
        sendBroadcast(Intent(action).setPackage(packageName).putExtra(EXTRA_ERROR, message))
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL_ID, "SSH upstream", NotificationManager.IMPORTANCE_LOW
            ))
        }
    }

    private fun notification(message: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("SimpleXray · SSH")
            .setContentText(message)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    private fun updateNotification(message: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification(message))
    }

    override fun onDestroy() {
        runCatching { runBlocking(Dispatchers.IO) { runtime.engine.disconnect() } }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.simplexray.an.ssh.START_SOCKS"
        const val ACTION_STOP = "com.simplexray.an.ssh.STOP_SOCKS"
        const val EXTRA_PROFILE_URI = "ssh_profile_uri"
        const val EXTRA_ERROR = "ssh_error"
        const val ACTION_CONNECTED = "com.simplexray.an.ssh.CONNECTED"
        const val ACTION_FAILED = "com.simplexray.an.ssh.FAILED"
        private const val CHANNEL_ID = "simplexray_ssh_upstream"
        private const val NOTIFICATION_ID = 7312
    }
}
