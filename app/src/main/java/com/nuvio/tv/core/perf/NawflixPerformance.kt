package com.nuvio.tv.core.perf

import android.app.ActivityManager
import android.content.Context

/**
 * Device performance tier. "Potato" mode trades a few visual refinements (long cross-fades,
 * translucent surfaces, big backdrops, focus zoom) for smoothness on low-RAM TVs (~1-2 GB).
 *
 * The mode is resolved once at process start: [Mode.AUTO] picks potato from the device RAM, the
 * user can force either tier from Settings > Advanced (the app restarts to apply it).
 */
object NawflixPerformance {
    enum class Mode { AUTO, STANDARD, POTATO }

    private const val PREFS = "nawflix_performance"
    private const val KEY_MODE = "mode"

    // A "2 GB" TV reports ~1.7-1.9 GB of usable RAM, a "1.5 GB" one ~1.3 GB.
    private const val LOW_RAM_LIMIT_MB = 2300L

    @Volatile
    var mode: Mode = Mode.AUTO
        private set

    /** True when the lightweight rendering path must be used. Stable for the whole process. */
    @Volatile
    var potato: Boolean = false
        private set

    @Volatile
    var totalRamMb: Long = 0L
        private set

    fun init(context: Context) {
        val app = context.applicationContext
        mode = runCatching {
            Mode.valueOf(
                app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_MODE, null) ?: Mode.AUTO.name
            )
        }.getOrDefault(Mode.AUTO)
        val am = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo().also(am::getMemoryInfo)
        totalRamMb = info.totalMem / (1024L * 1024L)
        potato = when (mode) {
            Mode.POTATO -> true
            Mode.STANDARD -> false
            Mode.AUTO -> am.isLowRamDevice || totalRamMb <= LOW_RAM_LIMIT_MB
        }
    }

    fun setMode(context: Context, newMode: Mode): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, newMode.name)
            .commit()
}
