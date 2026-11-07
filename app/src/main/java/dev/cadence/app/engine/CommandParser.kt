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
        Regex("\\b(next|skip|lagu\\s+berikutnya|ganti\\s+lagu|maju|selanjutnya|berikutnya|lewat|lewati|skip\\s+lagu|ganti|next\\s+track|skip\\s+track)\\b", RegexOption.IGNORE_CASE)
    )
    private val PREVIOUS_PATTERNS = listOf(
        Regex("\\b(previous|prev|back|lagu\\s+sebelumnya|kembali|mundur|sebelumnya|balik|lagu\\s+tadi|ulang|ulangi|prev\\s+track|back\\s+track)\\b", RegexOption.IGNORE_CASE)
    )
    private val PAUSE_PATTERNS = listOf(
        Regex("\\b(pause|stop|setop|jeda|berhenti|berhentikan|diam|tahan|matikan|matiin)\\b", RegexOption.IGNORE_CASE)
    )
    private val PLAY_PATTERNS = listOf(
        Regex("\\b(play|resume|lanjut|lanjutkan|mainkan|putar|puter|teruskan|nyalakan|hidupkan|start)\\b", RegexOption.IGNORE_CASE)
    )
    private val SEARCH_PATTERNS = listOf(
        Regex("^\\s*(?:play|playkan|putar|puter|puterin|mainkan|setel|setelkan|cari|cariin|search|dengarkan|dengerin|nyalakan|nyalain)\\s+(?:lagu\\s+|musik\\s+|song\\s+|track\\s+)?(.+)$", RegexOption.IGNORE_CASE)
    )
    private val FILLERS = listOf(
        "tolong", "dong", "ya", "cade", "kade", "please", "hey", "hei", "halo", "hai", "coba", "bisa", "nih"
    )
    private val WAKE_PREFIXES = listOf(
        "hey cadence", "hi cadence", "halo cadence", "hai cadence", "cadence",
        "hey cade", "hi cade", "ok cade", "hai cade", "hei cade", "halo cade", "oke cade",
        "hey kade", "hi kade", "ok kade", "hai kade", "hei kade", "halo kade", "oke kade",
        "hi candy", "hai candy", "candy", "cade", "kade"
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

    /**
     * Evaluates multiple candidate transcriptions from SpeechRecognizer hypotheses in order.
     * Returns the first non-Unknown command found, or Unknown if all candidates fail.
     */
    fun parseCandidates(candidates: List<String>): Command {
        if (candidates.isEmpty()) return Command.Unknown
        for (cand in candidates) {
            val cmd = parse(cand)
            if (cmd !is Command.Unknown) {
                return cmd
            }
        }
        return Command.Unknown
    }
}
