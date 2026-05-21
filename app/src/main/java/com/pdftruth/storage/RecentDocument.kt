package com.pdftruth.storage

data class RecentDocument(
    val uriString: String,
    val displayName: String,
    val lastPageIndex: Int,
    val lastOpenedAt: Long
)
