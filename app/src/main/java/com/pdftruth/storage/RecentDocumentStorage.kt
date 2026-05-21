package com.pdftruth.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private const val RECENT_DOCUMENT_DATASTORE_NAME = "recent_documents_store"
private const val MAX_RECENT_DOCUMENTS = 10
private val Context.recentDocumentDataStore by preferencesDataStore(name = RECENT_DOCUMENT_DATASTORE_NAME)

class RecentDocumentStorage(private val context: Context) {
    private val recentDocumentsKey = stringPreferencesKey("recent_documents")

    suspend fun getRecentDocuments(): List<RecentDocument> {
        return try {
            val json = context.recentDocumentDataStore.data.first()[recentDocumentsKey].orEmpty()
            deserialize(json)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun upsertRecentDocument(document: RecentDocument): Boolean {
        return try {
            val current = getRecentDocuments().toMutableList()
            current.removeAll { it.uriString == document.uriString }
            current.add(0, document)

            val sorted = current
                .sortedByDescending { it.lastOpenedAt }
                .take(MAX_RECENT_DOCUMENTS)

            context.recentDocumentDataStore.edit { preferences ->
                preferences[recentDocumentsKey] = serialize(sorted)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun removeRecentDocument(uriString: String): Boolean {
        return try {
            val filtered = getRecentDocuments().filterNot { it.uriString == uriString }
            context.recentDocumentDataStore.edit { preferences ->
                preferences[recentDocumentsKey] = serialize(filtered)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun clearRecentDocuments(): Boolean {
        return try {
            context.recentDocumentDataStore.edit { preferences ->
                preferences.remove(recentDocumentsKey)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun serialize(documents: List<RecentDocument>): String {
        val jsonArray = JSONArray()
        documents.forEach { document ->
            val item = JSONObject()
            item.put("uriString", document.uriString)
            item.put("displayName", document.displayName)
            item.put("lastPageIndex", document.lastPageIndex)
            item.put("lastOpenedAt", document.lastOpenedAt)
            jsonArray.put(item)
        }
        return jsonArray.toString()
    }

    private fun deserialize(json: String): List<RecentDocument> {
        if (json.isBlank()) {
            return emptyList()
        }

        val jsonArray = JSONArray(json)
        val documents = mutableListOf<RecentDocument>()

        for (index in 0 until jsonArray.length()) {
            val item = jsonArray.optJSONObject(index) ?: continue
            val uriString = item.optString("uriString")
            if (uriString.isBlank()) {
                continue
            }

            documents.add(
                RecentDocument(
                    uriString = uriString,
                    displayName = item.optString("displayName", "알 수 없는 문서"),
                    lastPageIndex = item.optInt("lastPageIndex", 0).coerceAtLeast(0),
                    lastOpenedAt = item.optLong("lastOpenedAt", 0L)
                )
            )
        }

        return documents
            .sortedByDescending { it.lastOpenedAt }
            .take(MAX_RECENT_DOCUMENTS)
    }
}
