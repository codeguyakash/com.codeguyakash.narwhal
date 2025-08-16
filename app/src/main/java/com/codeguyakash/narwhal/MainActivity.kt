package com.codeguyakash.narwhal

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var nfcReader: NFCReader
    private lateinit var logFolder: File
    private lateinit var logTextView: TextView
    private lateinit var scrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // System bars padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // UI refs
        logTextView = findViewById(R.id.logTextView)
        scrollView = findViewById(R.id.scrollView)

        // Select save folder based on Android version
        logFolder = getLogFolder()
        if (!logFolder.exists()) {
            if (logFolder.mkdirs()) {
                saveLog("✅ Log folder created: ${logFolder.absolutePath}")
            } else {
                saveLog("❌ Failed to create log folder: ${logFolder.absolutePath}")
            }
        } else {
            saveLog("ℹ️ Log folder already exists: ${logFolder.absolutePath}")
        }

        // Ask runtime permissions if needed
        if (!PermissionHelper.hasAllPermissions(this)) {
            PermissionHelper.requestAllPermissions(this)
        }

        // NFC reader with log callback
        nfcReader = NFCReader(this) { msg -> saveLog(msg) }
        nfcReader.initNFC()

        // 🔴 Button removed → scanning always ON when app is foreground
        saveLog("App started, ready to scan NFC tags...")
    }

    override fun onResume() {
        super.onResume()
        if (::nfcReader.isInitialized) {
            nfcReader.enableForegroundDispatch()
            saveLog("NFC foreground dispatch enabled")
        }
    }

    override fun onPause() {
        super.onPause()
        if (::nfcReader.isInitialized) {
            nfcReader.disableForegroundDispatch()
            saveLog("NFC foreground dispatch disabled")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (::nfcReader.isInitialized) {
            nfcReader.processIntent(intent)
        }
    }

    /** Get log folder depending on Android version */
    private fun getLogFolder(): File {
        val appName = getString(R.string.app_name)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ : Use Android/media so files are visible in file manager
            File(Environment.getExternalStorageDirectory(), "Android/media/$packageName/$appName")
        } else {
            // Older Android: Use app-specific external folder
            File(getExternalFilesDir(null), appName)
        }
    }

    /** Save to file AND append to UI */
    private fun saveLog(message: String) {
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val line = "$time - $message\n"

        // File name with date+time
        val fileName = "nfc_logs_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"

        // Write to file
        try {
            val logFile = File(logFolder, fileName)
            FileWriter(logFile, true).use { it.append(line) }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Show in UI
        runOnUiThread {
            logTextView.append(line)
            scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
        }
    }
}
