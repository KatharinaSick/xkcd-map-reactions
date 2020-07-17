package service

import exception.HttpException
import model.Place
import persistence.UsPlaceRepository
import util.trie.Trie
import util.trie.TrieSearch
import java.io.BufferedInputStream
import java.util.zip.GZIPInputStream

class TrieSearchService {
    private val trie = loadTrie()
    private val placeRepository = UsPlaceRepository()

    @Throws(HttpException::class)
    fun mapPhraseToRoute(search: String): List<Place>? {
        val results = TrieSearch(trie, search).search()
        if (results.isEmpty()) {
            return null
        }
        val bestResult = results[0] //TODO if we want route matching don't ignore the other 99

        val allPlaceIds = bestResult.map { it.toLong() }.toSet()
        val placeMappings = placeRepository.findAllForIds(allPlaceIds)

        return bestResult.map { placeMappings[it.toLong()]!! }
    }

    private fun loadTrie(): Trie {
        val input =
            GZIPInputStream(
                BufferedInputStream(
                    javaClass.getResourceAsStream("/US.trie")
                )
            )
        return Trie(input.readAllBytes())
    }
}