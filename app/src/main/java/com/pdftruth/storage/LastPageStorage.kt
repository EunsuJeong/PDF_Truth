package com.pdftruth.storage

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private const val LAST_PAGE_DATASTORE_NAME = "pdf_last_page_store"
private val Context.lastPageDataStore by preferencesDataStore(name = LAST_PAGE_DATASTORE_NAME)

class LastPageStorage(private val context: Context) {
    suspend fun readLastPage(uri: Uri): Int? {
        val key = intPreferencesKey(buildKey(uri))
        return try {
            context.lastPageDataStore.data.first()[key]
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveLastPage(uri: Uri, pageIndex: Int): Boolean {
        val key = intPreferencesKey(buildKey(uri))
        return try {
            context.lastPageDataStore.edit { preferences ->
                preferences[key] = pageIndex
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun buildKey(uri: Uri): String {
        return "pdf_last_page_${uri.hashCode()}"
    }
}
