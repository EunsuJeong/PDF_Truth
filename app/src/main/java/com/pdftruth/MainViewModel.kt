package com.pdftruth

import android.app.Application
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pdftruth.storage.LastPageStorage
import com.pdftruth.storage.RecentDocument
import com.pdftruth.storage.RecentDocumentStorage
import com.pdftruth.viewer.engine.PdfRendererEngine
import java.io.IOException
import java.lang.System.currentTimeMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val targetRenderWidth = 1080
    private val lastPageStorage = LastPageStorage(getApplication())
    private val recentDocumentStorage = RecentDocumentStorage(getApplication())

    private val pdfRendererEngine = PdfRendererEngine { uri ->
        getApplication<Application>().contentResolver.openFileDescriptor(uri, "r")
    }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState
    private var renderJob: Job? = null
    private var currentRenderRequestId: Long = 0L

    init {
        loadRecentDocuments()
    }

    fun loadRecentDocuments() {
        viewModelScope.launch {
            try {
                val documents = recentDocumentStorage.getRecentDocuments()
                _uiState.update {
                    it.copy(recentDocuments = documents)
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "최근 문서 목록을 불러오지 못했습니다.")
                }
            }
        }
    }

    fun openAndRenderFirstPage(
        uri: Uri?,
        removeFromRecentOnError: Boolean = false,
        restoreLastPage: Boolean = false
    ) {
        if (uri == null) {
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val fileName = extractDisplayName(uri)

            try {
                renderJob?.cancel()
                pdfRendererEngine.close()
                pdfRendererEngine.open(uri)

                val pageCount = pdfRendererEngine.getPageCount()
                if (pageCount <= 0) {
                    throw IllegalArgumentException("페이지 수가 0입니다.")
                }

                _uiState.update { current ->
                    current.currentPageBitmap?.recycle()
                    current.copy(
                        selectedPdfUri = uri,
                        selectedPdfName = fileName,
                        currentPageIndex = 0,
                        pageCount = pageCount,
                        currentPageBitmap = null,
                        isLoading = false,
                        errorMessage = null
                    )
                }

                val restorePage = if (restoreLastPage) {
                    val savedPage = try {
                        lastPageStorage.readLastPage(uri)
                    } catch (_: Exception) {
                        null
                    }

                    if (savedPage != null && savedPage in 0 until pageCount) {
                        savedPage
                    } else {
                        0
                    }
                } else {
                    0
                }

                renderPageInternal(pageIndex = restorePage, shouldSaveLastPage = false)
            } catch (_: SecurityException) {
                pdfRendererEngine.close()
                if (removeFromRecentOnError) removeRecentDocumentSilent(uri.toString())
                _uiState.update { current ->
                    current.currentPageBitmap?.recycle()
                    current.copy(
                        selectedPdfUri = uri,
                        selectedPdfName = fileName,
                        currentPageBitmap = null,
                        pageCount = 0,
                        currentPageIndex = 0,
                        isLoading = false,
                        errorMessage = "권한이 만료되었습니다. PDF를 다시 선택해 주세요."
                    )
                }
            } catch (_: IOException) {
                pdfRendererEngine.close()
                if (removeFromRecentOnError) removeRecentDocumentSilent(uri.toString())
                _uiState.update { current ->
                    current.currentPageBitmap?.recycle()
                    current.copy(
                        selectedPdfUri = uri,
                        selectedPdfName = fileName,
                        currentPageBitmap = null,
                        pageCount = 0,
                        currentPageIndex = 0,
                        isLoading = false,
                        errorMessage = "파일이 삭제되었거나 이동되었습니다. PDF를 다시 선택해 주세요."
                    )
                }
            } catch (_: IllegalArgumentException) {
                pdfRendererEngine.close()
                if (removeFromRecentOnError) removeRecentDocumentSilent(uri.toString())
                _uiState.update { current ->
                    current.currentPageBitmap?.recycle()
                    current.copy(
                        selectedPdfUri = uri,
                        selectedPdfName = fileName,
                        currentPageBitmap = null,
                        pageCount = 0,
                        currentPageIndex = 0,
                        isLoading = false,
                        errorMessage = "PDF를 열 수 없습니다."
                    )
                }
            } catch (_: Exception) {
                pdfRendererEngine.close()
                if (removeFromRecentOnError) removeRecentDocumentSilent(uri.toString())
                _uiState.update { current ->
                    current.currentPageBitmap?.recycle()
                    current.copy(
                        selectedPdfUri = uri,
                        selectedPdfName = fileName,
                        currentPageBitmap = null,
                        pageCount = 0,
                        currentPageIndex = 0,
                        isLoading = false,
                        errorMessage = "PDF를 열 수 없습니다."
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
        renderPageInternal(pageIndex = pageIndex, shouldSaveLastPage = true)
    }

    fun renderPage(pageIndex: Int) {
        renderPageInternal(pageIndex = pageIndex, shouldSaveLastPage = false)
    }

    fun renderCurrentPageForScale(scale: Float) {
        val state = _uiState.value
        if (state.pageCount <= 0) {
            return
        }
        renderPageInternal(
            pageIndex = state.currentPageIndex,
            shouldSaveLastPage = false,
            renderScale = scale,
            updateRecentDocument = false,
            showLoading = false
        )
    }

    private fun renderPageInternal(
        pageIndex: Int,
        shouldSaveLastPage: Boolean,
        renderScale: Float = 1f,
        updateRecentDocument: Boolean = true,
        showLoading: Boolean = true
    ) {
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

        if (showLoading) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        }

        renderJob?.cancel()
        val localRequestId = ++currentRenderRequestId

        renderJob = viewModelScope.launch {
            try {
                val nextBitmap = withContext(Dispatchers.Default) {
                    pdfRendererEngine.renderPage(
                        pageIndex = pageIndex,
                        targetWidth = targetRenderWidth,
                        targetHeight = 0,
                        scale = renderScale
                    )
                }

                if (localRequestId != currentRenderRequestId) {
                    nextBitmap.recycle()
                    return@launch
                }

                var pageSaved = true
                _uiState.update { current ->
                    current.currentPageBitmap?.takeIf { it != nextBitmap }?.recycle()
                    current.copy(
                        currentPageBitmap = nextBitmap,
                        currentPageIndex = pageIndex,
                        isLoading = false,
                        errorMessage = null
                    )
                }

                if (shouldSaveLastPage) {
                    val selectedUri = _uiState.value.selectedPdfUri
                    if (selectedUri != null) {
                        pageSaved = try {
                            lastPageStorage.saveLastPage(selectedUri, pageIndex)
                        } catch (_: Exception) {
                            false
                        }
                    }
                }

                if (!pageSaved) {
                    _uiState.update {
                        it.copy(errorMessage = "마지막 페이지 저장에 실패했습니다.")
                    }
                }

                if (updateRecentDocument) {
                    updateRecentDocumentOnOpenOrPageChange(pageIndex)
                }
            } catch (_: CancellationException) {
                if (localRequestId == currentRenderRequestId && showLoading) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (_: SecurityException) {
                if (localRequestId != currentRenderRequestId) {
                    return@launch
                }
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "권한이 만료되었습니다. PDF를 다시 선택해 주세요.")
                }
            } catch (_: IOException) {
                if (localRequestId != currentRenderRequestId) {
                    return@launch
                }
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "파일이 삭제되었거나 이동되었습니다.")
                }
            } catch (_: IllegalArgumentException) {
                if (localRequestId != currentRenderRequestId) {
                    return@launch
                }
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "PDF 렌더링 중 오류가 발생했습니다.")
                }
            } catch (_: IllegalStateException) {
                if (localRequestId != currentRenderRequestId) {
                    return@launch
                }
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "PDF가 열려 있지 않습니다.")
                }
            } catch (_: Exception) {
                if (localRequestId != currentRenderRequestId) {
                    return@launch
                }
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "PDF 렌더링 중 오류가 발생했습니다.")
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

    fun openRecentDocument(document: RecentDocument) {
        if (document.uriString.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "최근 문서 정보가 올바르지 않습니다.")
            }
            return
        }

        val uri = try {
            Uri.parse(document.uriString)
        } catch (_: Exception) {
            viewModelScope.launch {
                recentDocumentStorage.removeRecentDocument(document.uriString)
                val documents = recentDocumentStorage.getRecentDocuments()
                _uiState.update {
                    it.copy(
                        recentDocuments = documents,
                        errorMessage = "최근 문서 URI를 해석할 수 없습니다. PDF를 다시 선택해 주세요."
                    )
                }
            }
            return
        }

        openAndRenderFirstPage(
            uri = uri,
            removeFromRecentOnError = true,
            restoreLastPage = true
        )
    }

    fun removeRecentDocument(uriString: String) {
        viewModelScope.launch {
            recentDocumentStorage.removeRecentDocument(uriString)
            val documents = recentDocumentStorage.getRecentDocuments()
            val isCurrentDocument = _uiState.value.selectedPdfUri?.toString() == uriString
            if (isCurrentDocument) {
                closePdfInternal(
                    message = "최근 문서에서 제거되었습니다.",
                    recentDocumentsOverride = documents
                )
            } else {
                _uiState.update { it.copy(recentDocuments = documents) }
            }
        }
    }

    fun clearAllRecentDocuments() {
        viewModelScope.launch {
            val selectedUri = _uiState.value.selectedPdfUri?.toString()
            val selectedWasInRecent = selectedUri != null &&
                _uiState.value.recentDocuments.any { it.uriString == selectedUri }

            recentDocumentStorage.clearRecentDocuments()
            if (selectedWasInRecent) {
                closePdfInternal(
                    message = "최근 문서에서 제거되었습니다.",
                    recentDocumentsOverride = emptyList()
                )
            } else {
                _uiState.update { it.copy(recentDocuments = emptyList()) }
            }
        }
    }

    private suspend fun removeRecentDocumentSilent(uriString: String) {
        try {
            recentDocumentStorage.removeRecentDocument(uriString)
            val documents = recentDocumentStorage.getRecentDocuments()
            _uiState.update { it.copy(recentDocuments = documents) }
        } catch (_: Exception) {
            // 자동 제거 실패는 무시
        }
    }

    fun closePdf() {
        closePdfInternal(message = null)
    }

    private fun closePdfInternal(message: String?, recentDocumentsOverride: List<RecentDocument>? = null) {
        renderJob?.cancel()
        pdfRendererEngine.close()
        _uiState.update { current ->
            current.currentPageBitmap?.recycle()
            current.copy(
                selectedPdfUri = null,
                selectedPdfName = null,
                currentPageIndex = 0,
                pageCount = 0,
                currentPageBitmap = null,
                isLoading = false,
                errorMessage = message,
                recentDocuments = recentDocumentsOverride ?: current.recentDocuments
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        renderJob?.cancel()
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

    private suspend fun updateRecentDocumentOnOpenOrPageChange(pageIndex: Int) {
        val state = _uiState.value
        val selectedUri = state.selectedPdfUri ?: return
        val selectedName = state.selectedPdfName ?: fallbackName(selectedUri)

        val isSaved = try {
            recentDocumentStorage.upsertRecentDocument(
                document = RecentDocument(
                    uriString = selectedUri.toString(),
                    displayName = selectedName,
                    lastPageIndex = pageIndex.coerceAtLeast(0),
                    lastOpenedAt = currentTimeMillis()
                )
            )
        } catch (_: Exception) {
            false
        }

        if (isSaved) {
            val documents = recentDocumentStorage.getRecentDocuments()
            _uiState.update {
                it.copy(recentDocuments = documents)
            }
        } else {
            _uiState.update {
                it.copy(errorMessage = "최근 문서 저장에 실패했습니다.")
            }
        }
    }
}
