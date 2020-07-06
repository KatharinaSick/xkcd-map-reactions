package util.trie

data class TrieCacheEntry (
    /**
     * The id of the word that was matched
     */
    val wordId: Int,
    /**
     * Reference to the next cache entry, null if the end of the search string was reached
     */
    val suffix: Int?,
    /**
     * Representation of the matched place in the trie.
     */
    val matchedWordInTrie: String
)