package util

import model.Place
import org.apache.commons.codec.language.Nysiis
import org.apache.commons.codec.language.Soundex
import org.apache.commons.codec.language.bm.BeiderMorseEncoder
import persistence.PlaceRepository

object PhoneticMatcherUtils {
    private val beiderMorseEncoder = BeiderMorseEncoder()
    private val nysiisEncoder = Nysiis()
    private val soundexEnocder = Soundex()
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