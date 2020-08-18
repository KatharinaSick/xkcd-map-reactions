package util

/**
 * Class which uses a Matcher to perform a search. It basically is a dynamic programming algorithm.
 * It works based on the premise that no matter how you end up at a given point in the search-string, from here on out all the possible results are the same
 * This allows us to split the search into small chunks (saved in the cache) where we start from the beginning (depth=0) and work our way up to all possible other depths we reach with our Matcher.
 * As soon as the Matcher indicates we have a valid end this search returns the best n=maxResultSize solutions from this depth/cache
 */
class PhraseSearch(
    private val matcher: Matcher,
    private val maxResultSize: Int = 100
) {

    private val cache = mutableMapOf<Int, CacheEntry>()
    private val depthsToCompute = mutableSetOf<Int>()

    fun search(): List<List<Int>> {
        cache[0] = CacheEntry(
            mutableListOf(
                Pair(
                    0,
                    ResultNode(-1, null)
                )
            )
        )
        searchDepth(0)
        while (depthsToCompute.isNotEmpty()) {
            val depth = depthsToCompute.min()!!
            depthsToCompute.remove(depth)

            calculateResultsForDepth(depth)

            if (searchDepth(depth)) {
                return makeToResultList(cache[depth]!!.results)
            }
        }
        return emptyList()
    }

    private fun makeToResultList(resultChains: MutableList<Pair<Int, ResultNode>>): List<List<Int>> {
        return resultChains.map {
            val result = mutableListOf<Int>()
            var currentNode: ResultNode? = it.second
            while (currentNode != null && currentNode.wordId != -1) {
                result.add(currentNode.wordId)
                currentNode = currentNode.prefix
            }
            result.reversed()
        }
    }

    private fun calculateResultsForDepth(depth: Int) {
        // all our matches. toList() is important since the index of a match is the same for #matches, #matchPrefixIndexes and #prefixes
        val matches = cache[depth]!!.cacheMatches.toList()
        // matchPrefixIndexes is which match of the prefix should be used next (this relies on the matches of the prefix being sorted)
        val matchPrefixIndexes = Array(matches.size) { 0 }
        val prefixes =
            Array(matches.size) { i -> cache[matches[i].startDepth]!!.results }

        val matchDistances =
            Array(matches.size) { i ->
                matches[i].score
            }

        val results = cache[depth]!!.results
        while (results.size < maxResultSize) {
            var minI = -1
            var min: Int? = null
            for (i in matches.indices) {
                if (matchPrefixIndexes[i] >= prefixes[i].size) {
                    continue
                } else {
                    val distance = matchDistances[i] + prefixes[i][matchPrefixIndexes[i]].first
                    if (min == null || distance < min) {
                        min = distance
                        minI = i
                    }
                }
            }
            if (min == null) {
                break
            } else {
                val minMatch = matches[minI]
                results.add(
                    Pair(
                        min,
                        ResultNode(
                            minMatch.wordId,
                            prefixes[minI][matchPrefixIndexes[minI]].second
                        )
                    )
                )
                matchPrefixIndexes[minI] = matchPrefixIndexes[minI] + 1
            }
        }

        matches.map { it.startDepth }.toSet()
            .forEach {
                val cacheEntry = cache[it]!!
                cacheEntry.isNeededFrom.remove(depth)
                if (cacheEntry.isNeededFrom.isEmpty()) {
                    cache.remove(it)
                }
            }
    }

    private fun searchDepth(depth: Int): Boolean {
        val (done, results) = matcher.match(depth)
        if (done) {
            return true
        }

        val cacheGroups = results.groupBy { it.endDepth }
        cacheGroups.forEach {
            val suffix = it.key

            if (!cache.containsKey(suffix)) {
                depthsToCompute.add(suffix)
                cache[suffix] = CacheEntry()
            }

            cache[suffix]!!.cacheMatches.addAll(it.value.map { match ->
                CacheMatch(
                    match.wordId,
                    depth,
                    match.endDepth,
                    match.score
                )
            })
            cache[depth]!!.isNeededFrom.add(suffix)
        }
        return false
    }
}

