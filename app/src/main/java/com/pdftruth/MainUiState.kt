package com.pdftruth

import android.graphics.Bitmap
import android.net.Uri
import com.pdftruth.storage.RecentDocument

data class MainUiState(
    val selectedPdfUri: Uri? = null,
    val selectedPdfName: String? = null,
    val currentPageIndex: Int = 0,
    val pageCount: Int = 0,
    val currentPageBitmap: Bitmap? = null,
    val recentDocuments: List<RecentDocument> = emptyList(),
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)
