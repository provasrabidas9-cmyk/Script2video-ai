package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import com.example.data.model.ProjectData

object AppShareUtil {

    const val APP_NAME = "Script2Video AI"
    const val SHARED_APP_URL = "https://ais-pre-c3dyujjx4q2xnxn42i34nm-798927292358.asia-east1.run.app"

    fun getAppShareMessage(project: ProjectData? = null): String {
        return if (project != null && project.title.isNotBlank()) {
            """
🎬 Check out this video project created with Script2Video AI!
📌 Title: ${project.title}
🗣️ Language: ${project.settings.language.displayName} (${project.settings.language.nativeName})
🎞️ Scenes: ${project.scenes.size} | Style: ${project.settings.style.displayName}

Create your own AI script-to-video stories in Bengali, Hindi, & English:
👉 $SHARED_APP_URL
            """.trimIndent()
        } else {
            """
🎬 Check out Script2Video AI!
Create cinematic videos directly from scripts with automatic scene breakdowns, AI voices in Bengali (বাংলা), Hindi (हिन्दी), and English, synced subtitles, and video export.

Try it now:
👉 $SHARED_APP_URL
            """.trimIndent()
        }
    }

    /**
     * Shares the app or active project directly to WhatsApp.
     * If WhatsApp is not installed, gracefully falls back to the system share sheet.
     */
    fun shareViaWhatsApp(context: Context, project: ProjectData? = null) {
        val shareText = getAppShareMessage(project)
        try {
            val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(whatsappIntent)
        } catch (e: Exception) {
            // WhatsApp not installed, try WhatsApp Business or generic share fallback
            try {
                val whatsappBusinessIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage("com.whatsapp.w4b")
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(whatsappBusinessIntent)
            } catch (e2: Exception) {
                Toast.makeText(context, "WhatsApp not installed. Opening share options...", Toast.LENGTH_SHORT).show()
                shareAppLink(context, project)
            }
        }
    }

    /**
     * Standard Android share sheet to share link across all apps (Telegram, Messages, Gmail, Twitter, etc.)
     */
    fun shareAppLink(context: Context, project: ProjectData? = null) {
        val shareText = getAppShareMessage(project)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Script2Video AI - Turn scripts into video")
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Script2Video AI via...")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Copies the app or project link to the clipboard.
     */
    fun copyLinkToClipboard(context: Context, url: String = SHARED_APP_URL) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Script2Video AI Link", url)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
    }
}
