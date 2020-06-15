package service

import exception.BadRequestException
import kotlin.streams.asSequence

class PhraseService {

    /**
     * Checks if the passed phrase is valid (not empty & contains only letters and spaces) and if yes, it splits it to
     * a list of words and returns this list.
     *
     * @param phrase the phrase to check and split.
     * @return a list of all words contained in the passed phrase.
     */
    fun splitPhraseToWords(phrase: String?): List<String> {
        if (phrase == null || phrase.isBlank()) {
            throw BadRequestException("Phrase must not be empty")
        }

        if (!phrase.chars().asSequence().all { Character.isLetter(it) || Character.isSpaceChar(it) }) {
            throw BadRequestException("Phrase must contain only alphabetical letters and spaces")
        }

        val wordsToMap = phrase
            .split(" ")
            .filter { it.isNotBlank() }
            .toList()

        if (wordsToMap.isEmpty()) {
            throw BadRequestException("Phrase must not be empty")
        }

        return wordsToMap
    }

}