package com.codeguyakash.narwhal

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Handle padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Create log folder with app name
        logFolder = File(getExternalFilesDir(null), getString(R.string.app_name))
        if (!logFolder.exists()) {
            logFolder.mkdirs()
        }

        // Request permissions
        if (!PermissionHelper.hasAllPermissions(this)) {
            PermissionHelper.requestAllPermissions(this)
        }

        // Initialize NFC Reader with callback
        nfcReader = NFCReader(this) { logMessage ->
            saveLog(logMessage)
        }
        nfcReader.initNFC()

        // Button click → Start NFC scanning
        findViewById<Button>(R.id.myButton).setOnClickListener {
            val msg = "Scanning for NFC tags..."
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            saveLog(msg)
            nfcReader.enableForegroundDispatch()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::nfcReader.isInitialized) {
            nfcReader.enableForegroundDispatch()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::nfcReader.isInitialized) {
            nfcReader.disableForegroundDispatch()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (::nfcReader.isInitialized) {
            nfcReader.processIntent(intent)
        }
    }

    private fun saveLog(message: String) {
        try {
            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val logFile = File(logFolder, "nfc_logs.txt")
            FileWriter(logFile, true).use { writer ->
                writer.append("$timeStamp - $message\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
