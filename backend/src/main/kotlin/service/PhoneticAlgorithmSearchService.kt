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
import util.Region
import kotlin.streams.asSequence

class PhoneticAlgorithmSearchService {

    private val usPlaceRepository = UsPlaceRepository()
    private val dachPlaceRepository = DachPlaceRepository()

    private val beiderMorseEncoder = BeiderMorseEncoder()
    private val nysiisEncoder = Nysiis()
    private val soundexEnocder = Soundex()
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

        val placeRepository = when (region) {
            Region.US -> usPlaceRepository
            Region.DACH -> dachPlaceRepository
        }

        val route = ArrayList<Place>()
        wordsToMap.forEach { word ->
            val exactMatches = placeRepository.findAllWhereNameMatchesIgnoreCase(word)

            if (exactMatches.isNotEmpty()) {
                // there is a place that's named exactly like the word -> use it!
                // TODO take the match that fits best into the route!
                route.add(exactMatches[0])
                return@forEach
            }

            val matches = getPhoneticMatchesForWord(placeRepository, word)
            if (matches.isEmpty()) {
                throw NotFoundException("No phonetic match found for \"$word\"")
            }
            // TODO take the match that fits best into the route!
            route.add(findBestMatch(word, matches)[0])
        }
        return route
    }

    /**
     * Retrieves all places from the database that sound similar to the passed word. To calculate these matches it uses
     * the Beider-Morse and Nysiis as primary Phonetic Matchin Algorithms. If none of them delivers a result, Soundex
     * is used as fallback.
     *
     * @param word the word to find similar sounding places for.
     * @return a list of places that match to the passed word.
     */
    private fun getPhoneticMatchesForWord(placeRepository: PlaceRepository, word: String): List<Place> {
        // Nysiis
        val nysiisMatches = placeRepository.findAllWhereNysiisCodeMatches(nysiisEncoder.encode(word))

        // Beider Morse
        val beiderMorseCodes = beiderMorseEncoder.encode(word).split("\\|")
        val beiderMorseMatches = ArrayList<Place>()
        beiderMorseCodes.forEach { beiderMorseMatches.addAll(placeRepository.findAllWhereBeiderMorseCodeMatches(it)) }

        // Soundex as fallback if both result sets are empty (should barely never happen but to make sure...)
        if (beiderMorseMatches.isEmpty() && nysiisMatches.isEmpty()) {
            return placeRepository.findAllWhereSoundexCodeMatches(soundexEnocder.encode(word))
        }

        // Combine result sets
        val matchedPlaces = nysiisMatches.intersect(beiderMorseMatches).toList()
        return if (matchedPlaces.isNotEmpty()) {
            matchedPlaces
        } else {
            nysiisMatches.union(beiderMorseMatches).toList()
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
            val distance = levenshteinDistance.apply(word, match.name).toDouble() / word.length.toDouble()
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