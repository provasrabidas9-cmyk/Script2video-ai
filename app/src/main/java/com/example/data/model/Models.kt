package com.example.data.model

import java.util.Locale
import java.util.UUID

enum class ScriptLanguage(
    val displayName: String,
    val nativeName: String,
    val code: String,
    val locale: Locale
) {
    BENGALI("Bengali", "বাংলা", "bn", Locale("bn", "IN")),
    HINDI("Hindi", "हिन्दी", "hi", Locale("hi", "IN")),
    ENGLISH("English", "English", "en", Locale.US);

    val fullLabel: String get() = "$displayName ($nativeName)"
}

enum class VideoStyle(val displayName: String, val promptModifier: String) {
    CARTOON("Cartoon", "vibrant 2D cartoon illustration, expressive animated style, bold clean lines, rich colors"),
    CARTOON_3D("3D Cartoon", "Pixar/Dreamworks inspired 3D animated CGI render, soft studio lighting, cute stylized character design, 8k render"),
    REALISTIC("Realistic", "photorealistic cinematography, hyper-detailed, authentic 35mm lens, natural film grain, 4k ultra-real"),
    CINEMATIC("Cinematic", "epic cinematic movie still, anamorphic lighting, dramatic volumetric haze, golden hour backlight, masterclass cinematography"),
    ANIME("Anime", "high-end modern Makoto Shinkai anime aesthetic, lush painted backgrounds, detailed cel shading, radiant lighting"),
    STORYBOOK("Storybook", "charming whimsical fairy-tale storybook watercolor illustration, textured paper finish, warm nostalgic hand-drawn art")
}

enum class VideoAspectRatio(val displayName: String, val ratioLabel: String, val widthRatio: Float, val heightRatio: Float) {
    PORTRAIT_9_16("9:16 Portrait", "9:16", 9f, 16f),
    LANDSCAPE_16_9("16:9 Landscape", "16:9", 16f, 9f),
    SQUARE_1_1("1:1 Square", "1:1", 1f, 1f);

    val ratio: Float get() = widthRatio / heightRatio
}

enum class VideoDurationOption(val displayName: String, val targetMinutes: Int) {
    AUTO("Auto", 0),
    ONE_MIN("1 minute", 1),
    TWO_MIN("2 minutes", 2),
    THREE_MIN("3 minutes", 3),
    FIVE_MIN("5 minutes", 5)
}

enum class VoiceGender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female")
}

enum class VoiceSpeed(val displayName: String, val rateMultiplier: Float) {
    SLOW("Slow (0.8x)", 0.8f),
    NORMAL("Normal (1.0x)", 1.0f),
    FAST("Fast (1.25x)", 1.25f)
}

enum class SubtitlePosition(val displayName: String) {
    BOTTOM("Bottom"),
    CENTER("Center"),
    TOP("Top")
}

enum class SubtitleFontSize(val displayName: String, val spSize: Int) {
    SMALL("Small", 14),
    MEDIUM("Medium", 18),
    LARGE("Large", 24)
}

data class VideoSettings(
    val language: ScriptLanguage = ScriptLanguage.ENGLISH,
    val style: VideoStyle = VideoStyle.CINEMATIC,
    val aspectRatio: VideoAspectRatio = VideoAspectRatio.LANDSCAPE_16_9,
    val duration: VideoDurationOption = VideoDurationOption.AUTO,
    val voiceGender: VoiceGender = VoiceGender.MALE,
    val voiceSpeed: VoiceSpeed = VoiceSpeed.NORMAL,
    val subtitleEnabled: Boolean = true,
    val subtitlePosition: SubtitlePosition = SubtitlePosition.BOTTOM,
    val subtitleFontSize: SubtitleFontSize = SubtitleFontSize.MEDIUM,
    val backgroundMusicEnabled: Boolean = true
)

data class SubtitleSegment(
    val text: String,
    val startMs: Long,
    val endMs: Long
)

data class SceneItem(
    val id: String = UUID.randomUUID().toString(),
    val sceneNumber: Int,
    val sceneDescription: String,
    val characterDescription: String,
    val backgroundDescription: String,
    val cameraDirection: String,
    val dialogue: String,
    val estimatedDurationSec: Int = 4,
    val visualPrompt: String = "",
    val visualImagePath: String? = null,
    val audioFilePath: String? = null,
    val subtitles: List<SubtitleSegment> = emptyList()
)

data class ProjectData(
    val id: Long = 0,
    val title: String,
    val scriptText: String,
    val settings: VideoSettings,
    val scenes: List<SceneItem>,
    val finalVideoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class GenerationStep(val stepNumber: Int, val title: String, val description: String) {
    ANALYZING_SCRIPT(1, "Analyzing script", "Reading pacing, emotion, and narrative arcs..."),
    CREATING_SCENES(2, "Creating scenes", "Breaking script into cinematic shots & consistent characters..."),
    GENERATING_VISUALS(3, "Generating visuals", "Rendering visual art adhering to style and framing..."),
    GENERATING_VOICE(4, "Generating voice", "Synthesizing authentic voice-over in target language..."),
    CREATING_SUBTITLES(5, "Creating subtitles", "Synchronizing localized captions and timecodes..."),
    COMBINING_VIDEO(6, "Combining video", "Merging audio layers, score, and scene cut transitions..."),
    FINALIZING_VIDEO(7, "Finalizing video", "Packaging high-definition video ready for playback & export...")
}
