package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ScriptLanguage
import com.example.ui.components.*
import com.example.video.VideoExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Script2VideoApp(
    viewModel: MainViewModel,
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit
) {
    val context = LocalContext.current

    // State observers
    val scriptText by viewModel.scriptText.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scenes by viewModel.scenes.collectAsStateWithLifecycle()
    val generationState by viewModel.generationState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val isImproving by viewModel.isImprovingScript.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val savedProjects by viewModel.savedProjects.collectAsStateWithLifecycle()
    val currentProjectId by viewModel.currentProjectId.collectAsStateWithLifecycle()
    val projectTitle by viewModel.projectTitle.collectAsStateWithLifecycle()

    var showGalleryDialog by remember { mutableStateOf(false) }
    var showProjectsSheet by remember { mutableStateOf(false) }
    var showSceneEditor by remember { mutableStateOf(false) }
    var showSecurityNotice by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showGalleryDialog = true }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MovieFilter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Script2Video AI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = projectTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                actions = {
                    // Project Gallery button
                    FilledTonalButton(
                        onClick = { showGalleryDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("gallery_top_button")
                    ) {
                        Icon(Icons.Default.Collections, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (savedProjects.isNotEmpty()) "Gallery (${savedProjects.size})" else "Gallery",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Demo project button (Requirement 17)
                    FilledTonalButton(
                        onClick = {
                            viewModel.loadDemoProject()
                            Toast.makeText(context, "Loaded Demo Story with ready scenes!", Toast.LENGTH_SHORT).show()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("demo_mode_button")
                    ) {
                        Icon(Icons.Default.PlayCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Demo", style = MaterialTheme.typography.labelSmall)
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // Dark/Light mode toggle (Requirement 12)
                    IconButton(
                        onClick = onToggleDarkTheme,
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Prominent Bottom Generate Video Button (Requirement 10)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = { viewModel.startVideoGeneration() },
                        enabled = scriptText.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("generate_video_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MovieCreation,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "🎬 GENERATE VIDEO",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Workflow Breadcrumb Banner (Requirement 2)
            WorkflowBreadcrumb()

            // Script Input & AI Improver (Requirement 3)
            ScriptEditorSection(
                scriptText = scriptText,
                language = settings.language,
                isImproving = isImproving,
                onScriptChange = { viewModel.setScriptText(it) },
                onClearScript = { viewModel.clearScript() },
                onLoadExample = { viewModel.loadExampleScript() },
                onImproveScript = { viewModel.improveScriptWithAi() }
            )

            // Video & Audio Settings (Requirement 4)
            VideoSettingsSection(
                settings = settings,
                onSettingsChange = { viewModel.updateSettings(it) }
            )

            // Video Preview Player & Actions (Requirement 11)
            Text(
                text = "Video Preview & Timeline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            VideoPlayerView(
                scenes = scenes,
                settings = settings,
                playbackState = playbackState,
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSeekScene = { viewModel.seekToScene(it) },
                onRegenerate = { viewModel.startVideoGeneration() },
                onEditScenes = { showSceneEditor = true },
                onExportVideo = {
                    viewModel.exportVideo { file ->
                        VideoExporter.shareExportedFile(context, file)
                        Toast.makeText(context, "Exported: ${file.name}", Toast.LENGTH_LONG).show()
                    }
                }
            )

            // Saved Projects Gallery Section (Requirement: Grid of saved video projects with thumbnail previews, dates, Open/Delete actions)
            ProjectGallerySection(
                projects = savedProjects,
                currentProjectId = currentProjectId,
                onOpenProject = { id ->
                    viewModel.openProject(id)
                    Toast.makeText(context, "Opened Project!", Toast.LENGTH_SHORT).show()
                },
                onDeleteProject = { id ->
                    viewModel.deleteProject(id)
                    Toast.makeText(context, "Project Deleted", Toast.LENGTH_SHORT).show()
                },
                onRenameProject = { id, title -> viewModel.renameProject(id, title) },
                onNewProject = { viewModel.createNewProject() },
                onSaveCurrentProject = {
                    viewModel.saveCurrentProject()
                    Toast.makeText(context, "Project Saved to Gallery!", Toast.LENGTH_SHORT).show()
                },
                onViewAll = { showGalleryDialog = true }
            )

            // Security Notice Accordion (Requirement 15)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("API Key Security Info", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(onClick = { showSecurityNotice = !showSecurityNotice }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = if (showSecurityNotice) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                    }

                    if (showSecurityNotice) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Security Warning: I have included your API keys in the generated APK file for this prototype. Please be aware that Android APKs can be easily decompiled, and these keys can be extracted by anyone who has access to the file. Do not share this APK file publicly or with unauthorized individuals to prevent potential misuse.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Generation Progress Modal (Requirement 10 & 16)
    GenerationProgressDialog(
        generationState = generationState,
        onRetry = { viewModel.retryGeneration() },
        onDismiss = { viewModel.dismissError() }
    )

    // Scene & Timeline Editor Modal (Requirement 9)
    if (showSceneEditor) {
        SceneEditorDialog(
            scenes = scenes,
            onUpdateScene = { viewModel.updateScene(it) },
            onRegenerateScene = { viewModel.regenerateSceneVisual(it) },
            onDeleteScene = { viewModel.deleteScene(it) },
            onAddScene = { viewModel.addScene(it) },
            onMoveUp = { viewModel.moveSceneUp(it) },
            onMoveDown = { viewModel.moveSceneDown(it) },
            onDismiss = { showSceneEditor = false }
        )
    }

    // Projects Management Drawer (Requirement 13)
    if (showProjectsSheet) {
        ProjectsBottomSheet(
            projects = savedProjects,
            currentProjectId = currentProjectId,
            onSelectProject = { viewModel.openProject(it) },
            onNewProject = { viewModel.createNewProject() },
            onSaveProject = {
                viewModel.saveCurrentProject()
                Toast.makeText(context, "Project Saved!", Toast.LENGTH_SHORT).show()
            },
            onRenameProject = { id, title -> viewModel.renameProject(id, title) },
            onDeleteProject = { id -> viewModel.deleteProject(id) },
            onDismiss = { showProjectsSheet = false }
        )
    }

    // Project Gallery Dialog (Full-screen Grid Gallery)
    if (showGalleryDialog) {
        ProjectGalleryDialog(
            projects = savedProjects,
            currentProjectId = currentProjectId,
            onOpenProject = { id ->
                viewModel.openProject(id)
                Toast.makeText(context, "Opened Project!", Toast.LENGTH_SHORT).show()
            },
            onDeleteProject = { id ->
                viewModel.deleteProject(id)
                Toast.makeText(context, "Project Deleted", Toast.LENGTH_SHORT).show()
            },
            onRenameProject = { id, title -> viewModel.renameProject(id, title) },
            onNewProject = { viewModel.createNewProject() },
            onSaveCurrentProject = {
                viewModel.saveCurrentProject()
                Toast.makeText(context, "Project Saved to Gallery!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showGalleryDialog = false }
        )
    }
}

@Composable
private fun WorkflowBreadcrumb() {
    val steps = listOf(
        "SCRIPT",
        "ANALYZE",
        "SCENES",
        "VISUALS",
        "VOICE",
        "SUBTITLES",
        "COMBINE",
        "PREVIEW",
        "EXPORT"
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            steps.forEachIndexed { index, step ->
                Text(
                    text = step,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                if (index < steps.size - 1) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
