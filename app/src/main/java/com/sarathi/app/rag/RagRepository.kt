package com.sarathi.app.rag

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.sarathi.app.rag.SarathiDatabaseProvider.ASSET_DB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Offline FTS5 retrieval over bundled [sarathi_rag.sqlite].
 */
class RagRepository(private val appContext: Context) {

    private val lock = Any()
    @Volatile
    private var db: SQLiteDatabase? = null

    private fun getDb(): SQLiteDatabase? {
        synchronized(lock) {
            if (db != null) return db
            if (!SarathiDatabaseProvider.hasAsset(appContext)) return null
            db = SarathiDatabaseProvider.openReadableDatabase(appContext)
            return db
        }
    }

    suspend fun warmUp(): Boolean = withContext(Dispatchers.IO) {
        getDb() != null
    }

    fun isReady(): Boolean = db != null

    suspend fun search(query: String, limit: Int = 5): List<RagSearchResult> = withContext(Dispatchers.IO) {
        val database = getDb() ?: return@withContext emptyList()
        val q = ftsSafeQuery(query) ?: return@withContext emptyList()
        val sql =
            "SELECT d.*, bm25(documents_fts) AS rank " +
                "FROM documents_fts JOIN documents d ON d.id = documents_fts.doc_id " +
                "WHERE documents_fts MATCH ? ORDER BY rank LIMIT ?"
        rawSearch(database, sql, arrayOf(q, limit.toString()))
    }

    suspend fun searchByTheme(theme: String, limit: Int = 10): List<RagSearchResult> = withContext(Dispatchers.IO) {
        val database = getDb() ?: return@withContext emptyList()
        val t = theme.trim().lowercase()
        if (t.isEmpty()) return@withContext emptyList()
        val sql =
            "SELECT d.*, 1.0 AS rank FROM documents d " +
                "WHERE lower(d.themes_json) LIKE ? LIMIT ?"
        val arg = "%$t%"
        rawSearch(database, sql, arrayOf(arg, limit.toString()))
    }

    suspend fun getVerse(chapter: Int, verse: Int): RagSearchResult? = withContext(Dispatchers.IO) {
        val database = getDb() ?: return@withContext null
        val pattern =
            "gita_${chapter.toString().padStart(2, '0')}_${verse.toString().padStart(3, '0')}_*"
        val sql = "SELECT d.*, 1.0 AS rank FROM documents d WHERE d.collection = 'gita' AND d.id GLOB ? LIMIT 1"
        rawSearch(database, sql, arrayOf(pattern)).firstOrNull()
    }

    suspend fun getVerseOfDay(): RagSearchResult? = withContext(Dispatchers.IO) {
        val database = getDb() ?: return@withContext null
        val total = database.rawQuery(
            "SELECT COUNT(*) FROM documents WHERE collection = 'gita'",
            null,
        ).use { c ->
            if (!c.moveToFirst()) 0 else c.getInt(0)
        }
        if (total <= 0) return@withContext null
        val day = java.time.LocalDate.now().dayOfYear
        val offset = day % total
        val sql =
            "SELECT d.*, 1.0 AS rank FROM documents d WHERE d.collection = 'gita' ORDER BY d.id LIMIT 1 OFFSET ?"
        rawSearch(database, sql, arrayOf(offset.toString())).firstOrNull()
    }

    private fun ftsSafeQuery(raw: String): String? {
        val t = raw.trim()
        if (t.isEmpty()) return null
        val escaped = t.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun rawSearch(database: SQLiteDatabase, sql: String, args: Array<String>): List<RagSearchResult> {
        val out = mutableListOf<RagSearchResult>()
        var c: Cursor? = null
        try {
            c = database.rawQuery(sql, args)
            val idxId = c.getColumnIndex("id")
            val idxWork = c.getColumnIndex("work")
            val idxCol = c.getColumnIndex("collection")
            val idxTitle = c.getColumnIndex("title")
            val idxCit = c.getColumnIndex("citation")
            val idxText = c.getColumnIndex("text")
            val idxTrans = c.getColumnIndex("translation")
            val idxSk = c.getColumnIndex("sanskrit")
            val idxSid = c.getColumnIndex("source_id")
            val idxThemes = c.getColumnIndex("themes_json")
            val idxRank = c.getColumnIndex("rank")
            val srcTitle = sourceTitleMap(database)
            val srcUrl = sourceUrlMap(database)
            while (c.moveToNext()) {
                val sid = c.getString(idxSid) ?: ""
                val themes = parseJsonArray(c.getString(idxThemes))
                val rank = if (idxRank >= 0) c.getDouble(idxRank) else 1.0
                out.add(
                    RagSearchResult(
                        id = c.getString(idxId) ?: "",
                        work = c.getString(idxWork) ?: "",
                        collection = c.getString(idxCol) ?: "",
                        title = c.getString(idxTitle) ?: "",
                        citation = c.getString(idxCit) ?: "",
                        text = c.getString(idxText) ?: "",
                        translation = c.getString(idxTrans) ?: "",
                        sanskrit = c.getString(idxSk) ?: "",
                        sourceTitle = srcTitle[sid] ?: sid,
                        sourceUrl = srcUrl[sid] ?: "",
                        themes = themes,
                        score = rank,
                    ),
                )
            }
        } catch (_: Exception) {
            return emptyList()
        } finally {
            c?.close()
        }
        return out
    }

    private fun sourceTitleMap(db: SQLiteDatabase): Map<String, String> {
        val m = mutableMapOf<String, String>()
        db.rawQuery("SELECT id, title FROM sources", null).use { c ->
            while (c.moveToNext()) {
                m[c.getString(0) ?: ""] = c.getString(1) ?: ""
            }
        }
        return m
    }

    private fun sourceUrlMap(db: SQLiteDatabase): Map<String, String> {
        val m = mutableMapOf<String, String>()
        db.rawQuery("SELECT id, source_url FROM sources", null).use { c ->
            while (c.moveToNext()) {
                m[c.getString(0) ?: ""] = c.getString(1) ?: ""
            }
        }
        return m
    }

    private fun parseJsonArray(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    add(arr.getString(i))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        const val ASSET_RELATIVE_PATH: String = ASSET_DB
    }
}
