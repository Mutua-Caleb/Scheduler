package com.scheduler.calls.overlay

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.scheduler.calls.R
import com.scheduler.calls.alarm.CallNotifications
import java.util.concurrent.Executors

class NotesOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var telephonyManager: TelephonyManager? = null
    private var phoneCallback: Any? = null
    private var sawCallActive = false
    private var eventId: Long = -1L
    private var contactDisplay: String = ""
    private var resultPromptShown = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val name = intent?.getStringExtra(EXTRA_NAME).orEmpty()
        val notes = intent?.getStringExtra(EXTRA_NOTES).orEmpty()
        eventId = intent?.getLongExtra(EXTRA_EVENT_ID, -1L) ?: -1L
        contactDisplay = name.ifBlank { getString(R.string.overlay_default_title) }

        startInForeground(name, notes)
        registerCallStateListener()

        if (Settings.canDrawOverlays(this)) {
            showOverlay(name, notes)
        }
        return START_STICKY
    }

    private fun startInForeground(name: String, notes: String) {
        CallNotifications.ensureChannels(this)
        val title = if (name.isBlank()) {
            getString(R.string.overlay_default_title)
        } else {
            getString(R.string.overlay_calling, name)
        }
        val body = if (notes.isBlank()) getString(R.string.overlay_default_body) else notes
        val notification: Notification = NotificationCompat.Builder(this, CallNotifications.CHANNEL_NOTES)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerCallStateListener() {
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        telephonyManager = tm
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val cb = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) = handleCallState(state)
                }
                phoneCallback = cb
                tm.registerTelephonyCallback(Executors.newSingleThreadExecutor(), cb)
            } else {
                @Suppress("DEPRECATION")
                val cb = object : PhoneStateListener() {
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) =
                        handleCallState(state)
                }
                phoneCallback = cb
                @Suppress("DEPRECATION")
                tm.listen(cb, PhoneStateListener.LISTEN_CALL_STATE)
            }
        } catch (_: SecurityException) {
            // READ_PHONE_STATE not granted; user must dismiss manually
        }
    }

    private fun handleCallState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK,
            TelephonyManager.CALL_STATE_RINGING -> sawCallActive = true
            TelephonyManager.CALL_STATE_IDLE -> if (sawCallActive) onCallEnded()
        }
    }

    private fun onCallEnded() {
        if (!resultPromptShown && eventId >= 0) {
            resultPromptShown = true
            CallNotifications.showResultPrompt(this, eventId, contactDisplay)
        }
        stopSelf()
    }

    private fun unregisterCallStateListener() {
        val tm = telephonyManager ?: return
        val cb = phoneCallback ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && cb is TelephonyCallback) {
                tm.unregisterTelephonyCallback(cb)
            } else if (cb is PhoneStateListener) {
                @Suppress("DEPRECATION")
                tm.listen(cb, PhoneStateListener.LISTEN_NONE)
            }
        } catch (_: Throwable) { /* best effort */ }
        telephonyManager = null
        phoneCallback = null
    }

    private fun showOverlay(name: String, notes: String) {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(Color.parseColor("#F2222222"))
            }
        }

        val title = TextView(this).apply {
            text = if (name.isBlank()) {
                getString(R.string.overlay_default_title)
            } else {
                getString(R.string.overlay_calling, name)
            }
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val body = TextView(this).apply {
            text = if (notes.isBlank()) getString(R.string.overlay_no_notes) else notes
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, dp(8), 0, dp(8))
        }

        val close = Button(this).apply {
            text = getString(R.string.overlay_dismiss)
            setOnClickListener { stopSelf() }
        }

        container.addView(title)
        container.addView(body)
        container.addView(close)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(80)
            width = (resources.displayMetrics.widthPixels * 0.9f).toInt()
        }

        attachDragHandler(container, params, wm)

        wm.addView(container, params)
        overlayView = container
    }

    private fun attachDragHandler(
        view: View,
        params: WindowManager.LayoutParams,
        wm: WindowManager
    ) {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchX).toInt()
                    params.y = initialY + (event.rawY - touchY).toInt()
                    wm.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        unregisterCallStateListener()
        overlayView?.let { runCatching { windowManager?.removeView(it) } }
        overlayView = null
        windowManager = null
        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val NOTIFICATION_ID = 4242
        private const val EXTRA_NAME = "name"
        private const val EXTRA_NOTES = "notes"
        private const val EXTRA_EVENT_ID = "event_id"

        fun start(context: Context, eventId: Long, name: String, notes: String) {
            val intent = Intent(context, NotesOverlayService::class.java).apply {
                putExtra(EXTRA_NAME, name)
                putExtra(EXTRA_NOTES, notes)
                putExtra(EXTRA_EVENT_ID, eventId)
            }
            context.startForegroundService(intent)
        }
    }
}
