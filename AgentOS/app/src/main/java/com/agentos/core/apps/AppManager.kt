package com.agentos.core.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log

/**
 * Manages installed applications discovery and launching.
 * Provides intelligent app resolution for the WorkflowEngine.
 */
class AppManager(private val context: Context) {

    companion object {
        private const val TAG = "AppManager"
    }

    // Cached list of installed apps
    private var installedApps: List<InstalledApp> = emptyList()
    private var lastRefreshTime: Long = 0
    private val cacheValidityMs = 60_000L // 1 minute cache

    /**
     * Get all installed apps that can be launched.
     * Results are cached for performance.
     */
    fun getInstalledApps(forceRefresh: Boolean = false): List<InstalledApp> {
        val now = System.currentTimeMillis()
        if (!forceRefresh && installedApps.isNotEmpty() && (now - lastRefreshTime) < cacheValidityMs) {
            return installedApps
        }

        Log.d(TAG, "Refreshing installed apps list...")
        val pm = context.packageManager

        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val apps = pm.queryIntentActivities(mainIntent, 0)
            .mapNotNull { resolveInfo ->
                try {
                    val appInfo = resolveInfo.activityInfo.applicationInfo
                    val packageName = appInfo.packageName
                    val appName = pm.getApplicationLabel(appInfo).toString()

                    // Skip system apps that aren't useful (optional filter)
                    if (isSystemApp(appInfo) && !isUsefulSystemApp(packageName)) {
                        return@mapNotNull null
                    }

                    InstalledApp(
                        packageName = packageName,
                        appName = appName,
                        activityName = resolveInfo.activityInfo.name,
                        isSystemApp = isSystemApp(appInfo),
                        category = categorizeApp(packageName, appName)
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error processing app: ${e.message}")
                    null
                }
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }

        installedApps = apps
        lastRefreshTime = now
        Log.d(TAG, "Found ${apps.size} launchable apps")
        return apps
    }

    /**
     * Search for apps by name or keyword.
     * Returns apps matching the query, sorted by relevance.
     */
    fun searchApps(query: String): List<InstalledApp> {
        val normalizedQuery = query.lowercase().trim()
        if (normalizedQuery.isEmpty()) return emptyList()

        return getInstalledApps().filter { app ->
            app.appName.lowercase().contains(normalizedQuery) ||
            app.packageName.lowercase().contains(normalizedQuery) ||
            matchesCommonAlias(app, normalizedQuery)
        }.sortedByDescending { app ->
            // Prioritize exact matches and starts-with matches
            when {
                app.appName.lowercase() == normalizedQuery -> 100
                app.appName.lowercase().startsWith(normalizedQuery) -> 80
                app.appName.lowercase().contains(normalizedQuery) -> 60
                matchesCommonAlias(app, normalizedQuery) -> 50
                else -> 10
            }
        }
    }

    /**
     * Find an app by exact or fuzzy package name match.
     */
    fun findAppByPackage(packageName: String): InstalledApp? {
        return getInstalledApps().find {
            it.packageName.equals(packageName, ignoreCase = true)
        }
    }

    /**
     * Get the best app for a given intent/action.
     * Returns the app if installed, null if should use browser.
     */
    fun getBestAppForAction(action: String): AppRecommendation {
        val normalizedAction = action.lowercase()

        // Check for specific app requests
        val directMatch = searchApps(normalizedAction).firstOrNull()
        if (directMatch != null) {
            return AppRecommendation(
                type = RecommendationType.USE_APP,
                app = directMatch,
                reason = "App '${directMatch.appName}' is installed"
            )
        }

        // Check for service-specific apps
        val serviceApp = findServiceApp(normalizedAction)
        if (serviceApp != null) {
            return AppRecommendation(
                type = RecommendationType.USE_APP,
                app = serviceApp,
                reason = "Using ${serviceApp.appName} for this task"
            )
        }

        // Fallback to browser
        val browserApp = findBrowserApp()
        return AppRecommendation(
            type = RecommendationType.USE_BROWSER,
            app = browserApp,
            reason = "No specific app found, using browser"
        )
    }

    /**
     * Launch an app by package name.
     * Returns true if launch was successful.
     */
    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.d(TAG, "Launched app: $packageName")
                true
            } else {
                Log.w(TAG, "No launch intent for: $packageName")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch app: $packageName", e)
            false
        }
    }

    /**
     * Launch a URL in the best available browser.
     */
    fun launchUrl(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch URL: $url", e)
            false
        }
    }

    /**
     * Get a formatted list of installed apps for the AI context.
     */
    fun getAppsContextForAI(): String {
        val apps = getInstalledApps()
        val sb = StringBuilder()
        sb.appendLine("Installed apps on this device (${apps.size} total):")
        sb.appendLine()

        // Group by category
        val grouped = apps.groupBy { it.category }

        grouped.forEach { (category, categoryApps) ->
            sb.appendLine("$category:")
            categoryApps.take(10).forEach { app ->
                sb.appendLine("  - ${app.appName} (${app.packageName})")
            }
            if (categoryApps.size > 10) {
                sb.appendLine("  ... and ${categoryApps.size - 10} more")
            }
            sb.appendLine()
        }

        return sb.toString()
    }

    /**
     * Get a concise app summary for quick reference.
     */
    fun getAppsSummary(): String {
        val apps = getInstalledApps()
        val popular = apps.filter { !it.isSystemApp }.take(20)

        return popular.joinToString(", ") { it.appName }
    }

    private fun isSystemApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    }

    private fun isUsefulSystemApp(packageName: String): Boolean {
        val usefulSystemApps = setOf(
            "com.android.settings",
            "com.android.chrome",
            "com.google.android.apps.maps",
            "com.google.android.gm",
            "com.google.android.youtube",
            "com.google.android.apps.photos",
            "com.google.android.calendar",
            "com.google.android.contacts",
            "com.google.android.dialer",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.samsung.android.dialer",
            "com.samsung.android.contacts",
            "com.sec.android.app.camera",
            "com.android.camera",
            "com.android.vending", // Play Store
        )
        return usefulSystemApps.any { packageName.contains(it) }
    }

    private fun categorizeApp(packageName: String, appName: String): String {
        val pkg = packageName.lowercase()
        val name = appName.lowercase()

        return when {
            pkg.contains("music") || pkg.contains("spotify") || pkg.contains("podcast") ||
            name.contains("music") || name.contains("spotify") -> "Music & Audio"

            pkg.contains("video") || pkg.contains("youtube") || pkg.contains("netflix") ||
            pkg.contains("tiktok") || name.contains("video") -> "Video & Entertainment"

            pkg.contains("messaging") || pkg.contains("whatsapp") || pkg.contains("telegram") ||
            pkg.contains("messenger") || pkg.contains("sms") || name.contains("message") -> "Messaging"

            pkg.contains("social") || pkg.contains("twitter") || pkg.contains("facebook") ||
            pkg.contains("instagram") || pkg.contains("linkedin") -> "Social"

            pkg.contains("camera") || pkg.contains("photo") || pkg.contains("gallery") -> "Photos & Camera"

            pkg.contains("maps") || pkg.contains("navigation") || pkg.contains("uber") ||
            pkg.contains("lyft") || name.contains("map") -> "Navigation & Travel"

            pkg.contains("mail") || pkg.contains("gmail") || pkg.contains("outlook") ||
            name.contains("email") || name.contains("mail") -> "Email"

            pkg.contains("bank") || pkg.contains("pay") || pkg.contains("finance") ||
            pkg.contains("venmo") || pkg.contains("wallet") -> "Finance"

            pkg.contains("shop") || pkg.contains("amazon") || pkg.contains("ebay") ||
            pkg.contains("store") -> "Shopping"

            pkg.contains("game") || pkg.contains("play.") -> "Games"

            pkg.contains("settings") || pkg.contains("system") -> "System"

            pkg.contains("browser") || pkg.contains("chrome") || pkg.contains("firefox") ||
            pkg.contains("edge") || pkg.contains("safari") -> "Browser"

            pkg.contains("news") || pkg.contains("reddit") -> "News & Reading"

            pkg.contains("fitness") || pkg.contains("health") || pkg.contains("workout") -> "Health & Fitness"

            pkg.contains("food") || pkg.contains("doordash") || pkg.contains("ubereats") ||
            pkg.contains("grubhub") -> "Food & Delivery"

            else -> "Other"
        }
    }

    private fun matchesCommonAlias(app: InstalledApp, query: String): Boolean {
        val aliases = mapOf(
            "com.google.android.youtube" to listOf("yt", "videos"),
            "com.whatsapp" to listOf("wa", "whatsapp"),
            "com.instagram.android" to listOf("ig", "insta"),
            "com.twitter.android" to listOf("x", "tweets"),
            "com.facebook.katana" to listOf("fb"),
            "com.google.android.gm" to listOf("email", "mail"),
            "com.spotify.music" to listOf("music", "songs"),
            "com.netflix.mediaclient" to listOf("movies", "shows"),
            "com.amazon.mShop.android.shopping" to listOf("shop", "buy"),
            "com.google.android.apps.maps" to listOf("directions", "navigate"),
        )

        return aliases[app.packageName]?.any { it.contains(query) || query.contains(it) } ?: false
    }

    private fun findServiceApp(action: String): InstalledApp? {
        // Map common actions to preferred apps
        val actionAppMap = mapOf(
            "navigate" to listOf("com.google.android.apps.maps", "com.waze"),
            "directions" to listOf("com.google.android.apps.maps", "com.waze"),
            "email" to listOf("com.google.android.gm", "com.microsoft.office.outlook"),
            "message" to listOf("com.google.android.apps.messaging", "com.samsung.android.messaging"),
            "text" to listOf("com.google.android.apps.messaging", "com.samsung.android.messaging"),
            "call" to listOf("com.google.android.dialer", "com.samsung.android.dialer"),
            "photo" to listOf("com.google.android.apps.photos", "com.samsung.android.gallery"),
            "music" to listOf("com.spotify.music", "com.google.android.music"),
            "video" to listOf("com.google.android.youtube"),
            "shop" to listOf("com.amazon.mShop.android.shopping"),
            "food" to listOf("com.doordash.driverapp", "com.ubercab.eats"),
            "ride" to listOf("com.ubercab", "com.lyft.android"),
        )

        for ((keyword, packages) in actionAppMap) {
            if (action.contains(keyword)) {
                for (pkg in packages) {
                    findAppByPackage(pkg)?.let { return it }
                }
            }
        }
        return null
    }

    private fun findBrowserApp(): InstalledApp? {
        val browserPackages = listOf(
            "com.android.chrome",
            "com.google.android.browser",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.opera.browser",
            "com.brave.browser",
            "com.sec.android.app.sbrowser" // Samsung Internet
        )

        for (pkg in browserPackages) {
            findAppByPackage(pkg)?.let { return it }
        }

        // Fallback to any browser
        return getInstalledApps().find { it.category == "Browser" }
    }
}

/**
 * Represents an installed application.
 */
data class InstalledApp(
    val packageName: String,
    val appName: String,
    val activityName: String,
    val isSystemApp: Boolean,
    val category: String
)

/**
 * Recommendation for which app to use.
 */
data class AppRecommendation(
    val type: RecommendationType,
    val app: InstalledApp?,
    val reason: String
)

enum class RecommendationType {
    USE_APP,
    USE_BROWSER,
    NOT_AVAILABLE
}
