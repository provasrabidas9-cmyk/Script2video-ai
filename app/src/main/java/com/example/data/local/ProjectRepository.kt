package com.example.data.local

import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class ProjectRepository(private val projectDao: ProjectDao) {

    val allProjects: Flow<List<ProjectData>> = projectDao.getAllProjects().map { entities ->
        entities.map { it.toProjectData() }
    }

    suspend fun getProject(id: Long): ProjectData? {
        return projectDao.getProjectById(id)?.toProjectData()
    }

    suspend fun saveProject(project: ProjectData): Long {
        val entity = project.toEntity()
        return projectDao.insertProject(entity)
    }

    suspend fun renameProject(id: Long, newTitle: String) {
        projectDao.renameProject(id, newTitle)
    }

    suspend fun deleteProject(id: Long) {
        projectDao.deleteProjectById(id)
    }

    companion object {
        fun serializeSettings(settings: VideoSettings): String {
            val json = JSONObject()
            json.put("language", settings.language.name)
            json.put("style", settings.style.name)
            json.put("aspectRatio", settings.aspectRatio.name)
            json.put("duration", settings.duration.name)
            json.put("voiceGender", settings.voiceGender.name)
            json.put("voiceSpeed", settings.voiceSpeed.name)
            json.put("subtitleEnabled", settings.subtitleEnabled)
            json.put("subtitlePosition", settings.subtitlePosition.name)
            json.put("subtitleFontSize", settings.subtitleFontSize.name)
            json.put("backgroundMusicEnabled", settings.backgroundMusicEnabled)
            return json.toString()
        }

        fun deserializeSettings(jsonString: String?): VideoSettings {
            if (jsonString.isNullOrEmpty()) return VideoSettings()
            return try {
                val json = JSONObject(jsonString)
                VideoSettings(
                    language = ScriptLanguage.valueOf(json.optString("language", ScriptLanguage.ENGLISH.name)),
                    style = VideoStyle.valueOf(json.optString("style", VideoStyle.CINEMATIC.name)),
                    aspectRatio = VideoAspectRatio.valueOf(json.optString("aspectRatio", VideoAspectRatio.LANDSCAPE_16_9.name)),
                    duration = VideoDurationOption.valueOf(json.optString("duration", VideoDurationOption.AUTO.name)),
                    voiceGender = VoiceGender.valueOf(json.optString("voiceGender", VoiceGender.MALE.name)),
                    voiceSpeed = VoiceSpeed.valueOf(json.optString("voiceSpeed", VoiceSpeed.NORMAL.name)),
                    subtitleEnabled = json.optBoolean("subtitleEnabled", true),
                    subtitlePosition = SubtitlePosition.valueOf(json.optString("subtitlePosition", SubtitlePosition.BOTTOM.name)),
                    subtitleFontSize = SubtitleFontSize.valueOf(json.optString("subtitleFontSize", SubtitleFontSize.MEDIUM.name)),
                    backgroundMusicEnabled = json.optBoolean("backgroundMusicEnabled", true)
                )
            } catch (e: Exception) {
                VideoSettings()
            }
        }

        fun serializeScenes(scenes: List<SceneItem>): String {
            val array = JSONArray()
            scenes.forEach { scene ->
                val obj = JSONObject()
                obj.put("id", scene.id)
                obj.put("sceneNumber", scene.sceneNumber)
                obj.put("sceneDescription", scene.sceneDescription)
                obj.put("characterDescription", scene.characterDescription)
                obj.put("backgroundDescription", scene.backgroundDescription)
                obj.put("cameraDirection", scene.cameraDirection)
                obj.put("dialogue", scene.dialogue)
                obj.put("estimatedDurationSec", scene.estimatedDurationSec)
                obj.put("visualPrompt", scene.visualPrompt)
                obj.put("visualImagePath", scene.visualImagePath ?: "")
                obj.put("audioFilePath", scene.audioFilePath ?: "")

                val subsArray = JSONArray()
                scene.subtitles.forEach { sub ->
                    val subObj = JSONObject()
                    subObj.put("text", sub.text)
                    subObj.put("startMs", sub.startMs)
                    subObj.put("endMs", sub.endMs)
                    subsArray.put(subObj)
                }
                obj.put("subtitles", subsArray)
                array.put(obj)
            }
            return array.toString()
        }

        fun deserializeScenes(jsonString: String?): List<SceneItem> {
            if (jsonString.isNullOrEmpty()) return emptyList()
            return try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<SceneItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val subsArray = obj.optJSONArray("subtitles")
                    val subs = mutableListOf<SubtitleSegment>()
                    if (subsArray != null) {
                        for (s in 0 until subsArray.length()) {
                            val sObj = subsArray.getJSONObject(s)
                            subs.add(
                                SubtitleSegment(
                                    text = sObj.optString("text"),
                                    startMs = sObj.optLong("startMs"),
                                    endMs = sObj.optLong("endMs")
                                )
                            )
                        }
                    }

                    list.add(
                        SceneItem(
                            id = obj.optString("id"),
                            sceneNumber = obj.optInt("sceneNumber", i + 1),
                            sceneDescription = obj.optString("sceneDescription"),
                            characterDescription = obj.optString("characterDescription"),
                            backgroundDescription = obj.optString("backgroundDescription"),
                            cameraDirection = obj.optString("cameraDirection"),
                            dialogue = obj.optString("dialogue"),
                            estimatedDurationSec = obj.optInt("estimatedDurationSec", 4),
                            visualPrompt = obj.optString("visualPrompt"),
                            visualImagePath = obj.optString("visualImagePath").takeIf { it.isNotEmpty() },
                            audioFilePath = obj.optString("audioFilePath").takeIf { it.isNotEmpty() },
                            subtitles = subs
                        )
                    )
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

fun ProjectEntity.toProjectData(): ProjectData {
    return ProjectData(
        id = id,
        title = title,
        scriptText = scriptText,
        settings = ProjectRepository.deserializeSettings(settingsJson),
        scenes = ProjectRepository.deserializeScenes(scenesJson),
        finalVideoPath = finalVideoPath,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun ProjectData.toEntity(): ProjectEntity {
    return ProjectEntity(
        id = id,
        title = title,
        scriptText = scriptText,
        language = settings.language.name,
        settingsJson = ProjectRepository.serializeSettings(settings),
        scenesJson = ProjectRepository.serializeScenes(scenes),
        finalVideoPath = finalVideoPath,
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis()
    )
}
