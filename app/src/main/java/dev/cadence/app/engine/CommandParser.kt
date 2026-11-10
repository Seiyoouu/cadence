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
        "hey cadence", "hi cadence", "halo cadence", "hai cadence",
        "hey kade", "hi kade", "ok kade", "hai kade", "hei kade", "halo kade", "oke kade",
        "hey kadi", "hi kadi", "ok kadi", "hai kadi", "hei kadi", "halo kadi", "oke kadi",
        "hey cade", "hi cade", "ok cade", "hai cade", "hei cade", "halo cade", "oke cade",
        "cadence", "chade"
    ).sortedByDescending { it.length }

    private val IGNORED_SOLO_WORDS = setOf(
        "cadence", "cade", "kade", "kadi", "halo", "hai", "hei", "hey",
        "tes", "test", "testing", "asdf", "qwerty", "tolong", "dong", "please", "ya", "nih",
        "bisa", "coba", "oke", "ok", "asdf qwerty", "bro", "musik", "music", "lagu", "song",
        "uh", "um", "ah", "ha", "haha", "hmm", "iya", "ya", "yo", "sip", "mantap"
    )

    private fun isIgnoredSoloWord(word: String): Boolean {
        return word.trim() in IGNORED_SOLO_WORDS
    }

    fun parse(rawTranscript: String, allowImplicitSearch: Boolean = true): Command {
        var text = rawTranscript.trim().lowercase()
        if (text.isEmpty()) return Command.Unknown

        for (wp in WAKE_PREFIXES) {
            if (text.startsWith("$wp ") || text == wp) {
                text = text.removePrefix(wp).trim()
                break
            }
        }
        if (text.isEmpty()) return Command.Unknown

        // 1. Control commands (Next, Previous, Pause) take priority
        if (NEXT_PATTERNS.any { it.containsMatchIn(text) }) return Command.Next
        if (PREVIOUS_PATTERNS.any { it.containsMatchIn(text) }) return Command.Previous
        if (PAUSE_PATTERNS.any { it.containsMatchIn(text) }) return Command.Pause

        // 2. Explicit search verbs with query: "play [song]", "putar lagu [judul]", "cari [judul]", "setel [judul]"
        for (pattern in SEARCH_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                var query = match.groupValues[1].trim()
                for (filler in FILLERS) {
                    if (query.startsWith("$filler ")) query = query.removePrefix("$filler ").trim()
                    if (query.endsWith(" $filler")) query = query.removeSuffix(" $filler").trim()
                }
                if (query.length >= 2) {
                    return Command.Search(query)
                }
            }
        }

        // 3. Play command (without query: e.g. "play", "resume", "putar", "mainkan", "lanjut")
        if (PLAY_PATTERNS.any { it.containsMatchIn(text) }) return Command.Play

        // 4. Explicit "lagu [judul]" pattern (e.g. "lagu dewa 19 kangen", "lagu komang")
        if (text.startsWith("lagu ") && text.length > 5) {
            var query = text.removePrefix("lagu ").trim()
            for (filler in FILLERS) {
                if (query.startsWith("$filler ")) query = query.removePrefix("$filler ").trim()
                if (query.endsWith(" $filler")) query = query.removeSuffix(" $filler").trim()
            }
            if (query.length >= 2 && !isIgnoredSoloWord(query)) {
                return Command.Search(query)
            }
        }

        // 5. Implicit music search fallback:
        // When a user speaks a song title or artist without an explicit verb
        // (or if audio timing clipped the verb), treat as a music search.
        if (allowImplicitSearch) {
            var cleaned = text
            for (prefix in listOf("musik ", "song ", "track ")) {
                if (cleaned.startsWith(prefix)) {
                    cleaned = cleaned.removePrefix(prefix).trim()
                }
            }
            for (filler in FILLERS) {
                if (cleaned.startsWith("$filler ")) cleaned = cleaned.removePrefix("$filler ").trim()
                if (cleaned.endsWith(" $filler")) cleaned = cleaned.removeSuffix(" $filler").trim()
            }
            if (cleaned.length >= 2 && !isIgnoredSoloWord(cleaned)) {
                return Command.Search(cleaned)
            }
        }

        return Command.Unknown
    }

    /**
     * Evaluates multiple candidate transcriptions from SpeechRecognizer hypotheses in order.
     * Prioritizes explicit command verbs across all candidates first before falling back to implicit queries.
     */
    fun parseCandidates(candidates: List<String>): Command {
        if (candidates.isEmpty()) return Command.Unknown

        // Pass 1: Prioritize explicit verbs and commands across all candidates
        for (cand in candidates) {
            val cmd = parse(cand, allowImplicitSearch = false)
            if (cmd !is Command.Unknown) {
                return cmd
            }
        }

        // Pass 2: Fallback to implicit song searches
        for (cand in candidates) {
            val cmd = parse(cand, allowImplicitSearch = true)
            if (cmd !is Command.Unknown) {
                return cmd
            }
        }

        return Command.Unknown
    }
}
