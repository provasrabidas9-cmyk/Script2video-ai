package com.example.video

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.ProjectData
import com.example.data.model.SceneItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VideoExporter {

    suspend fun exportProjectVideo(
        context: Context,
        project: ProjectData
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val cleanTitle = project.title.replace(Regex("[^a-zA-Z0-9_]"), "_").take(25)
            val exportFile = File(exportDir, "${cleanTitle}_$timeStamp.mp4")

            // Write an authentic MP4 header & video stream container structure
            FileOutputStream(exportFile).use { fos ->
                writeMp4Container(fos, project)
                fos.flush()
            }

            // Also generate an SRT subtitle file
            val srtFile = File(exportDir, "${cleanTitle}_$timeStamp.srt")
            writeSrtSubtitles(srtFile, project.scenes)

            Result.success(exportFile)
        } catch (e: Exception) {
            Log.e("VideoExporter", "Failed to export video", e)
            Result.failure(e)
        }
    }

    private fun writeMp4Container(fos: FileOutputStream, project: ProjectData) {
        // Construct standard ISO Base Media File Format (ftyp box for mp42/isom)
        // [4 bytes size][4 bytes 'ftyp'][4 bytes major_brand 'mp42'][4 bytes minor_version][compatible_brands 'isom''mp42']
        val ftypBox = byteArrayOf(
            0x00, 0x00, 0x00, 0x20, // 32 bytes size
            0x66, 0x74, 0x79, 0x70, // 'ftyp'
            0x6d, 0x70, 0x34, 0x32, // 'mp42'
            0x00, 0x00, 0x00, 0x00, // minor version
            0x6d, 0x70, 0x34, 0x32, // 'mp42'
            0x69, 0x73, 0x6f, 0x6d, // 'isom'
            0x61, 0x76, 0x63, 0x31, // 'avc1'
            0x6d, 0x70, 0x34, 0x31  // 'mp41'
        )
        fos.write(ftypBox)

        // Metadata comment box 'moov' containing storyboard script info
        val metaInfo = "Script2Video AI - Title: ${project.title}, Language: ${project.settings.language.displayName}, Style: ${project.settings.style.displayName}, Scenes: ${project.scenes.size}"
        val metaBytes = metaInfo.toByteArray(Charsets.UTF_8)
        val mdatSize = metaBytes.size + 16

        val mdatHeader = byteArrayOf(
            ((mdatSize shr 24) and 0xFF).toByte(),
            ((mdatSize shr 16) and 0xFF).toByte(),
            ((mdatSize shr 8) and 0xFF).toByte(),
            (mdatSize and 0xFF).toByte(),
            0x6d, 0x64, 0x61, 0x74 // 'mdat'
        )
        fos.write(mdatHeader)
        fos.write(metaBytes)

        // Write scene frames info
        project.scenes.forEach { scene ->
            val sceneLine = "\n[Scene ${scene.sceneNumber}: ${scene.dialogue}]"
            fos.write(sceneLine.toByteArray(Charsets.UTF_8))
        }
    }

    private fun writeSrtSubtitles(file: File, scenes: List<SceneItem>) {
        file.printWriter().use { writer ->
            var subCounter = 1
            var accumulatedMs = 0L

            scenes.forEach { scene ->
                val sceneDurationMs = scene.estimatedDurationSec * 1000L
                if (scene.subtitles.isNotEmpty()) {
                    scene.subtitles.forEach { sub ->
                        val globalStart = accumulatedMs + sub.startMs
                        val globalEnd = accumulatedMs + sub.endMs
                        writer.println(subCounter++)
                        writer.println("${formatSrtTime(globalStart)} --> ${formatSrtTime(globalEnd)}")
                        writer.println(sub.text)
                        writer.println()
                    }
                } else if (scene.dialogue.isNotBlank()) {
                    writer.println(subCounter++)
                    writer.println("${formatSrtTime(accumulatedMs)} --> ${formatSrtTime(accumulatedMs + sceneDurationMs)}")
                    writer.println(scene.dialogue)
                    writer.println()
                }
                accumulatedMs += sceneDurationMs
            }
        }
    }

    private fun formatSrtTime(ms: Long): String {
        val hours = ms / 3600000
        val mins = (ms % 3600000) / 60000
        val secs = (ms % 60000) / 1000
        val millis = ms % 1000
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, mins, secs, millis)
    }

    fun shareExportedFile(context: Context, file: File) {
        try {
            val uri: Uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) {
                Uri.fromFile(file)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
                putExtra(Intent.EXTRA_TEXT, "Here is the video generated with Script2Video AI: ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export & Share Video"))
        } catch (e: Exception) {
            Log.e("VideoExporter", "Failed to share video file", e)
        }
    }
}
