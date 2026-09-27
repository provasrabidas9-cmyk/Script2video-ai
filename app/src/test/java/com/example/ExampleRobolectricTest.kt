package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.DefaultScripts
import com.example.data.local.ProjectRepository
import com.example.data.model.*
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Script2Video AI", appName)
    }

    @Test
    fun `test settings serialization with Android JSONObject`() {
        val settings = VideoSettings(
            language = ScriptLanguage.BENGALI,
            style = VideoStyle.CARTOON,
            aspectRatio = VideoAspectRatio.PORTRAIT_9_16,
            duration = VideoDurationOption.ONE_MIN,
            voiceGender = VoiceGender.FEMALE,
            voiceSpeed = VoiceSpeed.FAST,
            subtitleEnabled = true,
            subtitlePosition = SubtitlePosition.TOP,
            subtitleFontSize = SubtitleFontSize.LARGE,
            backgroundMusicEnabled = false
        )

        val json = ProjectRepository.serializeSettings(settings)
        val deserialized = ProjectRepository.deserializeSettings(json)

        assertEquals(settings.language, deserialized.language)
        assertEquals(settings.style, deserialized.style)
        assertEquals(settings.aspectRatio, deserialized.aspectRatio)
        assertEquals(settings.voiceGender, deserialized.voiceGender)
        assertEquals(settings.voiceSpeed, deserialized.voiceSpeed)
        assertEquals(settings.subtitlePosition, deserialized.subtitlePosition)
        assertEquals(settings.subtitleFontSize, deserialized.subtitleFontSize)
        assertEquals(settings.backgroundMusicEnabled, deserialized.backgroundMusicEnabled)
    }

    @Test
    fun `test scenes serialization with Android JSONArray`() {
        val demoScenes = DefaultScripts.getDemoScenes(ScriptLanguage.ENGLISH)
        val json = ProjectRepository.serializeScenes(demoScenes)
        val deserialized = ProjectRepository.deserializeScenes(json)

        assertEquals(demoScenes.size, deserialized.size)
        assertEquals(demoScenes.first().sceneNumber, deserialized.first().sceneNumber)
        assertEquals(demoScenes.first().dialogue, deserialized.first().dialogue)
    }

    @Test
    fun `test project gallery data integrity`() {
        val project = ProjectData(
            id = 101L,
            title = "Bengali Cartoon Adventure",
            scriptText = "শান্ত গ্রামের এক মিষ্টি শিয়াল...",
            settings = VideoSettings(
                language = ScriptLanguage.BENGALI,
                style = VideoStyle.CARTOON,
                aspectRatio = VideoAspectRatio.LANDSCAPE_16_9
            ),
            scenes = DefaultScripts.getDemoScenes(ScriptLanguage.BENGALI),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        assertEquals("Bengali Cartoon Adventure", project.title)
        assertEquals(ScriptLanguage.BENGALI, project.settings.language)
        assertEquals(3, project.scenes.size)
        assertEquals(13, project.scenes.sumOf { it.estimatedDurationSec })
    }
}
