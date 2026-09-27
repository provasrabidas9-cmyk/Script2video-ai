package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoSettingsSection(
    settings: VideoSettings,
    onSettingsChange: (VideoSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("video_settings_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Video settings",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Video & Audio Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Language Selection (Requirement 1: Bengali, Hindi, English)
            Text(
                text = "Language",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ScriptLanguage.values().forEach { lang ->
                    FilterChip(
                        selected = settings.language == lang,
                        onClick = { onSettingsChange(settings.copy(language = lang)) },
                        label = { Text(lang.fullLabel) },
                        leadingIcon = if (settings.language == lang) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("lang_chip_${lang.code}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Video Style Selection (Requirement 4)
            Text(
                text = "Video Style",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VideoStyle.values().forEach { style ->
                    FilterChip(
                        selected = settings.style == style,
                        onClick = { onSettingsChange(settings.copy(style = style)) },
                        label = { Text(style.displayName) },
                        leadingIcon = if (settings.style == style) {
                            { Icon(Icons.Default.MovieFilter, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("style_chip_${style.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Aspect Ratio Selection (Requirement 4: 9:16 Portrait, 16:9 Landscape, 1:1 Square)
            Text(
                text = "Aspect Ratio",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VideoAspectRatio.values().forEach { ratio ->
                    FilterChip(
                        selected = settings.aspectRatio == ratio,
                        onClick = { onSettingsChange(settings.copy(aspectRatio = ratio)) },
                        label = { Text(ratio.displayName) },
                        leadingIcon = if (settings.aspectRatio == ratio) {
                            { Icon(Icons.Default.AspectRatio, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("aspect_ratio_${ratio.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Duration Selection (Requirement 4: Auto, 1m, 2m, 3m, 5m)
            Text(
                text = "Target Duration",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VideoDurationOption.values().forEach { dur ->
                    FilterChip(
                        selected = settings.duration == dur,
                        onClick = { onSettingsChange(settings.copy(duration = dur)) },
                        label = { Text(dur.displayName) },
                        leadingIcon = if (settings.duration == dur) {
                            { Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("duration_${dur.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Voice & Speed Controls (Requirement 4)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Voice Gender",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VoiceGender.values().forEach { gender ->
                            FilterChip(
                                selected = settings.voiceGender == gender,
                                onClick = { onSettingsChange(settings.copy(voiceGender = gender)) },
                                label = { Text(gender.displayName) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (gender == VoiceGender.MALE) Icons.Default.RecordVoiceOver else Icons.Default.SpatialAudio,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("voice_gender_${gender.name.lowercase()}")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Voice Speed",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        VoiceSpeed.values().forEach { speed ->
                            FilterChip(
                                selected = settings.voiceSpeed == speed,
                                onClick = { onSettingsChange(settings.copy(voiceSpeed = speed)) },
                                label = { Text(speed.displayName) },
                                modifier = Modifier.testTag("voice_speed_${speed.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Subtitles & Background Music Toggles (Requirement 4 & 8)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Subtitles", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Switch(
                    checked = settings.subtitleEnabled,
                    onCheckedChange = { onSettingsChange(settings.copy(subtitleEnabled = it)) },
                    modifier = Modifier.testTag("subtitle_toggle_switch")
                )
            }

            AnimatedVisibility(visible = settings.subtitleEnabled) {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Subtitle position
                        Column {
                            Text("Position", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                SubtitlePosition.values().forEach { pos ->
                                    FilterChip(
                                        selected = settings.subtitlePosition == pos,
                                        onClick = { onSettingsChange(settings.copy(subtitlePosition = pos)) },
                                        label = { Text(pos.displayName, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }

                        // Subtitle font size
                        Column {
                            Text("Font Size", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                SubtitleFontSize.values().forEach { size ->
                                    FilterChip(
                                        selected = settings.subtitleFontSize == size,
                                        onClick = { onSettingsChange(settings.copy(subtitleFontSize = size)) },
                                        label = { Text(size.displayName, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Background Music", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Switch(
                    checked = settings.backgroundMusicEnabled,
                    onCheckedChange = { onSettingsChange(settings.copy(backgroundMusicEnabled = it)) },
                    modifier = Modifier.testTag("bgm_toggle_switch")
                )
            }
        }
    }
}
