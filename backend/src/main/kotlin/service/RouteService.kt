package service

import exception.BadRequestException
import exception.HttpException
import org.apache.commons.codec.language.Nysiis
import org.apache.commons.codec.language.Soundex
import org.apache.commons.codec.language.bm.BeiderMorseEncoder
import org.apache.commons.text.similarity.LevenshteinDistance
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import persistence.BeiderMorseEncodedPlace
import persistence.BeiderMorseEncodedPlaces
import persistence.NysiisEncodedPlace
import persistence.NysiisEncodedPlaces
import persistence.Place
import persistence.Places
import persistence.SoundexEncodedPlace
import persistence.SoundexEncodedPlaces

class RouteService {

    private val beiderMorseEncoder = BeiderMorseEncoder()
    private val nysiisEncoder = Nysiis()
    private val soundexEnocder = Soundex()
    private val levenshteinDistance = LevenshteinDistance()

    /**
     * Maps the passed phrase to a route (a list of places) that sound similar.
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
                val exactMatches = Place.find { Places.name.lowerCase() eq word.toLowerCase() }.toList()
                if (exactMatches.isNotEmpty()) {
                    // there is a place that's named exactly like the word -> use it!
                    // TODO take the match that fits best into the route!
                    route.add(exactMatches[0])
                } else {
                    val matches = getPhoneticMatchesForWord(word)
                    // TODO take the match that fits best into the route!
                    val bestMatch = findBestMatch(word, matches)[0]
                    route.add(bestMatch)
                }
            }
        }
        return route
    }

    /**
     * Retrieves all places from the database that sound similar to the passed word. To calculate these matches it uses
     * the Beider-Morse and the Nysiis Phonetic Matching Algorithm. If some matches are contained in both result sets,
     * only these matches are returned. Otherwise the result sets will simply be combined and returned.
     *
     * @param word the word to find similar sounding places for.
     * @return a list of places that match to the passed word.
     */
    private fun getPhoneticMatchesForWord(word: String): List<Place> {
        // Nysiis
        val nysiisMatches = NysiisEncodedPlace
            .find { NysiisEncodedPlaces.code eq nysiisEncoder.encode(word) }
            .map { it.place }

        // Beider Morse
        val beiderMorseCodes = beiderMorseEncoder.encode(word).split("|")
        val beiderMorseMatches = ArrayList<Place>()
        beiderMorseCodes.forEach { code ->
            beiderMorseMatches.addAll(
                BeiderMorseEncodedPlace
                    .find { BeiderMorseEncodedPlaces.code eq code }
                    .map { it.place }
            )
        }

        // use soundex as backup if both result sets are empty (should barely never happen but to make sure...)
        if (beiderMorseMatches.isEmpty() && nysiisMatches.isEmpty()) {
            return SoundexEncodedPlace
                .find { SoundexEncodedPlaces.code eq soundexEnocder.encode(word) }
                .map { it.place }
        }

        // Combine result sets
        val matchedPlaces = nysiisMatches.intersect(beiderMorseMatches).toList()
        return if (matchedPlaces.isEmpty()) {
            nysiisMatches.union(beiderMorseMatches).toList()
        } else {
            matchedPlaces
        }
    }

    /**
     * Finds the best matches out of a given list for a given word using the Levenshtein distance.
     *
     * @param word the word to find the best match for.
     * @param matches the matches where the best ones should be found.
     *
     * @return a list of places containing the best matches.
     */
    private fun findBestMatch(word: String, matches: List<Place>): List<Place> {
        var bestDistance: Int? = null
        val bestMatches = ArrayList<Place>()

        matches.forEach { match ->
            val distance = levenshteinDistance.apply(word, match.name)
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