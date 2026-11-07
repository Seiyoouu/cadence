package dev.cadence.app.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandParserTest {

    // --- 1. Next Commands (EN & ID) ---
    @Test
    fun testNext_en_simple() {
        assertEquals(CommandParser.Command.Next, CommandParser.parse("next"))
    }

    @Test
    fun testNext_en_skip() {
        assertEquals(CommandParser.Command.Next, CommandParser.parse("skip"))
    }

    @Test
    fun testNext_en_withWakeWord() {
        assertEquals(CommandParser.Command.Next, CommandParser.parse("hey cade next"))
    }

    @Test
    fun testNext_en_withFiller() {
        assertEquals(CommandParser.Command.Next, CommandParser.parse("next please"))
    }

    @Test
    fun testNext_id_laguBerikutnya() {
        assertEquals(CommandParser.Command.Next, CommandParser.parse("lagu berikutnya"))
    }

    @Test
    fun testNext_id_gantiLagu() {
        assertEquals(CommandParser.Command.Next, CommandParser.parse("ganti lagu"))
    }

    @Test
    fun testNext_id_maju() {
        assertEquals(CommandParser.Command.Next, CommandParser.parse("maju"))
    }

    @Test
    fun testNext_id_withWakeWord() {
        assertEquals(CommandParser.Command.Next, CommandParser.parse("halo cade lagu berikutnya"))
    }

    // --- 2. Previous Commands (EN & ID) ---
    @Test
    fun testPrevious_en_simple() {
        assertEquals(CommandParser.Command.Previous, CommandParser.parse("previous"))
    }

    @Test
    fun testPrevious_en_prev() {
        assertEquals(CommandParser.Command.Previous, CommandParser.parse("prev"))
    }

    @Test
    fun testPrevious_en_back() {
        assertEquals(CommandParser.Command.Previous, CommandParser.parse("back"))
    }

    @Test
    fun testPrevious_en_goBack() {
        assertEquals(CommandParser.Command.Previous, CommandParser.parse("go back"))
    }

    @Test
    fun testPrevious_id_laguSebelumnya() {
        assertEquals(CommandParser.Command.Previous, CommandParser.parse("lagu sebelumnya"))
    }

    @Test
    fun testPrevious_id_kembali() {
        assertEquals(CommandParser.Command.Previous, CommandParser.parse("kembali"))
    }

    @Test
    fun testPrevious_id_mundur() {
        assertEquals(CommandParser.Command.Previous, CommandParser.parse("mundur"))
    }

    @Test
    fun testPrevious_id_withWakeWord() {
        assertEquals(CommandParser.Command.Previous, CommandParser.parse("hai cade lagu sebelumnya"))
    }

    // --- 3. Pause Commands (EN & ID) ---
    @Test
    fun testPause_en_simple() {
        assertEquals(CommandParser.Command.Pause, CommandParser.parse("pause"))
    }

    @Test
    fun testPause_en_stop() {
        assertEquals(CommandParser.Command.Pause, CommandParser.parse("stop"))
    }

    @Test
    fun testPause_id_jeda() {
        assertEquals(CommandParser.Command.Pause, CommandParser.parse("jeda"))
    }

    @Test
    fun testPause_id_berhenti() {
        assertEquals(CommandParser.Command.Pause, CommandParser.parse("berhenti"))
    }

    @Test
    fun testPause_id_diam() {
        assertEquals(CommandParser.Command.Pause, CommandParser.parse("diam"))
    }

    @Test
    fun testPause_id_withWakeWord() {
        assertEquals(CommandParser.Command.Pause, CommandParser.parse("halo cade jeda"))
    }

    // --- 4. Play Commands (EN & ID) ---
    @Test
    fun testPlay_en_simple() {
        assertEquals(CommandParser.Command.Play, CommandParser.parse("play"))
    }

    @Test
    fun testPlay_en_resume() {
        assertEquals(CommandParser.Command.Play, CommandParser.parse("resume"))
    }

    @Test
    fun testPlay_id_lanjut() {
        assertEquals(CommandParser.Command.Play, CommandParser.parse("lanjut"))
    }

    @Test
    fun testPlay_id_mainkan() {
        assertEquals(CommandParser.Command.Play, CommandParser.parse("mainkan"))
    }

    @Test
    fun testPlay_id_putar() {
        assertEquals(CommandParser.Command.Play, CommandParser.parse("putar"))
    }

    // --- 5. Search Commands (EN & ID) ---
    @Test
    fun testSearch_en_playBohemianRhapsody() {
        val result = CommandParser.parse("play bohemian rhapsody")
        assertTrue(result is CommandParser.Command.Search)
        assertEquals("bohemian rhapsody", (result as CommandParser.Command.Search).query)
    }

    @Test
    fun testSearch_en_playSongYellow() {
        val result = CommandParser.parse("play coldplay yellow")
        assertTrue(result is CommandParser.Command.Search)
        assertEquals("coldplay yellow", (result as CommandParser.Command.Search).query)
    }

    @Test
    fun testSearch_en_searchSong() {
        val result = CommandParser.parse("search song imagine dragons")
        assertTrue(result is CommandParser.Command.Search)
        assertEquals("imagine dragons", (result as CommandParser.Command.Search).query)
    }

    @Test
    fun testSearch_id_putarLaguDewa19() {
        val result = CommandParser.parse("putar lagu dewa 19 kangen")
        assertTrue(result is CommandParser.Command.Search)
        assertEquals("dewa 19 kangen", (result as CommandParser.Command.Search).query)
    }

    @Test
    fun testSearch_id_mainkanTulus() {
        val result = CommandParser.parse("mainkan tulus hati-hati di jalan")
        assertTrue(result is CommandParser.Command.Search)
        assertEquals("tulus hati-hati di jalan", (result as CommandParser.Command.Search).query)
    }

    @Test
    fun testSearch_id_cariSheilaOn7() {
        val result = CommandParser.parse("cari sheila on 7 dan")
        assertTrue(result is CommandParser.Command.Search)
        assertEquals("sheila on 7 dan", (result as CommandParser.Command.Search).query)
    }

    @Test
    fun testSearch_id_putarKangenDong() {
        val result = CommandParser.parse("putar lagu kangen tolong")
        assertTrue(result is CommandParser.Command.Search)
        assertEquals("kangen", (result as CommandParser.Command.Search).query)
    }

    // --- 6. Search with Wake Words ---
    @Test
    fun testSearch_withWakeWord_en() {
        val result = CommandParser.parse("hey cade play bohemian rhapsody")
        assertTrue(result is CommandParser.Command.Search)
        assertEquals("bohemian rhapsody", (result as CommandParser.Command.Search).query)
    }

    @Test
    fun testSearch_withWakeWord_id() {
        val result = CommandParser.parse("halo cade putar lagu bohemian rhapsody")
        assertTrue(result is CommandParser.Command.Search)
        assertEquals("bohemian rhapsody", (result as CommandParser.Command.Search).query)
    }

    @Test
    fun testSearch_withWakeWord_hiCandy() {
        val result = CommandParser.parse("hi candy play yellow")
        assertTrue(result is CommandParser.Command.Search)
        assertEquals("yellow", (result as CommandParser.Command.Search).query)
    }

    @Test
    fun testSearch_withWakeWord_haiCade() {
        val result = CommandParser.parse("hai cade cari dewa 19 kangen")
        assertTrue(result is CommandParser.Command.Search)
        assertEquals("dewa 19 kangen", (result as CommandParser.Command.Search).query)
    }

    // --- 7. Unknown / Edge Cases ---
    @Test
    fun testUnknown_empty() {
        assertEquals(CommandParser.Command.Unknown, CommandParser.parse(""))
    }

    @Test
    fun testUnknown_blank() {
        assertEquals(CommandParser.Command.Unknown, CommandParser.parse("   "))
    }

    @Test
    fun testUnknown_gibberish() {
        assertEquals(CommandParser.Command.Unknown, CommandParser.parse("asdf qwerty"))
    }

    @Test
    fun testUnknown_wakeWordOnly() {
        assertEquals(CommandParser.Command.Unknown, CommandParser.parse("hey cade"))
    }

    @Test
    fun testUnknown_wakeWordOnlyId() {
        assertEquals(CommandParser.Command.Unknown, CommandParser.parse("halo cade"))
    }

    // --- 7. Natural Indonesian Phrasing & Candidates Tests ---
    @Test
    fun testIndonesianNaturalPhrasings() {
        assertEquals(CommandParser.Command.Next, CommandParser.parse("selanjutnya"))
        assertEquals(CommandParser.Command.Next, CommandParser.parse("berikutnya"))
        assertEquals(CommandParser.Command.Previous, CommandParser.parse("sebelumnya"))
        assertEquals(CommandParser.Command.Pause, CommandParser.parse("setop"))
        assertEquals(CommandParser.Command.Play, CommandParser.parse("lanjutkan"))
        assertEquals(CommandParser.Command.Play, CommandParser.parse("puter"))
        assertEquals(CommandParser.Command.Search("dewa 19 kangen"), CommandParser.parse("puter lagu dewa 19 kangen"))
        assertEquals(CommandParser.Command.Search("coldplay yellow"), CommandParser.parse("setelkan lagu coldplay yellow dong"))
        assertEquals(CommandParser.Command.Search("tulus hati hati di jalan"), CommandParser.parse("setel tulus hati hati di jalan"))
        assertEquals(CommandParser.Command.Search("sheila on 7"), CommandParser.parse("cariin lagu sheila on 7"))
    }

    @Test
    fun testParseCandidates_fallback() {
        val candidates = listOf("laku berikutnya", "lagu berikutnya", "lagu berikut nya")
        assertEquals(CommandParser.Command.Next, CommandParser.parseCandidates(candidates))
    }

    @Test
    fun testParseCandidates_search() {
        val candidates = listOf("asdf unknown", "puter lagu dewa 19", "something else")
        assertEquals(CommandParser.Command.Search("dewa 19"), CommandParser.parseCandidates(candidates))
    }
}
