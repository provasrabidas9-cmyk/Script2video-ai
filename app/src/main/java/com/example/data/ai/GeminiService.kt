package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed class GeminiResult<out T> {
    data class Success<out T>(val data: T) : GeminiResult<T>()
    data class Error(val message: String, val requiredApi: String? = null) : GeminiResult<Nothing>()
}

class GeminiService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val apiKey: String
        get() = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

    val isApiKeyConfigured: Boolean
        get() = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

    /**
     * Improves script preserving meaning and language (Bengali, Hindi, or English).
     */
    suspend fun improveScript(script: String, language: ScriptLanguage): GeminiResult<String> = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured) {
            return@withContext GeminiResult.Error(
                "Gemini API key is not configured in Secrets panel.",
                "gemini-3.5-flash"
            )
        }

        val prompt = """
You are a master cinematic screenwriter and storyteller.
Improve the following script written in ${language.displayName} (${language.nativeName}).
Make the pacing tighter, visual imagery richer, emotional dialogue more impactful, while preserving the EXACT core meaning, plot points, and character motivations.
DO NOT change the language. Output ONLY the improved script in ${language.displayName} (${language.nativeName}) with no conversational filler, no notes, and no markdown intros.

Original script:
$script
        """.trimIndent()

        try {
            val responseText = callGeminiTextModel(prompt, model = "gemini-3.5-flash")
            if (responseText.isNullOrBlank()) {
                GeminiResult.Error("Received empty response from AI script improver.")
            } else {
                GeminiResult.Success(responseText.trim())
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Script improvement failed", e)
            GeminiResult.Error("AI improvement error: ${e.localizedMessage ?: e.message}")
        }
    }

    /**
     * Divides script into cinematic scenes with character consistency.
     */
    suspend fun divideIntoScenes(
        script: String,
        settings: VideoSettings
    ): GeminiResult<List<SceneItem>> = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured) {
            // Provide intelligent structured scene generation from script even without active key
            val localScenes = parseScriptLocally(script, settings)
            return@withContext GeminiResult.Success(localScenes)
        }

        val durationGuide = when (settings.duration) {
            VideoDurationOption.AUTO -> "Choose an optimal count between 3 to 6 scenes."
            VideoDurationOption.ONE_MIN -> "Create 5 to 7 scenes totaling approximately 60 seconds."
            VideoDurationOption.TWO_MIN -> "Create 8 to 12 scenes totaling approximately 120 seconds."
            VideoDurationOption.THREE_MIN -> "Create 12 to 15 scenes totaling approximately 180 seconds."
            VideoDurationOption.FIVE_MIN -> "Create 15 to 20 scenes totaling approximately 300 seconds."
        }

        val prompt = """
You are an expert film director and AI video producer.
Break the following script into sequential video scenes.
Target Language: ${settings.language.displayName} (${settings.language.nativeName}).
Visual Style: ${settings.style.displayName} (${settings.style.promptModifier}).
Aspect Ratio: ${settings.aspectRatio.displayName}.
Duration Target: $durationGuide

CRITICAL RULES:
1. Maintain strict character consistency across ALL scenes. Describe each recurring character with the identical clothing, colors, distinctive traits, and art style in characterDescription.
2. Dialogue and narration MUST be strictly in ${settings.language.displayName} (${settings.language.nativeName}).
3. For visualPrompt, create an English high-resolution image prompt describing the subject, characters with exact consistent appearance, lighting, shot framing, and visual style (${settings.style.promptModifier}).
4. Output MUST be ONLY valid JSON array with NO markdown blocks or code fencing.

JSON Structure:
[
  {
    "sceneNumber": 1,
    "sceneDescription": "Detailed overview of what occurs visually",
    "characterDescription": "Consistent appearance of characters in scene",
    "backgroundDescription": "Environment and backdrop details",
    "cameraDirection": "Camera movement and shot type (e.g. Wide angle establishing shot, pan right)",
    "dialogue": "Spoken voice-over or dialogue in ${settings.language.displayName}",
    "estimatedDurationSec": 5,
    "visualPrompt": "Detailed English image generation prompt specifying visual style, lighting, camera and characters"
  }
]

Script:
$script
        """.trimIndent()

        try {
            val responseText = callGeminiTextModel(prompt, model = "gemini-3.5-flash")
            if (responseText.isNullOrBlank()) {
                val fallback = parseScriptLocally(script, settings)
                return@withContext GeminiResult.Success(fallback)
            }

            val parsedScenes = parseScenesJson(responseText, settings)
            if (parsedScenes.isNotEmpty()) {
                GeminiResult.Success(parsedScenes)
            } else {
                val fallback = parseScriptLocally(script, settings)
                GeminiResult.Success(fallback)
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Scene generation failed", e)
            val fallback = parseScriptLocally(script, settings)
            GeminiResult.Success(fallback)
        }
    }

    /**
     * Generates visual image using gemini-2.5-flash-image
     */
    suspend fun generateSceneVisual(
        scene: SceneItem,
        settings: VideoSettings
    ): GeminiResult<String> = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured) {
            return@withContext GeminiResult.Error(
                "Gemini API key is needed for AI Image generation.",
                "gemini-2.5-flash-image"
            )
        }

        val aspectParam = when (settings.aspectRatio) {
            VideoAspectRatio.PORTRAIT_9_16 -> "9:16"
            VideoAspectRatio.LANDSCAPE_16_9 -> "16:9"
            VideoAspectRatio.SQUARE_1_1 -> "1:1"
        }

        val enrichedPrompt = "${scene.visualPrompt}, ${settings.style.promptModifier}, highly detailed, cinematic lighting, master composition"

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image:generateContent?key=$apiKey"

        val requestJson = JSONObject().apply {
            val contents = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val parts = JSONArray().apply {
                        val part = JSONObject().apply {
                            put("text", enrichedPrompt)
                        }
                        put(part)
                    }
                    put("parts", parts)
                }
                put(contentObj)
            }
            put("contents", contents)

            val genConfig = JSONObject().apply {
                val responseModalities = JSONArray().apply {
                    put("TEXT")
                    put("IMAGE")
                }
                put("responseModalities", responseModalities)
                val imageConfig = JSONObject().apply {
                    put("aspectRatio", aspectParam)
                }
                put("imageConfig", imageConfig)
            }
            put("generationConfig", genConfig)
        }

        try {
            val body = requestJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Log.w("GeminiService", "Image generation API error: ${response.code} $errorBody")
                return@withContext GeminiResult.Error(
                    "Image generation API responded with status ${response.code}. Ensure gemini-2.5-flash-image is enabled for your project.",
                    "gemini-2.5-flash-image"
                )
            }

            val respString = response.body?.string() ?: ""
            val jsonResp = JSONObject(respString)
            val candidates = jsonResp.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        val inlineData = part.optJSONObject("inlineData")
                        if (inlineData != null) {
                            val base64Data = inlineData.optString("data")
                            if (base64Data.isNotEmpty()) {
                                val savedPath = saveBase64Image(base64Data, "scene_${scene.sceneNumber}_${System.currentTimeMillis()}")
                                if (savedPath != null) {
                                    return@withContext GeminiResult.Success(savedPath)
                                }
                            }
                        }
                    }
                }
            }

            GeminiResult.Error(
                "Image generation completed but no image binary was returned. Model may require image generation billing tier.",
                "gemini-2.5-flash-image"
            )
        } catch (e: Exception) {
            Log.e("GeminiService", "Visual generation call error", e)
            GeminiResult.Error(
                "Visual generation error: ${e.localizedMessage ?: e.message}",
                "gemini-2.5-flash-image"
            )
        }
    }

    private fun saveBase64Image(base64Str: String, filename: String): String? {
        return try {
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            val file = File(context.filesDir, "$filename.jpg")
            FileOutputStream(file).use { fos ->
                fos.write(bytes)
                fos.flush()
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("GeminiService", "Failed to write decoded image to file", e)
            null
        }
    }

    private suspend fun callGeminiTextModel(prompt: String, model: String = "gemini-3.5-flash"): String? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val requestJson = JSONObject().apply {
            val contents = JSONArray().apply {
                val content = JSONObject().apply {
                    val parts = JSONArray().apply {
                        val part = JSONObject().apply {
                            put("text", prompt)
                        }
                        put(part)
                    }
                    put("parts", parts)
                }
                put(content)
            }
            put("contents", contents)
            val genConfig = JSONObject().apply {
                put("temperature", 0.7)
            }
            put("generationConfig", genConfig)
        }

        val body = requestJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder().url(url).post(body).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val err = response.body?.string() ?: ""
            Log.w("GeminiService", "Gemini text call failed ${response.code}: $err")
            return null
        }

        val respStr = response.body?.string() ?: return null
        val json = JSONObject(respStr)
        val candidates = json.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null
        val content = candidates.getJSONObject(0).optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null
        return parts.getJSONObject(0).optString("text")
    }

    private fun parseScenesJson(rawText: String, settings: VideoSettings): List<SceneItem> {
        return try {
            var cleaned = rawText.trim()
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.removePrefix("```json").trim()
            }
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.removePrefix("```").trim()
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.removeSuffix("```").trim()
            }

            val startIndex = cleaned.indexOf('[')
            val endIndex = cleaned.lastIndexOf(']')
            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                cleaned = cleaned.substring(startIndex, endIndex + 1)
            }

            val array = JSONArray(cleaned)
            val list = mutableListOf<SceneItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val sceneNum = obj.optInt("sceneNumber", i + 1)
                val sceneDesc = obj.optString("sceneDescription", "Scene $sceneNum")
                val charDesc = obj.optString("characterDescription", "Consistent main character in ${settings.style.displayName}")
                val bgDesc = obj.optString("backgroundDescription", "Detailed atmospheric background")
                val camDir = obj.optString("cameraDirection", "Cinematic eye-level shot")
                val dialogue = obj.optString("dialogue", "")
                val duration = obj.optInt("estimatedDurationSec", 5).coerceIn(3, 15)
                val visPrompt = obj.optString("visualPrompt", "$sceneDesc, ${settings.style.promptModifier}")

                val subs = generateSceneSubtitles(dialogue, duration)

                list.add(
                    SceneItem(
                        sceneNumber = sceneNum,
                        sceneDescription = sceneDesc,
                        characterDescription = charDesc,
                        backgroundDescription = bgDesc,
                        cameraDirection = camDir,
                        dialogue = dialogue,
                        estimatedDurationSec = duration,
                        visualPrompt = visPrompt,
                        subtitles = subs
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e("GeminiService", "Failed to parse scenes JSON: $rawText", e)
            emptyList()
        }
    }

    fun generateSceneSubtitles(dialogue: String, durationSec: Int): List<SubtitleSegment> {
        val trimmed = dialogue.trim()
        if (trimmed.isEmpty()) return emptyList()

        val totalMs = durationSec * 1000L
        val sentences = trimmed.split(Regex("(?<=[.!?।\n])\\s+")).filter { it.isNotBlank() }
        if (sentences.isEmpty()) {
            return listOf(SubtitleSegment(trimmed, 0, totalMs))
        }

        val stepMs = totalMs / sentences.size
        return sentences.mapIndexed { idx, sent ->
            SubtitleSegment(
                text = sent.trim(),
                startMs = idx * stepMs,
                endMs = (idx + 1) * stepMs
            )
        }
    }

    private fun parseScriptLocally(script: String, settings: VideoSettings): List<SceneItem> {
        val lines = script.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val paragraphs = if (lines.size <= 2) {
            script.split(Regex("(?<=[.!?।])\\s+")).filter { it.isNotBlank() }
        } else {
            lines
        }

        val chunks = if (paragraphs.isEmpty()) listOf(script) else paragraphs

        return chunks.take(8).mapIndexed { index, chunk ->
            val sceneNum = index + 1
            val words = chunk.split("\\s+".toRegex()).size
            val estDuration = (words / 2.5f).toInt().coerceIn(4, 8)
            val subs = generateSceneSubtitles(chunk, estDuration)

            SceneItem(
                sceneNumber = sceneNum,
                sceneDescription = "Scene $sceneNum: Narrative key point illustrating the action",
                characterDescription = "Consistent central character stylized in ${settings.style.displayName} matching scenes 1-${chunks.size}",
                backgroundDescription = "Scenic environment appropriate for the narrative",
                cameraDirection = when (sceneNum % 3) {
                    1 -> "Wide establishing view, slowly zooming"
                    2 -> "Medium focus framing main action"
                    else -> "Dramatic close-up with soft background blur"
                },
                dialogue = chunk,
                estimatedDurationSec = estDuration,
                visualPrompt = "${settings.style.promptModifier}, cinematic storyboard scene showing $chunk, consistent characters, vibrant colors, master lighting",
                subtitles = subs
            )
        }
    }
}
