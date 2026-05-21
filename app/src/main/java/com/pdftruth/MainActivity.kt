package com.pdftruth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

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

            val minScale = 1f
            val maxScale = 3f
            var scale by rememberSaveable { mutableFloatStateOf(1f) }
            val transformableState = rememberTransformableState { zoomChange, _, _ ->
                scale = (scale * zoomChange).coerceIn(minScale, maxScale)
            }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF202124)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Text(
                            text = "PDF Truth",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
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

                        Text(
                            text = "최근 문서",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        )

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
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.openRecentDocument(document) }
                                        .padding(vertical = 8.dp)
                                ) {
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
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (uiState.isLoading) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "페이지를 불러오는 중입니다...",
                                color = Color(0xFFCBD0D6),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (currentPageBitmap != null) {
                            Text(
                                text = "${(scale * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD0D6),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .transformable(state = transformableState),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = currentPageBitmap.asImageBitmap(),
                                    contentDescription = "PDF 현재 페이지",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth(0.92f)
                                        .aspectRatio(pageRatio)
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = {
                                    scale = 1f
                                    viewModel.goToPreviousPage()
                                },
                                enabled = canNavigate && !isFirstPage
                            ) {
                                Text(text = "이전")
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = "${currentPageDisplay} / ${uiState.pageCount}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Button(
                                onClick = {
                                    scale = 1f
                                    viewModel.goToNextPage()
                                },
                                enabled = canNavigate && !isLastPage
                            ) {
                                Text(text = "다음")
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
    }
}
