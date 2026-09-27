package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SceneItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SceneEditorDialog(
    scenes: List<SceneItem>,
    onUpdateScene: (SceneItem) -> Unit,
    onRegenerateScene: (String) -> Unit,
    onDeleteScene: (String) -> Unit,
    onAddScene: (Int) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var editingSceneId by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Timeline & Scene Editor (${scenes.size} Scenes)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close editor")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { onAddScene(scenes.size - 1) },
                            modifier = Modifier.testTag("add_scene_top_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Scene")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(scenes, key = { _, s -> s.id }) { index, scene ->
                    SceneEditorCard(
                        scene = scene,
                        isFirst = index == 0,
                        isLast = index == scenes.size - 1,
                        isExpanded = editingSceneId == scene.id,
                        onToggleExpand = {
                            editingSceneId = if (editingSceneId == scene.id) null else scene.id
                        },
                        onUpdate = onUpdateScene,
                        onRegenerate = { onRegenerateScene(scene.id) },
                        onDelete = { onDeleteScene(scene.id) },
                        onAddAfter = { onAddScene(index) },
                        onMoveUp = { onMoveUp(scene.id) },
                        onMoveDown = { onMoveDown(scene.id) }
                    )
                }

                item {
                    Button(
                        onClick = { onAddScene(scenes.size - 1) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .testTag("bottom_add_scene_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add New Scene")
                    }
                }
            }
        }
    }
}

@Composable
private fun SceneEditorCard(
    scene: SceneItem,
    isFirst: Boolean,
    isLast: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onUpdate: (SceneItem) -> Unit,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit,
    onAddAfter: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var dialogueText by remember(scene.dialogue) { mutableStateOf(scene.dialogue) }
    var promptText by remember(scene.visualPrompt) { mutableStateOf(scene.visualPrompt) }
    var cameraText by remember(scene.cameraDirection) { mutableStateOf(scene.cameraDirection) }
    var durationSec by remember(scene.estimatedDurationSec) { mutableStateOf(scene.estimatedDurationSec) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scene_editor_card_${scene.sceneNumber}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Scene number, reorder buttons, expand toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Scene ${scene.sceneNumber}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${durationSec}s",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Reordering & Actions
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = !isFirst,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = !isLast,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete scene", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle expand",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dialogue / Narration field (Always editable)
            Text(
                text = "Narration / Dialogue",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = dialogueText,
                onValueChange = {
                    dialogueText = it
                    onUpdate(scene.copy(dialogue = it))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                minLines = 2,
                maxLines = 4
            )

            // Expanded detail fields: Prompt, Camera, Duration, Regenerate
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Visual Generation Prompt",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = promptText,
                    onValueChange = {
                        promptText = it
                        onUpdate(scene.copy(visualPrompt = it))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Camera Direction",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = cameraText,
                            onValueChange = {
                                cameraText = it
                                onUpdate(scene.copy(cameraDirection = it))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(0.7f)) {
                        Text(
                            text = "Duration: ${durationSec}s",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = durationSec.toFloat(),
                            onValueChange = {
                                durationSec = it.toInt()
                                onUpdate(scene.copy(estimatedDurationSec = durationSec))
                            },
                            valueRange = 2f..15f,
                            steps = 13
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onRegenerate,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Regenerate Visual")
                    }

                    OutlinedButton(
                        onClick = onAddAfter,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Insert Scene Below")
                    }
                }
            }
        }
    }
}
