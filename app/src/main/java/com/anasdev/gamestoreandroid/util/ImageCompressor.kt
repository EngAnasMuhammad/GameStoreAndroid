package com.anasdev.gamestoreandroid.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

object ImageCompressor {

    /**
     * Compresses the image at [uri] on [Dispatchers.IO]:
     * 1. Decodes bounds first (inJustDecodeBounds = true) to compute a power-of-2 inSampleSize.
     * 2. Decodes the downsampled bitmap without loading 4K pixels unnecessarily.
     * 3. Respects EXIF orientation and rotates if required.
     * 4. Proportionally scales down if dimensions still exceed [maxDimension] (never upscales).
     * 5. Compresses to JPEG at [quality], recycles intermediate bitmaps, and returns bytes.
     */
    suspend fun compressImage(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1024,
        quality: Int = 85
    ): ByteArray = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver

        // 1. Read bounds without allocating full bitmap pixels
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, boundsOptions)
        }

        val origWidth = boundsOptions.outWidth
        val origHeight = boundsOptions.outHeight
        if (origWidth <= 0 || origHeight <= 0) {
            throw IllegalArgumentException("Unable to decode image dimensions from URI: $uri")
        }

        // 2. Compute power-of-2 inSampleSize (downsample if either dimension exceeds maxDimension)
        var inSampleSize = 1
        if (origWidth > maxDimension || origHeight > maxDimension) {
            val halfWidth = origWidth / 2
            val halfHeight = origHeight / 2
            while (halfWidth / inSampleSize >= maxDimension || halfHeight / inSampleSize >= maxDimension) {
                inSampleSize *= 2
            }
        }

        // 3. Decode bitmap with inSampleSize
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decodedBitmap = contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, decodeOptions)
        } ?: throw IllegalStateException("Failed to decode bitmap from URI: $uri")

        // 4. Read EXIF orientation & apply transformation
        val orientation = try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        }

        val rotatedBitmap = if (!matrix.isIdentity) {
            val transformed = Bitmap.createBitmap(
                decodedBitmap,
                0,
                0,
                decodedBitmap.width,
                decodedBitmap.height,
                matrix,
                true
            )
            if (transformed != decodedBitmap) {
                decodedBitmap.recycle()
            }
            transformed
        } else {
            decodedBitmap
        }

        // 5. Proportionally scale down if width or height still exceeds maxDimension
        val currentWidth = rotatedBitmap.width
        val currentHeight = rotatedBitmap.height
        val scaledBitmap = if (currentWidth > maxDimension || currentHeight > maxDimension) {
            val scale = minOf(
                maxDimension.toFloat() / currentWidth,
                maxDimension.toFloat() / currentHeight
            )
            val targetWidth = (currentWidth * scale).roundToInt().coerceAtLeast(1)
            val targetHeight = (currentHeight * scale).roundToInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(rotatedBitmap, targetWidth, targetHeight, true)
            if (scaled != rotatedBitmap) {
                rotatedBitmap.recycle()
            }
            scaled
        } else {
            rotatedBitmap
        }

        // 6. Compress to JPEG at specified quality
        val outputStream = ByteArrayOutputStream()
        try {
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(0, 100), outputStream)
            outputStream.toByteArray()
        } finally {
            scaledBitmap.recycle()
            outputStream.close()
        }
    }
}
