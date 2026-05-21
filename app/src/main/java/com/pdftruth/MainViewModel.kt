package com.pdftruth

import android.app.Application
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pdftruth.viewer.engine.PdfRendererEngine
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val selectedPdfUri: Uri? = null,
    val selectedPdfName: String? = null,
    val currentPageIndex: Int = 0,
    val pageCount: Int = 0,
    val currentPageBitmap: Bitmap? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val targetRenderWidth = 1080

    private val pdfRendererEngine = PdfRendererEngine { uri ->
        getApplication<Application>().contentResolver.openFileDescriptor(uri, "r")
    }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    fun openAndRenderFirstPage(uri: Uri?) {
        if (uri == null) {
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val fileName = extractDisplayName(uri)

            try {
                pdfRendererEngine.close()
                pdfRendererEngine.open(uri)

                val pageCount = pdfRendererEngine.getPageCount()
                if (pageCount <= 0) {
                    throw IllegalArgumentException("페이지 수가 0입니다.")
                }

                _uiState.update {
                    it.copy(
                        selectedPdfUri = uri,
                        selectedPdfName = fileName,
                        currentPageIndex = 0,
                        pageCount = pageCount,
                        currentPageBitmap = null,
                        isLoading = false,
                        errorMessage = null
                    )
                }

                renderPage(0)
            } catch (_: SecurityException) {
                pdfRendererEngine.close()
                _uiState.update {
                    it.copy(
                        selectedPdfUri = uri,
                        selectedPdfName = fileName,
                        currentPageBitmap = null,
                        pageCount = 0,
                        currentPageIndex = 0,
                        isLoading = false,
                        errorMessage = "PDF 읽기 권한이 없거나 만료되었습니다."
                    )
                }
            } catch (_: IOException) {
                pdfRendererEngine.close()
                _uiState.update {
                    it.copy(
                        selectedPdfUri = uri,
                        selectedPdfName = fileName,
                        currentPageBitmap = null,
                        pageCount = 0,
                        currentPageIndex = 0,
                        isLoading = false,
                        errorMessage = "파일을 열 수 없습니다. 파일이 없거나 손상되었을 수 있습니다."
                    )
                }
            } catch (_: IllegalArgumentException) {
                pdfRendererEngine.close()
                _uiState.update {
                    it.copy(
                        selectedPdfUri = uri,
                        selectedPdfName = fileName,
                        currentPageBitmap = null,
                        pageCount = 0,
                        currentPageIndex = 0,
                        isLoading = false,
                        errorMessage = "PDF를 렌더링할 수 없습니다."
                    )
                }
            } catch (_: Exception) {
                pdfRendererEngine.close()
                _uiState.update {
                    it.copy(
                        selectedPdfUri = uri,
                        selectedPdfName = fileName,
                        currentPageBitmap = null,
                        pageCount = 0,
                        currentPageIndex = 0,
                        isLoading = false,
                        errorMessage = "PDF 처리 중 오류가 발생했습니다."
                    )
                }
            }
        }
    }

    fun goToPreviousPage() {
        val state = _uiState.value
        if (state.currentPageIndex <= 0) {
            return
        }
        goToPage(state.currentPageIndex - 1)
    }

    fun goToNextPage() {
        val state = _uiState.value
        if (state.currentPageIndex >= state.pageCount - 1) {
            return
        }
        goToPage(state.currentPageIndex + 1)
    }

    fun goToPage(pageIndex: Int) {
        val state = _uiState.value
        if (pageIndex < 0 || pageIndex >= state.pageCount) {
            return
        }
        renderPage(pageIndex)
    }

    fun renderPage(pageIndex: Int) {
        val state = _uiState.value
        if (state.pageCount <= 0) {
            _uiState.update {
                it.copy(errorMessage = "페이지 수가 없어 이동할 수 없습니다.", isLoading = false)
            }
            return
        }

        if (pageIndex < 0 || pageIndex >= state.pageCount) {
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val nextBitmap = pdfRendererEngine.renderPage(
                    pageIndex = pageIndex,
                    targetWidth = targetRenderWidth,
                    targetHeight = 0
                )

                _uiState.update { current ->
                    current.currentPageBitmap?.takeIf { it != nextBitmap }?.recycle()
                    current.copy(
                        currentPageBitmap = nextBitmap,
                        currentPageIndex = pageIndex,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (_: SecurityException) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "PDF 읽기 권한이 없거나 만료되었습니다.")
                }
            } catch (_: IOException) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "파일을 읽을 수 없습니다. 손상 여부를 확인해주세요.")
                }
            } catch (_: IllegalArgumentException) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "페이지 렌더링에 실패했습니다.")
                }
            } catch (_: IllegalStateException) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "PDF가 열려 있지 않습니다.")
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "페이지 처리 중 오류가 발생했습니다.")
                }
            }
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

    fun closePdf() {
        pdfRendererEngine.close()
        _uiState.update {
            it.currentPageBitmap?.recycle()
            it.copy(currentPageIndex = 0, pageCount = 0, currentPageBitmap = null, isLoading = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pdfRendererEngine.close()
        _uiState.value.currentPageBitmap?.recycle()
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
