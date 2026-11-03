package io.github.ntgfr.doomscrollkiller.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** One capture of a page of the built-in feed, taken in developer mode. */
data class PageCapture(
    val timeMillis: Long,
    val url: String,
    val content: String,
)

/**
 * Developer mode captures. Kept in memory for the in-app viewer, and written to the app's PRIVATE
 * storage (files/debug/) so they can be read from a computer with
 * `adb shell run-as io.github.ntgfr.doomscrollkiller ...` (debug builds only).
 */
object DebugStore {
    private const val MAX_IN_MEMORY = 20
    private const val MAX_FILES = 150

    private val _captures = MutableStateFlow<List<PageCapture>>(emptyList())
    /** Newest first. */
    val captures: StateFlow<List<PageCapture>> = _captures

    /** Adds a capture unless it is identical to the previous one. Returns true if it was added. */
    fun add(capture: PageCapture): Boolean {
        var added = false
        _captures.update { list ->
            if (list.firstOrNull()?.content == capture.content) {
                list
            } else {
                added = true
                (listOf(capture) + list).take(MAX_IN_MEMORY)
            }
        }
        return added
    }

    fun clear() {
        _captures.value = emptyList()
    }

    /** Writes the capture to [dir], keeping only the newest files. Call off the main thread. */
    fun saveToFile(dir: File, capture: PageCapture) {
        dir.mkdirs()
        val name = SimpleDateFormat("HHmmss", Locale.US).format(Date(capture.timeMillis)) + "_page.txt"
        File(dir, name).writeText(capture.content)
        dir.listFiles { f -> f.name.endsWith("_page.txt") }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_FILES)
            ?.forEach { it.delete() }
    }
}
