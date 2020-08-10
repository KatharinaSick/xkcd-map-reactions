package util

import org.apache.commons.text.similarity.LevenshteinDistance
import persistence.PlaceRepository
import util.PhoneticMatcherUtils.getPhoneticMatchesForWord

class PhoneticMatcher(
    search: String,
    private val placeRepository: PlaceRepository
) : Matcher {

    companion object {
        val LEVENSHTEIN_DISTANCE = LevenshteinDistance()
    }

    private val words = search.split("\\s+".toRegex())

    override fun match(depth: Int): Set<Match> {
        if (depth == words.size) {
            throw IllegalArgumentException("depth is too damn high!")
        }
        var wordCount = 1
        val results = mutableSetOf<Match>()
        do {
            var word = words[depth]
            for (i in 1 until wordCount) {
                word += " " + words[depth + i]
            }
            results.addAll(getPhoneticMatchesForWord(placeRepository, word)
                .distinctBy { it.name }
                .map {
                    Match(
                        it.id.toInt(),
                        depth + wordCount,
                        LEVENSHTEIN_DISTANCE.apply(word, it.name),
                        depth + wordCount == words.size
                    )
                })
            wordCount++
        } while (depth + wordCount - 1 < words.size && (wordCount <= 2 || word.length < 20))
        return results
    }
}