package com.consola.launcher.upload

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object QrCode {

    fun render(text: String, sizePx: Int = 400): ImageBitmap {
        // Margen chico: el QR va sobre una tarjeta blanca que ya le da aire.
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, mapOf(EncodeHintType.MARGIN to 1))
        val width = matrix.width
        val pixels = IntArray(width * matrix.height) { index ->
            if (matrix[index % width, index / width]) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(pixels, width, matrix.height, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
}
