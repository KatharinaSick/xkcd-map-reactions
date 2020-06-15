package service

import exception.BadRequestException
import exception.HttpException
import exception.NotFoundException
import model.Place
import org.apache.commons.codec.language.Nysiis
import org.apache.commons.codec.language.Soundex
import org.apache.commons.codec.language.bm.BeiderMorseEncoder
import org.apache.commons.text.similarity.LevenshteinDistance
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import persistence.BeiderMorseEncodedPlaceDao
import persistence.BeiderMorseEncodedPlaces
import persistence.NysiisEncodedPlaceDao
import persistence.NysiisEncodedPlaces
import persistence.PlaceDao
import persistence.Places
import persistence.SoundexEncodedPlaceDao
import persistence.SoundexEncodedPlaces
import kotlin.math.max

class RouteService {

    private val beiderMorseEncoder = BeiderMorseEncoder()
    private val nysiisEncoder = Nysiis()
    private val soundexEnocder = Soundex()
    private val levenshteinDistance = LevenshteinDistance()

    /**
     * Maps the passed phrase to a route (a list of places) that sounds similar.
     *
     * @param phraseToMap the phrase to map to a route.
     * @return a list of places representing the mapped route.
     */
    @Throws(HttpException::class)
    fun mapPhraseToRoute(phraseToMap: String?): List<Place> {
        val wordsToMap = phraseToMap
            ?.split(" ")
            ?.filter { it.isNotBlank() }
            ?.toList()

        if (wordsToMap == null || wordsToMap.isEmpty()) {
            throw BadRequestException("Phrase must not be empty")
        }

        Database.connect(
            "jdbc:postgresql://${System.getenv("DB_URL")}/${System.getenv("DB_NAME")}",
            driver = "org.postgresql.Driver",
            user = System.getenv("DB_USER"),
            password = System.getenv("DB_PASSWORD")
        )

        val route = ArrayList<Place>()
        transaction {
            wordsToMap.forEach { word ->
                val exactMatches = PlaceDao
                    .find { Places.name.lowerCase() eq word.toLowerCase() }
                    .map { it.toModel() }
                    .toList()

                if (exactMatches.isNotEmpty()) {
                    // there is a place that's named exactly like the word -> use it!
                    // TODO take the match that fits best into the route!
                    route.add(exactMatches[0])
                    return@forEach
                }

                val matches = getPhoneticMatchesForWord(word)
                if (matches.isEmpty()) {
                    throw NotFoundException("No phonetic match found for \"$word\"")
                }
                // TODO take the match that fits best into the route!
                route.add(findBestMatch(word, matches)[0])
            }
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
    private fun getPhoneticMatchesForWord(word: String): List<Place> {
        // Nysiis
        val nysiisMatches = NysiisEncodedPlaceDao
            .find { NysiisEncodedPlaces.code eq nysiisEncoder.encode(word) }
            .map { it.place.toModel() }

        // Beider Morse
        val beiderMorseCodes = beiderMorseEncoder.encode(word).split("\\|")
        val beiderMorseMatches = ArrayList<Place>()
        beiderMorseCodes.forEach { code ->
            beiderMorseMatches.addAll(
                BeiderMorseEncodedPlaceDao
                    .find { BeiderMorseEncodedPlaces.code eq code }
                    .map { it.place.toModel() }
            )
        }

        // Soundex as fallback if both result sets are empty (should barely never happen but to make sure...)
        if (beiderMorseMatches.isEmpty() && nysiisMatches.isEmpty()) {
            return SoundexEncodedPlaceDao
                .find { SoundexEncodedPlaces.code eq soundexEnocder.encode(word) }
                .map { it.place.toModel() }
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
        var bestDistance: Int? = null
        val bestMatches = ArrayList<Place>()

        matches.forEach { match ->
            // Normalized Levenshtein distance can also be used to compare words of different length
            val distance = levenshteinDistance.apply(word, match.name) / max(word.length, match.name.length)
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
}