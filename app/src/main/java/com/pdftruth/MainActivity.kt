package com.pdftruth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var lastHandledExternalIntentUri: String? = null

    private val pdfPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data

            if (uri == null) {
                return@registerForActivityResult
            }

            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                viewModel.openAndRenderFirstPage(uri)
            } catch (_: SecurityException) {
                viewModel.setError("PDF 읽기 권한 유지에 실패했습니다.")
            } catch (_: Exception) {
                viewModel.setError("PDF 선택 처리 중 오류가 발생했습니다.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val errorMessage = uiState.errorMessage
            val currentPageBitmap = uiState.currentPageBitmap
            val pageRatio = if (currentPageBitmap != null && currentPageBitmap.height > 0) {
                currentPageBitmap.width.toFloat() / currentPageBitmap.height.toFloat()
            } else {
                1f
            }

            val isFirstPage = uiState.currentPageIndex <= 0
            val isLastPage = uiState.pageCount == 0 || uiState.currentPageIndex >= uiState.pageCount - 1
            val canNavigate = uiState.pageCount > 0 && !uiState.isLoading
            val currentPageDisplay = if (uiState.pageCount > 0) uiState.currentPageIndex + 1 else 0
            val isViewingPdf = uiState.selectedPdfUri != null && uiState.pageCount > 0

            val minScale = 1f
            val maxScale = 3f
            var scale by rememberSaveable { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            val logGesture: (String) -> Unit = remember {
                { message ->
                    Log.d("PDFTruthGesture", message)
                }
            }

            val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
                val previousScale = scale
                val nextScale = (previousScale * zoomChange).coerceIn(minScale, maxScale)
                scale = nextScale

                if (nextScale <= 1f) {
                    if (offset != Offset.Zero || previousScale != 1f) {
                        logGesture("scale reset")
                    }
                    scale = 1f
                    offset = Offset.Zero
                    return@rememberTransformableState
                }

                offset += panChange
            }

            LaunchedEffect(uiState.selectedPdfUri) {
                if (uiState.selectedPdfUri == null) {
                    scale = 1f
                    offset = Offset.Zero
                }
            }

            LaunchedEffect(uiState.currentPageIndex, uiState.selectedPdfUri) {
                if (scale != 1f || offset != Offset.Zero) {
                    scale = 1f
                    offset = Offset.Zero
                    logGesture("scale reset")
                }
            }

            var showClearDialog by remember { mutableStateOf(false) }
            var deleteTargetUri by remember { mutableStateOf<String?>(null) }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF202124)
                ) {
                    if (isViewingPdf) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(onClick = { viewModel.closePdf() }) {
                                    Text(text = "닫기")
                                }

                                Text(
                                    text = "${currentPageDisplay} / ${uiState.pageCount}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.width(56.dp))
                            }

                            BoxWithConstraints(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                val density = LocalDensity.current
                                val swipeThresholdDp = max(maxWidth.value * 0.12f, 80f)
                                val swipeThresholdPx = with(density) { swipeThresholdDp.dp.toPx() }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(scale, canNavigate, isFirstPage, isLastPage, swipeThresholdPx) {
                                        awaitEachGesture {
                                            awaitFirstDown(requireUnconsumed = false)
                                            var totalDragX = 0f
                                            var totalDragY = 0f
                                            var maxPointerCount = 1

                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val pressedCount = event.changes.count { it.pressed }
                                                if (pressedCount == 0) {
                                                    break
                                                }

                                                maxPointerCount = max(maxPointerCount, pressedCount)

                                                val change = event.changes.firstOrNull { it.pressed } ?: event.changes.first()
                                                val delta = change.positionChange()
                                                totalDragX += delta.x
                                                totalDragY += delta.y

                                                if (scale == 1f && maxPointerCount < 2 && abs(totalDragX) > abs(totalDragY)) {
                                                    change.consume()
                                                }
                                            }

                                            val canSwipe =
                                                canNavigate &&
                                                scale == 1f &&
                                                maxPointerCount < 2 &&
                                                abs(totalDragX) > abs(totalDragY) &&
                                                abs(totalDragX) >= swipeThresholdPx

                                            if (!canSwipe) {
                                                return@awaitEachGesture
                                            }

                                            if (totalDragX < 0f && !isLastPage) {
                                                viewModel.goToNextPage()
                                                logGesture("swipe next")
                                            } else if (totalDragX > 0f && !isFirstPage) {
                                                viewModel.goToPreviousPage()
                                                logGesture("swipe previous")
                                            }
                                        }
                                        }
                                        .transformable(state = transformableState),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (currentPageBitmap != null) {
                                        Image(
                                            bitmap = currentPageBitmap.asImageBitmap(),
                                            contentDescription = "PDF 현재 페이지",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(pageRatio)
                                                .graphicsLayer(
                                                    scaleX = scale,
                                                    scaleY = scale,
                                                    translationX = offset.x,
                                                    translationY = offset.y
                                                )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (uiState.isLoading) {
                                Text(
                                    text = "페이지를 불러오는 중입니다...",
                                    color = Color(0xFFCBD0D6),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            if (!errorMessage.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = errorMessage,
                                    color = Color(0xFFFFB4AB),
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
                        ) {
                            Button(
                                onClick = {
                                    scale = 1f
                                    offset = Offset.Zero
                                    viewModel.clearError()
                                    val openPdfIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "application/pdf"
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                                    }
                                    pdfPickerLauncher.launch(openPdfIntent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "PDF 선택")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = uiState.selectedPdfName ?: "선택된 파일 없음",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "최근 문서",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                if (uiState.recentDocuments.isNotEmpty()) {
                                    TextButton(onClick = { showClearDialog = true }) {
                                        Text(
                                            text = "전체 비우기",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFCBD0D6)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (uiState.recentDocuments.isEmpty()) {
                                Text(
                                    text = "최근 문서가 없습니다.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD0D6),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                uiState.recentDocuments.forEach { document ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                scale = 1f
                                                offset = Offset.Zero
                                                viewModel.openRecentDocument(document)
                                            }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = document.displayName,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "마지막 위치: ${document.lastPageIndex + 1}페이지",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFCBD0D6)
                                            )
                                        }
                                        TextButton(onClick = { deleteTargetUri = document.uriString }) {
                                            Text(
                                                text = "삭제",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFCBD0D6)
                                            )
                                        }
                                    }
                                }
                            }

                            if (!errorMessage.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = errorMessage,
                                    color = Color(0xFFFFB4AB),
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // 개별 삭제 확인 다이얼로그
            val targetUri = deleteTargetUri
            if (targetUri != null) {
                AlertDialog(
                    onDismissRequest = { deleteTargetUri = null },
                    title = { Text(text = "목록에서 삭제") },
                    text = { Text(text = "이 항목을 최근 문서 목록에서 삭제합니다.\n실제 PDF 파일은 삭제되지 않습니다.") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.removeRecentDocument(targetUri)
                            deleteTargetUri = null
                        }) {
                            Text(text = "삭제")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteTargetUri = null }) {
                            Text(text = "취소")
                        }
                    }
                )
            }

            // 전체 비우기 확인 다이얼로그
            if (showClearDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    title = { Text(text = "전체 비우기") },
                    text = { Text(text = "최근 문서 목록을 모두 비웁니다.\n실제 PDF 파일은 삭제되지 않습니다.") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.clearAllRecentDocuments()
                            showClearDialog = false
                        }) {
                            Text(text = "비우기")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearDialog = false }) {
                            Text(text = "취소")
                        }
                    }
                )
            }
        }

        handleExternalPdfIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleExternalPdfIntent(intent)
    }

    private fun handleExternalPdfIntent(sourceIntent: Intent?) {
        if (sourceIntent?.action != Intent.ACTION_VIEW) {
            return
        }

        val uri = sourceIntent.data ?: return
        val mimeType = sourceIntent.type?.lowercase()
        val hasPdfMime = mimeType == "application/pdf" || mimeType?.startsWith("application/pdf;") == true
        val hasPdfHint = uri.toString().contains(".pdf", ignoreCase = true)

        if (!hasPdfMime && !hasPdfHint) {
            return
        }

        val uriKey = uri.toString()
        if (lastHandledExternalIntentUri == uriKey) {
            return
        }
        lastHandledExternalIntentUri = uriKey

        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // 외부 VIEW intent는 persistable 권한이 없는 경우가 있으므로 무시하고 열기를 시도한다.
        }

        viewModel.openAndRenderFirstPage(uri)
    }
}
