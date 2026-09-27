package com.example.ui.signature

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.Base64
import androidx.compose.ui.geometry.Offset
import java.io.ByteArrayOutputStream

object SignatureHelper {

    /**
     * Converts a list of stroke list (list of points) into a Base64 encoded PNG string.
     */
    fun createSignatureBitmapBase64(strokes: List<List<Offset>>, width: Int = 400, height: Int = 200): String {
        if (strokes.isEmpty()) return ""
        
        try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            // Draw background (solid white to avoid PDF transparency rendering issues)
            canvas.drawColor(Color.WHITE)
            
            // Setup paint
            val paint = Paint().apply {
                color = Color.BLACK
                isAntiAlias = true
                strokeWidth = 6f
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }
            
            // Draw each stroke
            for (stroke in strokes) {
                if (stroke.isEmpty()) continue
                val path = Path()
                val first = stroke.first()
                path.moveTo(first.x, first.y)
                for (i in 1 until stroke.size) {
                    val pt = stroke[i]
                    path.lineTo(pt.x, pt.y)
                }
                canvas.drawPath(path, paint)
            }
            
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val bytes = outputStream.toByteArray()
            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
    
    /**
     * Decodes Base64 to Android Bitmap for drawing.
     */
    fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
        if (base64Str.isEmpty()) return null
        return try {
            val cleaned = base64Str.replace("\n", "").replace("\r", "").trim()
            val decodedBytes = try {
                Base64.decode(cleaned, Base64.DEFAULT)
            } catch (e: Exception) {
                Base64.decode(cleaned, Base64.NO_WRAP)
            }
            android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
