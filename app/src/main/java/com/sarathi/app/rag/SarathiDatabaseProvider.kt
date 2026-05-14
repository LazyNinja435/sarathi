package com.sarathi.app.rag

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.OPEN_READONLY
import java.io.File
import java.io.FileOutputStream

/**
 * Copies [ASSET_DB] from assets to internal storage once, then opens read-only SQLite.
 */
object SarathiDatabaseProvider {

    const val ASSET_DB = "rag/sarathi_rag.sqlite"
    private const val INTERNAL_SUBDIR = "rag"
    private const val DB_NAME = "sarathi_rag.sqlite"

    @Volatile
    private var cachedPath: File? = null

    fun databaseFile(context: Context): File =
        File(context.filesDir, "$INTERNAL_SUBDIR/$DB_NAME")

    /**
     * Returns null if asset missing or copy fails.
     */
    fun openReadableDatabase(context: Context): SQLiteDatabase? {
        val f = databaseFile(context)
        if (!f.exists()) {
            if (!copyFromAssets(context, f)) return null
        }
        fun tryOpen(): SQLiteDatabase? =
            try {
                SQLiteDatabase.openDatabase(f.absolutePath, null, OPEN_READONLY)
            } catch (_: Exception) {
                null
            }
        tryOpen()?.let {
            cachedPath = f
            return it
        }
        // Corrupt or partial file: one recovery attempt from assets
        return try {
            f.delete()
            if (!copyFromAssets(context, f)) return null
            val db = tryOpen()
            if (db != null) cachedPath = f
            db
        } catch (_: Exception) {
            null
        }
    }

    private fun copyFromAssets(context: Context, dest: File): Boolean {
        return try {
            dest.parentFile?.mkdirs()
            context.assets.open(ASSET_DB).use { input ->
                FileOutputStream(dest).use { out -> input.copyTo(out) }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun hasAsset(context: Context): Boolean =
        try {
            context.assets.open(ASSET_DB).use { true }
        } catch (_: Exception) {
            false
        }
}
