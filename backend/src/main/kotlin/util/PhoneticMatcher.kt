package util

import model.Place
import org.apache.commons.codec.language.Nysiis
import org.apache.commons.codec.language.Soundex
import org.apache.commons.codec.language.bm.BeiderMorseEncoder
import org.apache.commons.text.similarity.LevenshteinDistance
import persistence.PlaceRepository

/**
 * Matcher which uses a PlaceRepository to find phonetic matches. it uses a combination of BeiderMorse, Nyiis und Soundex to find it's matches.
 */
class PhoneticMatcher(
    search: String,
    private val placeRepository: PlaceRepository
) : Matcher {

    companion object {
        private val LEVENSHTEIN_DISTANCE = LevenshteinDistance()
        private val beiderMorseEncoder = BeiderMorseEncoder()
        private val nysiisEncoder = Nysiis()
        private val soundexEnocder = Soundex()
    }

    private val words = search.split("\\s+".toRegex()).filter { it.isNotEmpty() }

    override fun match(depth: Int): Pair<Boolean, Set<Match>> {
        if (depth == words.size) {
            return Pair(true, emptySet())
        }
        var wordCount = 1
        val results = mutableSetOf<Match>()
        do {
            var word = words[depth]
            for (i in 1 until wordCount) {
                word += " " + words[depth + i]
            }
            results.addAll(getPhoneticMatchesForWord(placeRepository, word)
                //TODO do we really want to do that?
                .distinctBy { it.name }
                .map {
                    Match(
                        it.id.toInt(),
                        depth + wordCount,
                        LEVENSHTEIN_DISTANCE.apply(word, it.name)
                    )
                })
            wordCount++
        } while (depth + wordCount - 1 < words.size && (wordCount <= 2 || word.length < 20))
        return Pair(false, results)
    }

    /**
     * Retrieves all places from the database that sound similar to the passed word. To calculate these matches it uses
     * the Beider-Morse and Nysiis as primary Phonetic Matchin Algorithms. If none of them delivers a result, Soundex
     * is used as fallback.
     *
     * @param word the word to find similar sounding places for.
     * @return a list of places that match to the passed word.
     */
    fun getPhoneticMatchesForWord(placeRepository: PlaceRepository, word: String): List<Place> {
        // Nysiis
        val nysiisMatches = placeRepository.findAllWhereNysiisCodeMatches(nysiisEncoder.encode(word))

        // Beider Morse
        val beiderMorseCodes = beiderMorseEncoder.encode(word).split("|")
        val beiderMorseMatches = placeRepository.findAllWhereBeiderMorseCodeMatches(beiderMorseCodes)

        // Soundex as fallback if both result sets are empty (should barely never happen but to make sure...)
        if (beiderMorseMatches.isEmpty() && nysiisMatches.isEmpty()) {
            return placeRepository.findAllWhereSoundexCodeMatches(soundexEnocder.encode(word))
        }

        val matchedPlaces = nysiisMatches.intersect(beiderMorseMatches).toList()
        return if (matchedPlaces.isNotEmpty()) {
            matchedPlaces
        } else {
            nysiisMatches.union(beiderMorseMatches).toList()
        }
    }
}