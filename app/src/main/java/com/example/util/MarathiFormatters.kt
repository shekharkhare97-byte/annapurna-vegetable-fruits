package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MarathiFormatters {
    private val marathiDigits = charArrayOf('०', '१', '२', '३', '४', '५', '६', '७', '८', '९')

    /**
     * Converts standard ASCII digits (0-9) into Marathi Devanagari digits (०-९).
     */
    fun toMarathiNumber(value: Int): String = toMarathiDigits(value.toString())

    fun toMarathiDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(marathiDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Normalizes user-entered phone digits (supporting both Marathi ०-९ and ASCII 0-9)
     * into clean ASCII digits for validation and storage.
     */
    fun normalizePhoneDigits(raw: String): String {
        val sb = StringBuilder()
        for (ch in raw) {
            when (ch) {
                in '0'..'9' -> sb.append(ch)
                in '०'..'९' -> sb.append(('0'.code + (ch.code - '०'.code)).toChar())
            }
        }
        return sb.toString().take(10)
    }

    fun isValidIndianMobile(raw: String): Boolean {
        val digits = normalizePhoneDigits(raw)
        return digits.length == 10 && digits[0] in '6'..'9'
    }

    fun formatTimestampMarathi(epochMillis: Long): String {
        val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
        return toMarathiDigits(formatter.format(Date(epochMillis)))
    }

    fun shareOrderReceipt(context: Context, receiptText: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "अन्नपूर्णा भाजीपाला आणि फळे - ऑर्डर पावती")
            putExtra(Intent.EXTRA_TEXT, receiptText)
        }
        context.startActivity(
            Intent.createChooser(shareIntent, "पावती शेअर करा").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun shareGeneratedBitmap(context: Context, bitmap: Bitmap, prompt: String) {
        val cacheDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
        val imageFile = File(cacheDir, "annapurna_studio_${System.currentTimeMillis()}.png")
        FileOutputStream(imageFile).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "अन्नपूर्णा AI फोटो स्टुडिओ: $prompt")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(shareIntent, "फोटो शेअर करा").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
