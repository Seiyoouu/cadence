package dev.cadence.app.engine

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object MorphePackageResolver {
    private const val TAG = "MorphePackageResolver"
    private const val PREFS_NAME = "cadence_prefs"
    private const val KEY_TARGET_PACKAGE = "target_ytm_package"

    const val PACKAGE_MORPHE = "app.morphe.android.apps.youtube.music"
    const val PACKAGE_REVANCED = "app.revanced.android.apps.youtube.music"
    const val PACKAGE_GOOGLE = "com.google.android.apps.youtube.music"

    val KNOWN_PACKAGES = listOf(
        PACKAGE_MORPHE,
        PACKAGE_REVANCED,
        PACKAGE_GOOGLE
    )

    fun getPreferredPackage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_TARGET_PACKAGE, null)
        if (!saved.isNullOrEmpty() && isPackageInstalled(context, saved)) {
            Log.d(TAG, "Using user-selected target package: $saved")
            return saved
        }

        val pm = context.packageManager

        // Priority 1: Morphe patched package (rootless standard)
        if (isPackageInstalled(context, PACKAGE_MORPHE)) {
            Log.i(TAG, "Found Morphe YTM ($PACKAGE_MORPHE) -> prioritizing it over Google")
            return PACKAGE_MORPHE
        }

        // Priority 2: ReVanced patched package
        if (isPackageInstalled(context, PACKAGE_REVANCED)) {
            Log.i(TAG, "Found ReVanced YTM ($PACKAGE_REVANCED) -> prioritizing it")
            return PACKAGE_REVANCED
        }

        // Priority 3: Any custom non-Google YouTube Music package
        try {
            val installedPackages = pm.getInstalledPackages(0)
            val customPatched = installedPackages.firstOrNull {
                it.packageName.endsWith(".android.apps.youtube.music") && it.packageName != PACKAGE_GOOGLE
            }?.packageName
            if (customPatched != null) {
                Log.i(TAG, "Found custom patched YTM ($customPatched)")
                return customPatched
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query installed packages", e)
        }

        // Priority 4: Default Google / system package
        Log.i(TAG, "Defaulting to Google package ($PACKAGE_GOOGLE)")
        return PACKAGE_GOOGLE
    }

    fun isTargetPackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return packageName in KNOWN_PACKAGES || packageName.endsWith(".android.apps.youtube.music")
    }

    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getInstalledYtmPackages(context: Context): List<String> {
        val list = mutableListOf<String>()
        if (isPackageInstalled(context, PACKAGE_MORPHE)) list.add(PACKAGE_MORPHE)
        if (isPackageInstalled(context, PACKAGE_REVANCED)) list.add(PACKAGE_REVANCED)
        if (isPackageInstalled(context, PACKAGE_GOOGLE)) list.add(PACKAGE_GOOGLE)
        return list
    }

    fun getDisplayName(context: Context, packageName: String): String {
        return when (packageName) {
            PACKAGE_MORPHE -> "YouTube Music (Morphe)"
            PACKAGE_REVANCED -> "YouTube Music (ReVanced)"
            PACKAGE_GOOGLE -> "YouTube Music (Google / Stock)"
            else -> try {
                val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                context.packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                packageName
            }
        }
    }
}
