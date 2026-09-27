package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.PlaybackState
import java.io.File

@Composable
fun VideoPlayerView(
    scenes: List<SceneItem>,
    settings: VideoSettings,
    playbackState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onSeekScene: (Int) -> Unit,
    onRegenerate: () -> Unit,
    onEditScenes: () -> Unit,
    onExportVideo: () -> Unit,
    onShare: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (scenes.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MovieFilter,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your generated video will appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    val currentSceneIndex = playbackState.currentSceneIndex.coerceIn(0, scenes.size - 1)
    val activeScene = scenes[currentSceneIndex]

    // Ken Burns slow pan/zoom animation effect during playback
    val infiniteTransition = rememberInfiniteTransition(label = "ken_burns")
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "zoom_anim"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("video_player_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // Top Status Bar inside Player
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (playbackState.isPlaying) Color.Green else Color.Yellow)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scene ${activeScene.sceneNumber} of ${scenes.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${settings.style.displayName} • ${settings.aspectRatio.ratioLabel}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Aspect Ratio Constrained Video Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(settings.aspectRatio.ratio)
                    .background(Color(0xFF101018))
                    .clip(RoundedCornerShape(0.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Visual Content: Bitmap Image if exists, else artistic scene frame
                val bitmap = remember(activeScene.visualImagePath) {
                    activeScene.visualImagePath?.let { path ->
                        val f = File(path)
                        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
                    }
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = activeScene.sceneDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(if (playbackState.isPlaying) scaleAnim else 1.0f)
                    )
                } else {
                    // Stylized cinematic scene card
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF1E1B4B),
                                        Color(0xFF0F172A),
                                        Color(0xFF1E293B)
                                    )
                                )
                            )
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "SCENE ${activeScene.sceneNumber}",
                                color = Color(0xFFA5B4FC),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = activeScene.cameraDirection,
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = activeScene.backgroundDescription,
                                color = Color.LightGray.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }

                // Subtitle Overlay (Requirement 8)
                if (settings.subtitleEnabled) {
                    val subtitleAlignment = when (settings.subtitlePosition) {
                        SubtitlePosition.TOP -> Alignment.TopCenter
                        SubtitlePosition.CENTER -> Alignment.Center
                        SubtitlePosition.BOTTOM -> Alignment.BottomCenter
                    }

                    val subText = playbackState.currentSubtitleText.ifBlank { activeScene.dialogue }
                    if (subText.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = subtitleAlignment
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.75f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = subText,
                                    color = Color.Yellow,
                                    fontSize = settings.subtitleFontSize.spSize.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Center Play/Pause Overlay Button on Tap
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onTogglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    if (!playbackState.isPlaying) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play video",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Timeline Scrubber & Progress Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF14141E))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Time elapsed vs total duration
                val currentSeconds = (playbackState.totalElapsedMs / 1000).toInt()
                val totalSeconds = playbackState.totalDurationSec
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTimecode(currentSeconds),
                        color = Color.LightGray,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = formatTimecode(totalSeconds),
                        color = Color.LightGray,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Progress Slider
                val progressFraction = if (totalSeconds > 0) {
                    (currentSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
                } else 0f

                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Scene Selector Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(scenes) { index, scene ->
                        val isSelected = index == currentSceneIndex
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                            modifier = Modifier
                                .clickable { onSeekScene(index) }
                                .testTag("scene_pill_$index")
                        ) {
                            Text(
                                text = "Scene ${scene.sceneNumber} (${scene.estimatedDurationSec}s)",
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Playback Controls Row (Requirement 11)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Play/Pause button
                    Button(
                        onClick = onTogglePlayPause,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (playbackState.isPlaying) "Pause" else "Play")
                    }

                    // Secondary actions: Regenerate, Edit, Export
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalIconButton(
                            onClick = onRegenerate,
                            modifier = Modifier.testTag("regenerate_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate", modifier = Modifier.size(18.dp))
                        }

                        FilledTonalIconButton(
                            onClick = onEditScenes,
                            modifier = Modifier.testTag("edit_scenes_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit scenes", modifier = Modifier.size(18.dp))
                        }

                        FilledTonalIconButton(
                            onClick = onShare,
                            modifier = Modifier.testTag("share_video_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share App / Project", modifier = Modifier.size(18.dp))
                        }

                        Button(
                            onClick = onExportVideo,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.testTag("export_video_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export MP4", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimecode(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
