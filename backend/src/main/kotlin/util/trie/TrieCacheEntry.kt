package util.trie

data class TrieCacheEntry(
    val results: MutableList<Pair<Int,TrieResultNode>> = mutableListOf(),
    /**
     * map from startindex to all
     */
    val matches: MutableList<TrieMatch> = mutableListOf(),

    val isNeededFrom: MutableSet<Int> = mutableSetOf()
)

data class TrieResultNode (
    val wordId: Int,
    val prefix: TrieResultNode?
)

data class TrieMatch(
    /**
     * The id of the word that was matched
     */
    val wordId: Int,
    /**
     * TODO
     */
    val startDepth: Int,
    /**
     * TODO
     */
    val endDepth: Int,
    /**
     * Representation of the matched place in the trie.
     */
    val matchedWordInTrie: String
)