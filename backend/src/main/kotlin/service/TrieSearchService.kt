package service

import exception.HttpException
import model.Place
import org.apache.commons.text.similarity.LevenshteinDistance
import persistence.PlaceRepository
import util.trie.Trie
import util.trie.TrieSearch
import java.io.BufferedInputStream
import java.util.zip.GZIPInputStream

class TrieSearchService {
    private val trie = loadTrie()
    private val placeRepository = PlaceRepository()

    @Throws(HttpException::class)
    fun mapPhraseToRoute(search: String): List<Place>? {
        val results = TrieSearch(trie, search).search()
        if (results.isEmpty()) {
            return null
        }

        val allPlaceIds = results.flatten().map { it.toLong() }.toSet()
        val placeMappings = placeRepository.findAllForIds(allPlaceIds)

        val translatedResults = results.map { it.map { placeMappings[it.toLong()]!! } }

        return translatedResults.minBy {
            LevenshteinDistance().apply(
                search,
                it.joinToString(" ") { it.name.toLowerCase() }
            )
        }
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