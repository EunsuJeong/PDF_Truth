package com.pdftruth.viewer.engine

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.IOException

class PdfRendererEngine(
    private val openFileDescriptor: (Uri) -> ParcelFileDescriptor?
) {
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null

    @Throws(IOException::class, IllegalArgumentException::class)
    fun open(uri: Uri) {
        close()

        val pfd = openFileDescriptor(uri) ?: throw IOException("PDF 파일을 열 수 없습니다.")
        val pdfRenderer = try {
            PdfRenderer(pfd)
        } catch (e: Exception) {
            pfd.close()
            throw e
        }

        if (pdfRenderer.pageCount <= 0) {
            pdfRenderer.close()
            pfd.close()
            throw IllegalArgumentException("페이지 수가 0입니다.")
        }

        parcelFileDescriptor = pfd
        renderer = pdfRenderer
    }

    fun getPageCount(): Int {
        return renderer?.pageCount ?: 0
    }

    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    fun renderPage(pageIndex: Int, targetWidth: Int, targetHeight: Int): Bitmap {
        val pdfRenderer = renderer ?: throw IllegalStateException("PDF가 열려 있지 않습니다.")

        if (pageIndex < 0 || pageIndex >= pdfRenderer.pageCount) {
            throw IllegalArgumentException("유효하지 않은 pageIndex 입니다.")
        }

        val page = pdfRenderer.openPage(pageIndex)
        try {
            val sourceWidth = page.width.coerceAtLeast(1)
            val sourceHeight = page.height.coerceAtLeast(1)

            val renderWidth = if (targetWidth > 0) targetWidth else sourceWidth
            val renderHeight = when {
                targetHeight > 0 -> targetHeight
                targetWidth > 0 -> ((renderWidth.toFloat() / sourceWidth) * sourceHeight).toInt()
                    .coerceAtLeast(1)

                else -> sourceHeight
            }

            val bitmap = Bitmap.createBitmap(
                renderWidth.coerceAtLeast(1),
                renderHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        } finally {
            page.close()
        }
    }

    fun close() {
        runCatching { renderer?.close() }
        renderer = null

        runCatching { parcelFileDescriptor?.close() }
        parcelFileDescriptor = null
    }
}
