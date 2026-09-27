package com.example

import com.example.data.ai.DefaultScripts
import com.example.data.model.ScriptLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testScriptLanguagesAvailability() {
        val languages = ScriptLanguage.values()
        assertEquals(3, languages.size)
        assertTrue(languages.contains(ScriptLanguage.BENGALI))
        assertTrue(languages.contains(ScriptLanguage.HINDI))
        assertTrue(languages.contains(ScriptLanguage.ENGLISH))

        for (lang in languages) {
            val sample = DefaultScripts.getSampleScript(lang)
            assertTrue(sample.isNotBlank())
            val demoScenes = DefaultScripts.getDemoScenes(lang)
            assertTrue(demoScenes.isNotEmpty())
        }
    }
}
