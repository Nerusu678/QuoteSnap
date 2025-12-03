package uk.ac.tees.mad.quotesnap.utils

import android.content.Context
import android.graphics.*
import androidx.core.graphics.toColorInt
import uk.ac.tees.mad.quotesnap.data.local.SavedPoster
import java.io.File
import java.io.FileOutputStream

object PosterImageGenerator {


    // bitmap image from poster design

    fun generatePosterBitmap(poster: SavedPoster): Bitmap {
        val width = 1080
        val height = 1920

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // background
        val backgroundColor = try {
            poster.backgroundColor.toColorInt()
        } catch (e: Exception) {
            Color.BLACK
        }

        canvas.drawColor(backgroundColor)

        val textPaint = Paint().apply {
            color = try {
                poster.textColor.toColorInt()
            } catch (e: Exception) {
                android.graphics.Color.WHITE
            }
            textSize = poster.fontSize * 2 // Scale for higher resolution
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val authorPaint = Paint().apply {
            color = textPaint.color
            textSize = poster.fontSize * 1.5f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        // Draw quote text (with word wrapping)
        val quoteLines = wrapText(poster.quoteText, textPaint, width - 200f)
        var yPos = height / 2f - (quoteLines.size * textPaint.textSize / 2)

        quoteLines.forEach { line ->
            canvas.drawText(line, width / 2f, yPos, textPaint)
            yPos += textPaint.textSize + 20
        }

        // Draw author
        yPos += 60
        canvas.drawText("- ${poster.author}", width / 2f, yPos, authorPaint)

        return bitmap
    }


    // to wrap the text
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val bounds = Rect()
            paint.getTextBounds(testLine, 0, testLine.length, bounds)

            if (bounds.width() <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                currentLine = word
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }

    // save bitmap to file
    fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String): File {
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }
}