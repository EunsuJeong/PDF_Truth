package com.pdftruth

import android.app.Application
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class MainUiState(
    val selectedPdfUri: Uri? = null,
    val selectedPdfName: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    fun onPdfSelected(uri: Uri?) {
        if (uri == null) {
            return
        }

        val fileName = extractDisplayName(uri)

        _uiState.update {
            it.copy(
                selectedPdfUri = uri,
                selectedPdfName = fileName,
                errorMessage = null,
                isLoading = false
            )
        }
    }

    fun setError(message: String) {
        _uiState.update {
            it.copy(errorMessage = message, isLoading = false)
        }
    }

    fun clearError() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }

    private fun extractDisplayName(uri: Uri): String {
        val resolver = getApplication<Application>().contentResolver
        var cursor: Cursor? = null

        return try {
            cursor = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (columnIndex >= 0) {
                    cursor.getString(columnIndex) ?: fallbackName(uri)
                } else {
                    fallbackName(uri)
                }
            } else {
                fallbackName(uri)
            }
        } catch (_: Exception) {
            fallbackName(uri)
        } finally {
            cursor?.close()
        }
    }

    private fun fallbackName(uri: Uri): String {
        val segment = uri.lastPathSegment?.substringAfterLast('/')?.trim().orEmpty()
        return if (segment.isNotBlank()) segment else "선택된 PDF"
    }
}
