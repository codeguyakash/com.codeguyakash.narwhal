package com.codeguyakash.narwhal

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Parcelable
import android.widget.Toast

class NFCReader(
    private val activity: Activity,
    private val logCallback: (String) -> Unit
) {
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var filters: Array<IntentFilter>? = null

    fun initNFC() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        if (nfcAdapter == null) {
            val msg = "NFC not supported on this device."
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
            logCallback(msg)
            return
        }
        if (nfcAdapter?.isEnabled == false) {
            val msg = "NFC is disabled. Please enable it in Settings."
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
            logCallback(msg)
        }

        pendingIntent = PendingIntent.getActivity(
            activity, 0,
            Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )

        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        try { ndef.addDataType("*/*") } catch (_: IntentFilter.MalformedMimeTypeException) { }
        val tag = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        val tech = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        filters = arrayOf(ndef, tag, tech)

        logCallback("NFC initialized.")
    }

    fun enableForegroundDispatch() {
        nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, filters, null)
        logCallback("Foreground dispatch ENABLED.")
    }

    fun disableForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(activity)
        logCallback("Foreground dispatch DISABLED.")
    }

    fun processIntent(intent: Intent) {
        val action = intent.action ?: return
        logCallback("NFC intent received: $action")

        // Try to read NDEF messages
        @Suppress("DEPRECATION")
        val raw = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
                as? Array<Parcelable>

        if (raw != null && raw.isNotEmpty()) {
            raw.forEach { p ->
                val msg = p as NdefMessage
                msg.records.forEach { record ->
                    try {
                        val text = decodeTextRecord(record.payload)
                        Toast.makeText(activity, "NFC Text: $text", Toast.LENGTH_SHORT).show()
                        logCallback("NDEF Text: $text")
                    } catch (_: Exception) {
                        val data = String(record.payload)
                        logCallback("NDEF Raw: $data")
                    }
                }
            }
            return
        }

        // No NDEF? at least log tag id
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            val idHex = tag.id.joinToString("") { b -> "%02x".format(b) }
            Toast.makeText(activity, "NFC Tag detected: $idHex", Toast.LENGTH_SHORT).show()
            logCallback("Tag detected (no NDEF): $idHex")
        } else {
            logCallback("NFC Read Failed: No tag / no NDEF.")
        }
    }

    /** NDEF Text decoding: first byte status (lang len + encoding), then lang code, then text */
    private fun decodeTextRecord(payload: ByteArray): String {
        val status = payload[0].toInt()
        val langLen = status and 0x3F
        val isUtf16 = (status and 0x80) != 0
        val textBytes = payload.copyOfRange(1 + langLen, payload.size)
        return if (isUtf16) String(textBytes, Charsets.UTF_16) else String(textBytes, Charsets.UTF_8)
    }
}
