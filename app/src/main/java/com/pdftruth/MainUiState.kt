package com.pdftruth

import android.graphics.Bitmap
import android.net.Uri

data class MainUiState(
    val selectedPdfUri: Uri? = null,
    val selectedPdfName: String? = null,
    val currentPageIndex: Int = 0,
    val pageCount: Int = 0,
    val currentPageBitmap: Bitmap? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)
