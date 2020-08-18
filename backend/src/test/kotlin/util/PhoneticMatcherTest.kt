package util

import model.Place
import org.apache.commons.codec.language.Nysiis
import org.apache.commons.codec.language.Soundex
import org.apache.commons.codec.language.bm.BeiderMorseEncoder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import persistence.PlaceRepository

internal class PhoneticMatcherTest {

    @Test
    fun `test empty trie is empty result`() {
        test(0, "search", emptyList(), false, emptySet())
    }

    @Test
    fun `test single search is single result`() {
        test(
            0, "mountain",
            listOf(
                "mountain"
            ),
            false, setOf(
                Match(0, 1, 0)
            )
        )
    }

    @Test
    fun `test single search with end index is done`() {
        test(
            1, "s",
            listOf(
                "s"
            ),
            true, setOf(
            )
        )
    }

    @Test
    fun `test fuzzy searches`() {
        test(
            0, "mountain",
            listOf(
                "moontain",
                "somotherword"
            ),
            false, setOf(
                Match(0, 1, 1)
            )
        )
    }

    @Test
    fun `test search word in phrase`() {
        test(
            0, "mountain is high",
            listOf(
                "mountain",
                "anotherword"
            ),
            false, setOf(
                Match(0, 1, 0),
                Match(0, 2, 3),
                Match(0, 3, 8)
            )
        )
    }

    @Test
    fun `test search second word in phrase`() {
        test(
            1, "this mountain is high",
            listOf(
                "mountain",
                "anotherword"
            ),
            false, setOf(
                Match(0, 2, 0),
                Match(0, 3, 3),
                Match(0, 4, 8)
            )
        )
    }

    @Test
    fun `test score is levenshtein distance`() {
        test(
            0, "moontaim",
            listOf(
                "mountain"
            ),
            false, setOf(
                Match(0, 1, 2)
            )
        )
    }

    private fun test(
        depth: Int,
        search: String,
        inputWords: List<String>,
        done: Boolean,
        results: Set<Match>
    ) {
        val placeRepository = InMemoryPlaceRepository()
        inputWords.forEachIndexed { id, word -> placeRepository.addPlace(id, word) }

        val result = PhoneticMatcher(search, placeRepository).match(depth)

        Assertions.assertEquals(done, result.first)
        Assertions.assertEquals(results, result.second)
    }

}

class InMemoryPlaceRepository : PlaceRepository {

    companion object {
        private val beiderMorseEncoder = BeiderMorseEncoder()
        private val nyiis = Nysiis()
        private val soundex = Soundex()
    }


    private val allPlaces: MutableList<Place> = mutableListOf()
    private val nyiisMatches: MutableMap<String, MutableList<Place>> = mutableMapOf()
    private val beiderMorseMatches: MutableMap<String, MutableList<Place>> = mutableMapOf()
    private val soundexMatches: MutableMap<String, MutableList<Place>> = mutableMapOf()

    override fun findAllWhereNysiisCodeMatches(nysiisCode: String): List<Place> {
        return nyiisMatches.getOrDefault(nysiisCode, emptyList())
    }

    override fun findAllWhereBeiderMorseCodeMatches(beiderMorseCodes: List<String>): List<Place> {
        return beiderMorseCodes
            .map { beiderMorseCode -> beiderMorseMatches.getOrDefault(beiderMorseCode, mutableListOf()) }
            .flatten()
    }

    override fun findAllWhereSoundexCodeMatches(soundexCode: String): List<Place> {
        return soundexMatches.getOrDefault(soundexCode, emptyList())
    }

    override fun findAllForIds(allPlaceIds: Set<Long>): Map<Long, Place> {
        return allPlaces.filter { allPlaceIds.contains(it.id) }.associateBy { it.id }
    }

    override fun findAll(): List<Place> {
        return allPlaces
    }

    fun addPlace(id: Int, word: String) {
        val newPlace = Place(id.toLong(), word, 0.toDouble(), 0.toDouble())
        allPlaces.add(newPlace)
        beiderMorseEncoder.encode(word).split("|").forEach {
            if (!beiderMorseMatches.containsKey(it)) {
                beiderMorseMatches[it] = mutableListOf()
            }
            beiderMorseMatches[it]!!.add(newPlace)
        }
        val nyiis = nyiis.encode(word)
        if (!nyiisMatches.containsKey(nyiis)) {
            nyiisMatches[nyiis] = mutableListOf()
        }
        nyiisMatches[nyiis]!!.add(newPlace)
        val soundex = soundex.encode(word)
        if (!soundexMatches.containsKey(soundex)) {
            soundexMatches[soundex] = mutableListOf()
        }
        soundexMatches[soundex]!!.add(newPlace)
    }

}
