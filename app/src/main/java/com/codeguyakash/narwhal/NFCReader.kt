package com.codeguyakash.narwhal

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
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
            val msg = "NFC not supported on this device"
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
            logCallback(msg)
            return
        }

        if (!nfcAdapter!!.isEnabled) {
            val msg = "Please enable NFC in settings"
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
            logCallback(msg)
        }

        pendingIntent = PendingIntent.getActivity(
            activity, 0,
            Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )

        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        try {
            ndef.addDataType("*/*")
        } catch (e: IntentFilter.MalformedMimeTypeException) {
            logCallback("Failed to add MIME type: ${e.message}")
        }
        filters = arrayOf(ndef)
    }

    fun enableForegroundDispatch() {
        nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, filters, null)
    }

    fun disableForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(activity)
    }

    fun processIntent(intent: Intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {

            val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (rawMsgs != null) {
                for (raw in rawMsgs) {
                    val msg = raw as android.nfc.NdefMessage
                    for (record in msg.records) {
                        val payload = String(record.payload)
                        Toast.makeText(activity, "NFC Data: $payload", Toast.LENGTH_LONG).show()
                        logCallback("NFC Data: $payload")
                    }
                }
            } else {
                val tag: Tag? = intent.getParcelableExtra<Parcelable>(NfcAdapter.EXTRA_TAG) as? Tag
                tag?.let {
                    val idHex = it.id.joinToString("") { b -> "%02x".format(b) }
                    Toast.makeText(activity, "NFC Tag detected: $idHex", Toast.LENGTH_LONG).show()
                    logCallback("NFC Tag detected: $idHex")
                }
            }
        }
    }
}
