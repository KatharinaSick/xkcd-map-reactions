package util

data class CacheEntry(
    val results: MutableList<Pair<Int, ResultNode>> = mutableListOf(),

    val cacheMatches: MutableList<CacheMatch> = mutableListOf(),

    val isNeededFrom: MutableSet<Int> = mutableSetOf()
)

data class ResultNode(
    val wordId: Int,
    val prefix: ResultNode?
)

data class CacheMatch(
    /**
     * The id of the word that was matched
     */
    val wordId: Int,
    /**
     * Where this match starts
     */
    val startDepth: Int,
    /**
     * Where this match ends
     */
    val endDepth: Int,
    /**
     * score of match (lower = better)
     */
    val score: Int
)

data class Match(
    /**
     * The id of the word that was matched
     */
    val wordId: Int,
    /**
     * Where this match ends
     */
    val endDepth: Int,
    /**
     * score of match (lower = better)
     */
    val score: Int
)