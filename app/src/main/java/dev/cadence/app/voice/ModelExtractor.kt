package dev.cadence.app.voice

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object ModelExtractor {
    private const val TAG = "ModelExtractor"

    fun extractIfNeeded(context: Context, modelName: String): File {
        val targetDir = File(context.filesDir, "vosk/$modelName")
        val marker = File(targetDir, ".extracted")
        if (marker.exists()) {
            Log.i(TAG, "Model $modelName already extracted at ${targetDir.absolutePath}")
            return targetDir
        }
        Log.i(TAG, "Extracting model $modelName from assets...")
        targetDir.deleteRecursively()
        targetDir.mkdirs()
        copyAssetsRecursively(context, "model-$modelName", targetDir)
        marker.createNewFile()
        Log.i(TAG, "Model $modelName extracted to ${targetDir.absolutePath}")
        return targetDir
    }

    private fun copyAssetsRecursively(context: Context, assetPath: String, targetFileOrDir: File) {
        val children = context.assets.list(assetPath)
        if (children.isNullOrEmpty()) {
            try {
                context.assets.open(assetPath).use { input ->
                    targetFileOrDir.parentFile?.mkdirs()
                    FileOutputStream(targetFileOrDir).use { output -> input.copyTo(output) }
                }
            } catch (e: Exception) {
                targetFileOrDir.mkdirs()
            }
        } else {
            targetFileOrDir.mkdirs()
            for (child in children) {
                copyAssetsRecursively(context, "$assetPath/$child", File(targetFileOrDir, child))
            }
        }
    }
}
