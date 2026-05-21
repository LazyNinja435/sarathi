package com.sarathi.app.rag

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SarathiDatabaseProviderTest {
    @Test
    fun databaseRefreshesWhenBundledAssetSizeChanges() {
        assertTrue(SarathiDatabaseProvider.shouldRefreshDatabase(currentBytes = 10, assetBytes = 20))
    }

    @Test
    fun databaseDoesNotRefreshWhenBundledAssetSizeMatches() {
        assertFalse(SarathiDatabaseProvider.shouldRefreshDatabase(currentBytes = 20, assetBytes = 20))
    }
}
