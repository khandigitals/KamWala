package com.example.util

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.util.Base64
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

object PlatformHelpers {
    fun formatPkr(amount: Int): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        return "Rs. ${formatter.format(amount)}"
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }

    fun normalizeWhatsAppNumber(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.startsWith("03") && digits.length >= 10 -> "92${digits.drop(1)}"
            digits.startsWith("92") -> digits
            digits.isEmpty() -> "923001234567"
            else -> digits
        }
    }

    fun dialPhone(context: Context, rawPhone: String) {
        val cleaned = rawPhone.filter { it.isDigit() || it == '+' }.ifBlank { "03001234567" }
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned"))
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            copyToClipboard(context, "Phone", cleaned)
        }
    }

    fun openWhatsApp(context: Context, rawPhone: String, message: String) {
        val normalized = normalizeWhatsAppNumber(rawPhone)
        val encoded = URLEncoder.encode(message, "UTF-8")
        val uri = Uri.parse("https://wa.me/$normalized?text=$encoded")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            shareText(context, message)
        }
    }

    fun playAlertTone() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
        } catch (_: Exception) {
            // Audio device unavailable in headless tests
        }
    }

    @SuppressLint("MissingPermission")
    fun captureApproximateLocation(
        context: Context,
        onResult: (String?) -> Unit
    ) {
        try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val lat = String.format(Locale.US, "%.4f", location.latitude)
                        val lon = String.format(Locale.US, "%.4f", location.longitude)
                        onResult("$lat, $lon")
                    } else {
                        client.lastLocation
                            .addOnSuccessListener { last ->
                                if (last != null) {
                                    val lat = String.format(Locale.US, "%.4f", last.latitude)
                                    val lon = String.format(Locale.US, "%.4f", last.longitude)
                                    onResult("$lat, $lon")
                                } else {
                                    onResult(null)
                                }
                            }
                            .addOnFailureListener { onResult(null) }
                    }
                }
                .addOnFailureListener { onResult(null) }
        } catch (_: Exception) {
            onResult(null)
        }
    }

    fun encodeCompressedPhotoDataUri(
        context: Context,
        uri: Uri,
        maxDim: Int = 420,
        initialQuality: Int = 60,
        maxChars: Int = 48000
    ): String {
        return try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            var sampleSize = 1
            val longestRaw = maxOf(bounds.outWidth, bounds.outHeight)
            if (longestRaw > maxDim * 2) {
                while (longestRaw / (sampleSize * 2) >= maxDim) {
                    sampleSize *= 2
                }
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return uri.toString().take(1000)

            val scaled = try {
                val maxSide = maxOf(decoded.width, decoded.height)
                if (maxSide > maxDim && maxSide > 0) {
                    val ratio = maxDim.toFloat() / maxSide.toFloat()
                    val w = (decoded.width * ratio).toInt().coerceAtLeast(1)
                    val h = (decoded.height * ratio).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(decoded, w, h, true)
                } else {
                    decoded
                }
            } finally {
                // recycled below if scaled != decoded
            }

            try {
                val qualities = listOf(initialQuality, 48, 36, 25).distinct()
                for (q in qualities) {
                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, q, out)
                    val candidate = "data:image/jpeg;base64," +
                        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                    if (candidate.length <= maxChars) {
                        return candidate
                    }
                }
                val finalOut = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 20, finalOut)
                val lastCandidate = "data:image/jpeg;base64," +
                    Base64.encodeToString(finalOut.toByteArray(), Base64.NO_WRAP)
                if (lastCandidate.length <= 65000) lastCandidate else uri.toString().take(1000)
            } finally {
                if (scaled !== decoded && !scaled.isRecycled) scaled.recycle()
                if (!decoded.isRecycled) decoded.recycle()
            }
        } catch (_: Exception) {
            uri.toString().take(1000)
        }
    }
}

class VoiceNoteHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    var currentFilePath: String? = null
        private set

    fun startRecording(): String? {
        return try {
            stopRecording()
            val outFile = File(context.cacheDir, "kaamwala_voice_${System.currentTimeMillis()}.m4a")
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            r.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            recorder = r
            currentFilePath = outFile.absolutePath
            outFile.absolutePath
        } catch (_: Exception) {
            recorder = null
            null
        }
    }

    fun stopRecording(): String? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            currentFilePath
        } catch (_: Exception) {
            recorder?.release()
            recorder = null
            null
        }
    }

    fun playFile(path: String, onComplete: () -> Unit = {}) {
        try {
            stopPlayback()
            val p = MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener {
                    onComplete()
                    stopPlayback()
                }
                prepare()
                start()
            }
            player = p
        } catch (_: Exception) {
            onComplete()
        }
    }

    fun stopPlayback() {
        try {
            player?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {
        } finally {
            player = null
        }
    }

    fun release() {
        stopRecording()
        stopPlayback()
    }
}
