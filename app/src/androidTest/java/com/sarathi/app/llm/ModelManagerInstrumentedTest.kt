package com.sarathi.app.llm

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ModelManagerInstrumentedTest {

    @Test
    fun resolvePreferredModelPath_prefersLitertlmOverTaskInAppPrivateModels() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val models = File(ctx.filesDir, "models")
        models.mkdirs()
        File(models, "gemma.task").writeBytes(byteArrayOf(1))
        File(models, "gemma-4-E2B-it.litertlm").writeBytes(byteArrayOf(2))
        val path = ModelManager.resolvePreferredModelPath(ctx, "")
        assertTrue(path!!.endsWith("gemma-4-E2B-it.litertlm", ignoreCase = true))
        File(models, "gemma.task").delete()
        File(models, "gemma-4-E2B-it.litertlm").delete()
    }
}
