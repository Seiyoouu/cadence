package dev.cadence.app.engine

object CommandParser {

    sealed class Command {
        data object Next : Command()
        data object Previous : Command()
        data object Play : Command()
        data object Pause : Command()
        data object TogglePlayPause : Command()
        data class Search(val query: String) : Command()
        data object Unknown : Command()
    }

    private val NEXT_PATTERNS = listOf(
        Regex("\\b(next|skip|lagu\\s+berikutnya|ganti\\s+lagu|maju)\\b", RegexOption.IGNORE_CASE)
    )
    private val PREVIOUS_PATTERNS = listOf(
        Regex("\\b(previous|prev|back|lagu\\s+sebelumnya|kembali|mundur)\\b", RegexOption.IGNORE_CASE)
    )
    private val PAUSE_PATTERNS = listOf(
        Regex("\\b(pause|stop|jeda|berhenti|diam)\\b", RegexOption.IGNORE_CASE)
    )
    private val PLAY_PATTERNS = listOf(
        Regex("\\b(play|resume|lanjut|mainkan|putar)\\b", RegexOption.IGNORE_CASE)
    )
    private val SEARCH_PATTERNS = listOf(
        Regex("^\\s*(?:play|putar|mainkan|cari|search)\\s+(?:lagu\\s+|song\\s+)?(.+)$", RegexOption.IGNORE_CASE)
    )
    private val FILLERS = listOf(
        "tolong", "dong", "ya", "cade", "please", "hey", "hei", "halo", "hai"
    )
    private val WAKE_PREFIXES = listOf(
        "hey cadence", "hi cadence", "halo cadence", "hai cadence", "cadence",
        "hey cade", "hi cade", "ok cade", "hai cade", "hei cade", "halo cade", "oke cade",
        "hi candy", "hai candy", "cade"
    )

    fun parse(rawTranscript: String): Command {
        var text = rawTranscript.trim().lowercase()
        if (text.isEmpty()) return Command.Unknown

        for (wp in WAKE_PREFIXES) {
            if (text.startsWith("$wp ") || text == wp) {
                text = text.removePrefix(wp).trim()
                break
            }
        }
        if (text.isEmpty()) return Command.Unknown

        for (pattern in SEARCH_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                var query = match.groupValues[1].trim()
                for (filler in FILLERS) {
                    if (query.startsWith("$filler ")) {
                        query = query.removePrefix("$filler ").trim()
                    }
                    if (query.endsWith(" $filler")) {
                        query = query.removeSuffix(" $filler").trim()
                    }
                }
                if (query.length >= 2) {
                    return Command.Search(query)
                }
            }
        }

        if (NEXT_PATTERNS.any { it.containsMatchIn(text) }) return Command.Next
        if (PREVIOUS_PATTERNS.any { it.containsMatchIn(text) }) return Command.Previous
        if (PAUSE_PATTERNS.any { it.containsMatchIn(text) }) return Command.Pause
        if (PLAY_PATTERNS.any { it.containsMatchIn(text) }) return Command.Play

        return Command.Unknown
    }
}
