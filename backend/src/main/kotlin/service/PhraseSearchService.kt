package service

import exception.HttpException
import model.Place
import persistence.DachPlaceRepository
import persistence.UsPlaceRepository
import util.PhoneticMatcher
import util.PhraseSearch
import util.Region
import util.trie.Trie
import util.trie.TrieMatcher
import java.io.BufferedInputStream
import java.util.zip.GZIPInputStream

class PhraseSearchService {
    private val usTrie = loadTrie("/US.trie")
    private val dachTrie = loadTrie("/DACH.trie")

    private val usPlaceRepository = UsPlaceRepository()
    private val dachPlaceRepository = DachPlaceRepository()

    @Throws(HttpException::class)
    fun mapPhraseToRoute(search: String, region: Region): List<Place>? {
        val (trie, placeRepository) = when (region) {
            Region.US -> Pair(usTrie, usPlaceRepository)
            Region.DACH -> Pair(dachTrie, dachPlaceRepository)
        }

//        val results = PhraseSearch(TrieMatcher(search, trie)).search()
        val results = PhraseSearch(PhoneticMatcher(search, placeRepository)).search()
        if (results.isEmpty()) {
            return null
        }
        val bestResult = results[0] //TODO if we want route matching don't ignore the other 99

        val allPlaceIds = bestResult.map { it.toLong() }.toSet()
        val placeMappings = placeRepository.findAllForIds(allPlaceIds)

        return bestResult.map { placeMappings[it.toLong()]!! }
    }

    private fun loadTrie(filePath: String): Trie {
        val input =
            GZIPInputStream(
                BufferedInputStream(
                    javaClass.getResourceAsStream(filePath)
                )
            )

        return Trie(input.readAllBytes())
    }
}