package com.example.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.BackgroundMusicSynthesizer
import com.example.audio.TtsAudioEngine
import com.example.data.ai.DefaultScripts
import com.example.data.ai.GeminiResult
import com.example.data.ai.GeminiService
import com.example.data.local.AppDatabase
import com.example.data.local.ProjectRepository
import com.example.data.model.*
import com.example.video.VideoExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class GenerationUiState {
    object Idle : GenerationUiState()
    data class Progress(
        val step: GenerationStep,
        val progressPercent: Int,
        val statusMessage: String
    ) : GenerationUiState()
    object Completed : GenerationUiState()
    data class Error(
        val message: String,
        val requiredApi: String? = null,
        val failedStep: GenerationStep? = null
    ) : GenerationUiState()
}

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentSceneIndex: Int = 0,
    val currentSceneElapsedMs: Long = 0L,
    val totalElapsedMs: Long = 0L,
    val currentSubtitleText: String = "",
    val totalDurationSec: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = ProjectRepository(database.projectDao())
    private val geminiService = GeminiService(application)

    // TTS & Music Audio Engines
    private val ttsEngine = TtsAudioEngine(application)
    private val bgmSynthesizer = BackgroundMusicSynthesizer()

    // Script & Settings state
    private val _currentProjectId = MutableStateFlow<Long?>(null)
    val currentProjectId: StateFlow<Long?> = _currentProjectId.asStateFlow()

    private val _projectTitle = MutableStateFlow("Untitled Video Project")
    val projectTitle: StateFlow<String> = _projectTitle.asStateFlow()

    private val _scriptText = MutableStateFlow(DefaultScripts.getSampleScript(ScriptLanguage.ENGLISH))
    val scriptText: StateFlow<String> = _scriptText.asStateFlow()

    private val _settings = MutableStateFlow(VideoSettings())
    val settings: StateFlow<VideoSettings> = _settings.asStateFlow()

    private val _scenes = MutableStateFlow<List<SceneItem>>(emptyList())
    val scenes: StateFlow<List<SceneItem>> = _scenes.asStateFlow()

    // Generation State
    private val _generationState = MutableStateFlow<GenerationUiState>(GenerationUiState.Idle)
    val generationState: StateFlow<GenerationUiState> = _generationState.asStateFlow()

    // Playback State
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    // UI flags
    private val _isImprovingScript = MutableStateFlow(false)
    val isImprovingScript: StateFlow<Boolean> = _isImprovingScript.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportedVideoFile = MutableStateFlow<File?>(null)
    val exportedVideoFile: StateFlow<File?> = _exportedVideoFile.asStateFlow()

    // Projects list from Room
    val savedProjects: StateFlow<List<ProjectData>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Playback coroutine ticker
    private var playbackJob: Job? = null

    init {
        // Load initial demo scenes for English so user has immediate preview capability
        loadDemoProject()
    }

    fun setScriptText(text: String) {
        _scriptText.value = text
    }

    fun setProjectTitle(title: String) {
        _projectTitle.value = title
    }

    fun clearScript() {
        _scriptText.value = ""
    }

    fun setLanguage(language: ScriptLanguage) {
        val currentLang = _settings.value.language
        if (currentLang != language) {
            _settings.value = _settings.value.copy(language = language)
            // Update script sample if empty or user wants language sample
            if (_scriptText.value.isBlank() || _scriptText.value == DefaultScripts.getSampleScript(currentLang)) {
                _scriptText.value = DefaultScripts.getSampleScript(language)
            }
        }
    }

    fun updateSettings(newSettings: VideoSettings) {
        _settings.value = newSettings
    }

    fun loadExampleScript() {
        val lang = _settings.value.language
        _scriptText.value = DefaultScripts.getSampleScript(lang)
    }

    fun loadDemoProject() {
        val lang = _settings.value.language
        val sample = DefaultScripts.getSampleScript(lang)
        _scriptText.value = sample
        val demoScenes = DefaultScripts.getDemoScenes(lang)
        _scenes.value = demoScenes
        _projectTitle.value = when (lang) {
            ScriptLanguage.BENGALI -> "চতুর শিয়াল ও রোবটের গল্প"
            ScriptLanguage.HINDI -> "चालाक लोमड़ी और नटखट रोबोट"
            ScriptLanguage.ENGLISH -> "Felix the Fox & Bolt"
        }
        recalculatePlaybackDuration()
    }

    fun improveScriptWithAi() {
        val text = _scriptText.value.trim()
        if (text.isEmpty() || _isImprovingScript.value) return

        _isImprovingScript.value = true
        viewModelScope.launch {
            when (val result = geminiService.improveScript(text, _settings.value.language)) {
                is GeminiResult.Success -> {
                    _scriptText.value = result.data
                }
                is GeminiResult.Error -> {
                    _generationState.value = GenerationUiState.Error(
                        message = result.message,
                        requiredApi = result.requiredApi
                    )
                }
            }
            _isImprovingScript.value = false
        }
    }

    fun startVideoGeneration() {
        val script = _scriptText.value.trim()
        if (script.isEmpty()) return

        stopPlayback()
        viewModelScope.launch {
            runGenerationWorkflow(script)
        }
    }

    fun retryGeneration() {
        _generationState.value = GenerationUiState.Idle
        startVideoGeneration()
    }

    fun dismissError() {
        _generationState.value = GenerationUiState.Idle
    }

    private suspend fun runGenerationWorkflow(script: String) {
        val currentSettings = _settings.value

        // Step 1: Analyzing script (15%)
        _generationState.value = GenerationUiState.Progress(
            step = GenerationStep.ANALYZING_SCRIPT,
            progressPercent = 15,
            statusMessage = "Analyzing pacing, narrative arcs, and character motivations in ${currentSettings.language.displayName}..."
        )
        delay(700)

        // Step 2: Creating scenes (30%)
        _generationState.value = GenerationUiState.Progress(
            step = GenerationStep.CREATING_SCENES,
            progressPercent = 30,
            statusMessage = "Breaking story into cinematic shots with character consistency..."
        )

        val scenesResult = geminiService.divideIntoScenes(script, currentSettings)
        val generatedScenes = when (scenesResult) {
            is GeminiResult.Success -> scenesResult.data
            is GeminiResult.Error -> {
                _generationState.value = GenerationUiState.Error(
                    message = scenesResult.message,
                    requiredApi = scenesResult.requiredApi,
                    failedStep = GenerationStep.CREATING_SCENES
                )
                return
            }
        }

        if (generatedScenes.isEmpty()) {
            _generationState.value = GenerationUiState.Error(
                message = "Failed to divide script into scenes. Please try again.",
                failedStep = GenerationStep.CREATING_SCENES
            )
            return
        }

        _scenes.value = generatedScenes

        // Step 3: Generating visuals (50%)
        _generationState.value = GenerationUiState.Progress(
            step = GenerationStep.GENERATING_VISUALS,
            progressPercent = 50,
            statusMessage = "Generating visual concepts in ${currentSettings.style.displayName} style..."
        )

        // Generate visuals for scenes if API key is present
        val updatedScenes = generatedScenes.mapIndexed { index, scene ->
            _generationState.value = GenerationUiState.Progress(
                step = GenerationStep.GENERATING_VISUALS,
                progressPercent = 30 + ((index + 1) * 20 / generatedScenes.size),
                statusMessage = "Visualizing Scene ${scene.sceneNumber}/${generatedScenes.size}: ${scene.cameraDirection}..."
            )

            if (geminiService.isApiKeyConfigured) {
                when (val imgResult = geminiService.generateSceneVisual(scene, currentSettings)) {
                    is GeminiResult.Success -> scene.copy(visualImagePath = imgResult.data)
                    is GeminiResult.Error -> {
                        // Notice of fallback
                        Log.d("MainViewModel", "Visual gen notice: ${imgResult.message}")
                        scene
                    }
                }
            } else {
                delay(300)
                scene
            }
        }
        _scenes.value = updatedScenes

        // Step 4: Generating voice (70%)
        _generationState.value = GenerationUiState.Progress(
            step = GenerationStep.GENERATING_VOICE,
            progressPercent = 70,
            statusMessage = "Configuring ${currentSettings.voiceGender.displayName} voice in ${currentSettings.language.displayName} (${currentSettings.voiceSpeed.displayName})..."
        )
        ttsEngine.applySettings(currentSettings.language, currentSettings.voiceGender, currentSettings.voiceSpeed)
        delay(600)

        // Step 5: Creating subtitles (85%)
        _generationState.value = GenerationUiState.Progress(
            step = GenerationStep.CREATING_SUBTITLES,
            progressPercent = 85,
            statusMessage = "Synchronizing localized dialogue subtitles..."
        )
        val finalizedScenes = updatedScenes.map { scene ->
            if (scene.subtitles.isEmpty()) {
                scene.copy(subtitles = geminiService.generateSceneSubtitles(scene.dialogue, scene.estimatedDurationSec))
            } else {
                scene
            }
        }
        _scenes.value = finalizedScenes
        delay(500)

        // Step 6: Combining video (95%)
        _generationState.value = GenerationUiState.Progress(
            step = GenerationStep.COMBINING_VIDEO,
            progressPercent = 95,
            statusMessage = "Blending visuals, dynamic camera movement, and audio timeline..."
        )
        delay(600)

        // Step 7: Finalizing video (100%)
        _generationState.value = GenerationUiState.Progress(
            step = GenerationStep.FINALIZING_VIDEO,
            progressPercent = 100,
            statusMessage = "Ready for preview and export!"
        )
        delay(400)

        _generationState.value = GenerationUiState.Completed
        recalculatePlaybackDuration()

        // Auto-save project
        saveCurrentProject()

        // Automatically start video playback
        startPlayback()
    }

    // Playback control
    fun togglePlayPause() {
        if (_playbackState.value.isPlaying) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    fun startPlayback() {
        val currentScenes = _scenes.value
        if (currentScenes.isEmpty()) return

        stopPlayback()

        if (_settings.value.backgroundMusicEnabled) {
            bgmSynthesizer.start()
        }

        ttsEngine.applySettings(_settings.value.language, _settings.value.voiceGender, _settings.value.voiceSpeed)

        _playbackState.value = _playbackState.value.copy(isPlaying = true)

        playbackJob = viewModelScope.launch(Dispatchers.Default) {
            var sceneIdx = _playbackState.value.currentSceneIndex
            if (sceneIdx >= currentScenes.size) {
                sceneIdx = 0
            }

            while (sceneIdx < currentScenes.size && _playbackState.value.isPlaying) {
                val scene = currentScenes[sceneIdx]
                val sceneDurationMs = scene.estimatedDurationSec * 1000L

                // Speak dialogue if present
                withContext(Dispatchers.Main) {
                    if (scene.dialogue.isNotBlank()) {
                        ttsEngine.speak(scene.dialogue)
                    }
                }

                var elapsed = 0L
                val tickStep = 100L
                while (elapsed < sceneDurationMs && _playbackState.value.isPlaying) {
                    delay(tickStep)
                    elapsed += tickStep

                    // Determine active subtitle
                    val activeSub = scene.subtitles.firstOrNull { sub ->
                        elapsed in sub.startMs..sub.endMs
                    }?.text ?: scene.dialogue

                    val totalElapsed = calculateTotalElapsed(sceneIdx, elapsed)

                    withContext(Dispatchers.Main) {
                        _playbackState.value = _playbackState.value.copy(
                            currentSceneIndex = sceneIdx,
                            currentSceneElapsedMs = elapsed,
                            totalElapsedMs = totalElapsed,
                            currentSubtitleText = activeSub
                        )
                    }
                }

                sceneIdx++
                if (sceneIdx >= currentScenes.size) {
                    // Loop or stop
                    break
                }
            }

            withContext(Dispatchers.Main) {
                stopPlayback()
            }
        }
    }

    fun pausePlayback() {
        ttsEngine.stop()
        bgmSynthesizer.stop()
        playbackJob?.cancel()
        playbackJob = null
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
    }

    fun stopPlayback() {
        ttsEngine.stop()
        bgmSynthesizer.stop()
        playbackJob?.cancel()
        playbackJob = null
        _playbackState.value = _playbackState.value.copy(
            isPlaying = false,
            currentSceneElapsedMs = 0L,
            currentSubtitleText = ""
        )
    }

    fun seekToScene(index: Int) {
        val currentScenes = _scenes.value
        if (index in currentScenes.indices) {
            val wasPlaying = _playbackState.value.isPlaying
            pausePlayback()
            val totalElapsed = calculateTotalElapsed(index, 0L)
            _playbackState.value = _playbackState.value.copy(
                currentSceneIndex = index,
                currentSceneElapsedMs = 0L,
                totalElapsedMs = totalElapsed,
                currentSubtitleText = currentScenes[index].dialogue
            )
            if (wasPlaying) {
                startPlayback()
            }
        }
    }

    private fun calculateTotalElapsed(sceneIndex: Int, sceneElapsedMs: Long): Long {
        val currentScenes = _scenes.value
        var sum = 0L
        for (i in 0 until sceneIndex.coerceAtMost(currentScenes.size)) {
            sum += currentScenes[i].estimatedDurationSec * 1000L
        }
        return sum + sceneElapsedMs
    }

    private fun recalculatePlaybackDuration() {
        val totalSec = _scenes.value.sumOf { it.estimatedDurationSec }
        _playbackState.value = _playbackState.value.copy(
            totalDurationSec = totalSec
        )
    }

    // Scene Editor Actions (Requirement 9)
    fun updateScene(updatedScene: SceneItem) {
        val current = _scenes.value.toMutableList()
        val index = current.indexOfFirst { it.id == updatedScene.id }
        if (index != -1) {
            current[index] = updatedScene
            _scenes.value = current
            recalculatePlaybackDuration()
        }
    }

    fun regenerateSceneVisual(sceneId: String) {
        val current = _scenes.value.toMutableList()
        val index = current.indexOfFirst { it.id == sceneId }
        if (index != -1) {
            val targetScene = current[index]
            viewModelScope.launch {
                when (val res = geminiService.generateSceneVisual(targetScene, _settings.value)) {
                    is GeminiResult.Success -> {
                        current[index] = targetScene.copy(visualImagePath = res.data)
                        _scenes.value = current
                    }
                    is GeminiResult.Error -> {
                        _generationState.value = GenerationUiState.Error(
                            message = "Regenerate visual failed: ${res.message}",
                            requiredApi = res.requiredApi
                        )
                    }
                }
            }
        }
    }

    fun deleteScene(sceneId: String) {
        val current = _scenes.value.filter { it.id != sceneId }
        // Re-index scene numbers
        val reindexed = current.mapIndexed { idx, item -> item.copy(sceneNumber = idx + 1) }
        _scenes.value = reindexed
        recalculatePlaybackDuration()
    }

    fun addScene(afterIndex: Int = _scenes.value.size - 1) {
        val current = _scenes.value.toMutableList()
        val newNum = (afterIndex + 2).coerceAtLeast(1)
        val newScene = SceneItem(
            sceneNumber = newNum,
            sceneDescription = "New Scene $newNum",
            characterDescription = "Consistent main character in ${_settings.value.style.displayName}",
            backgroundDescription = "Scenic environment backdrop",
            cameraDirection = "Medium tracking shot",
            dialogue = "Enter dialogue or narration here...",
            estimatedDurationSec = 4,
            visualPrompt = "${_settings.value.style.promptModifier}, cinematic new scene, high detail"
        )
        if (afterIndex in current.indices) {
            current.add(afterIndex + 1, newScene)
        } else {
            current.add(newScene)
        }
        val reindexed = current.mapIndexed { idx, item -> item.copy(sceneNumber = idx + 1) }
        _scenes.value = reindexed
        recalculatePlaybackDuration()
    }

    fun moveSceneUp(sceneId: String) {
        val current = _scenes.value.toMutableList()
        val index = current.indexOfFirst { it.id == sceneId }
        if (index > 0) {
            val item = current.removeAt(index)
            current.add(index - 1, item)
            _scenes.value = current.mapIndexed { idx, s -> s.copy(sceneNumber = idx + 1) }
        }
    }

    fun moveSceneDown(sceneId: String) {
        val current = _scenes.value.toMutableList()
        val index = current.indexOfFirst { it.id == sceneId }
        if (index in 0 until current.size - 1) {
            val item = current.removeAt(index)
            current.add(index + 1, item)
            _scenes.value = current.mapIndexed { idx, s -> s.copy(sceneNumber = idx + 1) }
        }
    }

    // Project Management (Requirement 13)
    fun createNewProject() {
        stopPlayback()
        _currentProjectId.value = null
        _projectTitle.value = "New Video Project"
        _scriptText.value = ""
        _scenes.value = emptyList()
        _playbackState.value = PlaybackState()
        _generationState.value = GenerationUiState.Idle
    }

    fun saveCurrentProject() {
        val title = _projectTitle.value.ifBlank { "Untitled Project" }
        val project = ProjectData(
            id = _currentProjectId.value ?: 0L,
            title = title,
            scriptText = _scriptText.value,
            settings = _settings.value,
            scenes = _scenes.value,
            finalVideoPath = _exportedVideoFile.value?.absolutePath
        )
        viewModelScope.launch {
            val savedId = repository.saveProject(project)
            _currentProjectId.value = savedId
        }
    }

    fun openProject(projectId: Long) {
        stopPlayback()
        viewModelScope.launch {
            val project = repository.getProject(projectId) ?: return@launch
            _currentProjectId.value = project.id
            _projectTitle.value = project.title
            _scriptText.value = project.scriptText
            _settings.value = project.settings
            _scenes.value = project.scenes
            recalculatePlaybackDuration()
            _generationState.value = GenerationUiState.Idle
        }
    }

    fun renameProject(projectId: Long, newTitle: String) {
        viewModelScope.launch {
            repository.renameProject(projectId, newTitle)
            if (_currentProjectId.value == projectId) {
                _projectTitle.value = newTitle
            }
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            if (_currentProjectId.value == projectId) {
                createNewProject()
            }
        }
    }

    // Export Video
    fun exportVideo(onReady: (File) -> Unit) {
        val currentScenes = _scenes.value
        if (currentScenes.isEmpty()) return

        _isExporting.value = true
        viewModelScope.launch {
            val project = ProjectData(
                id = _currentProjectId.value ?: 0L,
                title = _projectTitle.value,
                scriptText = _scriptText.value,
                settings = _settings.value,
                scenes = currentScenes
            )
            val result = VideoExporter.exportProjectVideo(getApplication(), project)
            _isExporting.value = false
            result.onSuccess { file ->
                _exportedVideoFile.value = file
                onReady(file)
            }.onFailure { err ->
                _generationState.value = GenerationUiState.Error(
                    message = "Export failed: ${err.localizedMessage ?: err.message}"
                )
            }
        }
    }

    fun getCurrentProjectData(): ProjectData {
        return ProjectData(
            id = _currentProjectId.value ?: 0L,
            title = _projectTitle.value,
            scriptText = _scriptText.value,
            settings = _settings.value,
            scenes = _scenes.value
        )
    }

    override fun onCleared() {
        super.onCleared()
        ttsEngine.release()
        bgmSynthesizer.stop()
    }
}
