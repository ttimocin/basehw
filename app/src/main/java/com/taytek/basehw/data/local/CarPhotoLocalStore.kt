package com.taytek.basehw.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CarPhotoLocalStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val MAX_DIMENSION = 600
        private const val TARGET_BYTES = 100 * 1024 // ~100 KB target per image
        private const val MIN_QUALITY = 32
        private const val START_QUALITY = 72
        private const val MIN_DIMENSION = 420
    }

    private val photosDir: File by lazy {
        File(context.filesDir, "car_photos").apply { mkdirs() }
    }

    fun persistCompressed(localUriString: String, carIdHint: Long? = null, suffix: String? = null): String? {
        if (localUriString.startsWith("http://") || localUriString.startsWith("https://")) {
            return localUriString
        }

        val sourceUri = Uri.parse(localUriString)

        return runCatching {
            val bitmap = decodeSampledBitmap(sourceUri, maxDimension = MAX_DIMENSION) ?: return null
            val baseName = "car_${carIdHint ?: System.currentTimeMillis()}"
            val fileName = if (suffix != null) "${baseName}_$suffix.jpg" else "$baseName.jpg"
            val outFile = File(photosDir, fileName)
            val jpegBytes = compressToTarget(bitmap)
            FileOutputStream(outFile).use { output -> output.write(jpegBytes) }
            bitmap.recycle()
            Uri.fromFile(outFile).toString()
        }.onFailure {
            Log.e("CarPhotoLocalStore", "Failed to persist compressed photo", it)
        }.getOrNull()
    }

    private fun decodeSampledBitmap(uri: Uri, maxDimension: Int): Bitmap? {
        if (uri.scheme == "file") {
            val path = uri.path ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val decode = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds, maxDimension)
                inJustDecodeBounds = false
            }
            return BitmapFactory.decodeFile(path, decode)
        }

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        options.inSampleSize = calculateInSampleSize(options, maxDimension)
        options.inJustDecodeBounds = false

        return openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }

    private fun openInputStream(uri: Uri): InputStream? {
        return if (uri.scheme == "file") {
            val path = uri.path ?: return null
            runCatching { FileInputStream(File(path)) }
                .onFailure { Log.e("CarPhotoLocalStore", "Failed to open file URI stream", it) }
                .getOrNull()
        } else {
            context.contentResolver.openInputStream(uri)
        }
    }

    private fun compressToTarget(bitmap: Bitmap): ByteArray {
        var working = bitmap
        var ownsWorking = false
        var smallest = encodeJpeg(working, START_QUALITY)

        repeat(6) {
            var quality = START_QUALITY
            var encoded = encodeJpeg(working, quality)

            while (encoded.size > TARGET_BYTES && quality > MIN_QUALITY) {
                quality -= 6
                encoded = encodeJpeg(working, quality)
            }

            if (encoded.size < smallest.size) {
                smallest = encoded
            }

            if (encoded.size <= TARGET_BYTES) {
                if (ownsWorking) working.recycle()
                return encoded
            }

            val maxSide = maxOf(working.width, working.height)
            if (maxSide <= MIN_DIMENSION) {
                if (ownsWorking) working.recycle()
                return smallest
            }

            val scale = 0.82f
            val targetW = (working.width * scale).toInt().coerceAtLeast(MIN_DIMENSION)
            val targetH = (working.height * scale).toInt().coerceAtLeast(MIN_DIMENSION)
            val scaled = Bitmap.createScaledBitmap(working, targetW, targetH, true)

            if (ownsWorking) working.recycle()
            working = scaled
            ownsWorking = true
        }

        if (ownsWorking) working.recycle()
        return smallest
    }

    private fun encodeJpeg(bitmap: Bitmap, quality: Int): ByteArray {
        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            stream.toByteArray()
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, maxDimension: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > maxDimension || width > maxDimension) {
            var halfHeight = height / 2
            var halfWidth = width / 2
            while (halfHeight / inSampleSize >= maxDimension && halfWidth / inSampleSize >= maxDimension) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }
}
