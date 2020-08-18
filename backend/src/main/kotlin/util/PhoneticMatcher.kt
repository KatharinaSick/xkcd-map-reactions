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
}