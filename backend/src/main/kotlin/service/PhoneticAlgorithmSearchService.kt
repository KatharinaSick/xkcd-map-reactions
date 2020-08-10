package service

import exception.BadRequestException
import exception.HttpException
import exception.NotFoundException
import model.Place
import org.apache.commons.codec.language.Nysiis
import org.apache.commons.codec.language.Soundex
import org.apache.commons.codec.language.bm.BeiderMorseEncoder
import org.apache.commons.text.similarity.LevenshteinDistance
import persistence.DachPlaceRepository
import persistence.PlaceRepository
import persistence.UsPlaceRepository
import util.PhoneticMatcherUtils.getPhoneticMatchesForWord
import util.Region
import kotlin.streams.asSequence

class PhoneticAlgorithmSearchService {

    private val usPlaceRepository = UsPlaceRepository()
    private val dachPlaceRepository = DachPlaceRepository()

    private val levenshteinDistance = LevenshteinDistance()

    /**
     * Maps the passed phrase to a route (a list of places) that sounds similar.
     *
     * @param phrase the phrase to map to a route.
     * @return a list of places representing the mapped route.
     */
    @Throws(HttpException::class)
    fun mapPhraseToRoute(phrase: String, region: Region): List<Place>? {
        val wordsToMap = splitPhraseToWords(phrase)

        if (wordsToMap.isEmpty()) {
            throw BadRequestException("Phrase must not be empty")
        }

        val route = ArrayList<Place>()
        wordsToMap.forEach { word ->
            val matches = mapWord(region, word)
            // TODO take the match that fits best into the route!
            route.add(matches[0])
        }
        return route
    }

    private fun mapWord(region: Region, word: String): List<Place> {
        val placeRepository = getRepository(region)
        val exactMatches = placeRepository.findAllWhereNameMatchesIgnoreCase(word)

        return if (exactMatches.isNotEmpty()) {
            // there is a place that's named exactly like the word -> use it!
            exactMatches
        } else {
            val matches = getPhoneticMatchesForWord(placeRepository, word)
            if (matches.isEmpty()) {
                throw NotFoundException("No phonetic match found for \"$word\"")
            }
            findBestMatch(word, matches)
        }
    }

    private fun getRepository(region: Region): PlaceRepository {
        return when (region) {
            Region.US -> usPlaceRepository
            Region.DACH -> dachPlaceRepository
        }
    }

    /**
     * Finds the best matches out of a given list for a given word using the Levenshtein distance.
     *
     * @param word the word to find the best match for.
     * @param matches the matches where the best ones should be found.
     *
     * @return a list of places containing the best matches for the passed word.
     */
    private fun findBestMatch(word: String, matches: List<Place>): List<Place> {
        var bestDistance: Double? = null
        val bestMatches = ArrayList<Place>()

        matches.forEach { match ->
            // Normalized Levenshtein distance can also be used to compare words of different length
            // TODO didn't think this through yet - maybe also another measure than the levenshtein distance could be good!
            val distance = levenshteinDistance.apply(word.toLowerCase(), match.name.toLowerCase()).toDouble() / word.length.toDouble()
            bestDistance.let { best ->
                if (best == null || distance < best) {
                    bestMatches.clear()
                    bestMatches.add(match)
                    bestDistance = distance
                } else if (distance == best) {
                    bestMatches.add(match)
                }
            }
        }
        return bestMatches
    }

    /**
     * Checks if the passed phrase is valid (not empty & contains only letters and spaces) and if yes, it splits it to
     * a list of words and returns this list.
     *
     * @param phrase the phrase to check and split.
     * @return a list of all words contained in the passed phrase.
     */
    private fun splitPhraseToWords(phrase: String?): List<String> {
        if (phrase == null || phrase.isBlank()) {
            throw BadRequestException("Phrase must not be empty")
        }

        if (!phrase.chars().asSequence().all { Character.isLetter(it) || Character.isSpaceChar(it) }) {
            throw BadRequestException("Phrase must contain only alphabetical letters and spaces")
        }

        return phrase
            .split(" ")
            .filter { it.isNotBlank() }
            .toList()
    }
}