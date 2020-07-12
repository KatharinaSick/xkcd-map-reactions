package util.trie

import org.apache.commons.text.similarity.LevenshteinDistance

class TrieSearch(
    private val trie: Trie, search: String,
    private val splitWords: Boolean = false,
    private val maxResultSize: Int = 100
) {
    companion object {
        val LEVENSHTEIN_DISTANCE = LevenshteinDistance()
        val FUZZY_GROUPS = listOf(
            setOf("z", "zz", "s", "ss", "ts", "zs"),
            setOf("ou", "oe", "ue", "uo"),
            setOf("qu", "k"),
            ofPair("c", "z"),
            ofPair("a", "e"),
            ofPair("i", "y"),
            ofPair("m", "n"),
            ofPair("o", "u"),
            ofPair("o", "a"),
            ofPair("a", "e"),
            ofPair("b", "p"),
            ofPair("g", "k"),
            ofPair("t", "d"),
            ofPair("w", "v"),
            ofPair("f", "v"),
            ofPair("k", "q")
        )

        private fun ofPair(s1: String, s2: String): Set<String> {
            return setOf(s1, s2, s1 + s1, s2 + s2, s1 + s2, s2 + s1)
        }
    }

    private val wordBeginnings = mutableSetOf<Int>()
    private val word = prepare(search)

    private val cache = mutableMapOf<Int, MutableSet<TrieCacheEntry>>()
    private val currentTriePath = StringBuilder()

    fun search(): List<List<Int>> {
        val depthsToCompute = mutableSetOf(0)
        while (depthsToCompute.isNotEmpty()) {
            val depth = depthsToCompute.first()
            depthsToCompute.remove(depth)

            val results = mutableSetOf<TrieCacheEntry>()
            recursiveSearch(depth, trie.getRoot(), results)
            cache[depth] = results

            results.forEach {
                if (it.suffix != null && !cache.containsKey(it.suffix)) {
                    depthsToCompute.add(it.suffix)
                }
            }
        }
        return collectResults()
    }

    private fun recursiveSearch(depth: Int, node: TrieNode, results: MutableSet<TrieCacheEntry>) {
        if (depth >= word.length) {
            if (node.isWord()) {
                results.add(TrieCacheEntry(node.getWord(), null, currentTriePath.toString()))
            }
            return
        }
        if (isAcceptableWordSplit(depth) && node.isWord()) {
            results.add(TrieCacheEntry(node.getWord(), depth, currentTriePath.toString()))
        }
        val nextNodes = collectNextNodes(depth, node)
        for (nextNode in nextNodes) {
            currentTriePath.append(nextNode.third)
            recursiveSearch(nextNode.first, nextNode.second, results)
            currentTriePath.setLength(currentTriePath.length - nextNode.third.length)
        }
    }

    private fun isAcceptableWordSplit(depth: Int): Boolean {
        return splitWords || wordBeginnings.contains(depth)
    }

    private fun collectNextNodes(depth: Int, node: TrieNode): List<Triple<Int, TrieNode, String>> {
        val nextChars = mutableSetOf<String>()
        if (depth + 1 <= word.length) nextChars.add(word.substring(depth, depth + 1))
        if (depth + 2 <= word.length) nextChars.add(word.substring(depth, depth + 2))
        if (nextChars.isEmpty()) {
            return emptyList()
        }

        var fuzzyNextStrings = FUZZY_GROUPS
            .filter {
                var result = false
                for (nextChar in nextChars) {
                    if (it.contains(nextChar)) {
                        result = true
                        break
                    }
                }
                result
            }
            .flatten()
            .toSet()

        if (fuzzyNextStrings.isEmpty()) {
            val nextChar = word.substring(depth, depth + 1)
            fuzzyNextStrings = setOf(nextChar, nextChar + nextChar)
        }

        val nextNodes = mutableListOf<Triple<Int, TrieNode, String>>()
        for (fuzzyNextString in fuzzyNextStrings) {
            var valid = true
            var currentNode = node
            for (char in fuzzyNextString) {
                if (currentNode.hasChild(char)) {
                    currentNode = currentNode.getChild(char)
                } else {
                    valid = false
                    break
                }
            }
            if (valid) {
                nextNodes.add(Triple(depth + 1, currentNode, fuzzyNextString))
                nextNodes.add(Triple(depth + 2, currentNode, fuzzyNextString))
            }
        }
        return nextNodes
    }

    private fun prepare(search: String): String {
        val word = StringBuilder()
        var i = 0
        var lastChar: Char? = null
        for (char in search) {
            val lowerChar = char.toLowerCase()
            if (lowerChar.isLetter()) {//TODO numbers and stuff?
                word.append(lowerChar)
                i++
            } else {
                if (lowerChar.isWhitespace() && !(lastChar == null || lastChar.isWhitespace())) {
                    wordBeginnings.add(i)
                }
            }
            lastChar = lowerChar
        }
        return word.toString()
    }

    private fun collectResults(): List<List<Int>> {
        return collectResultsRecursive(0).map { it.second }
    }

    private val collectCache = mutableMapOf<Int, List<Pair<Int, List<Int>>>>()

    /**
     * This collection works by recursivly calculating the best (=min) 100 results for a given node and then cache them into #collectCache.
     * score is the levenshstein distance + the distance for the suffix you are using
     */
    private fun collectResultsRecursive(depth: Int): List<Pair<Int, List<Int>>> {
        if (collectCache.containsKey(depth)) {
            return collectCache[depth]!!
        }
        val cacheEntry = cache[depth]
        if (cacheEntry == null || cacheEntry.isEmpty()) {
            collectCache[depth] = emptyList()
            return emptyList()
        }
        // all our children. toList() is important since the index of a child is the same for #children , #childrenSuffixIndex and #suffixes
        val children = cacheEntry.toList()
        // childrenSuffixIndex is which child of the suffix should be used next (this relies on the children of the suffix being sorted)
        val childrenSuffixIndex = Array(children.size) { 0 }
        // suffixes are all suffixes, which are a pair of current score to which places are already in the result
        val suffixes =
            Array(children.size) { i ->
                val suffix = children[i].suffix
                if (suffix != null) {
                    collectResultsRecursive(suffix)
                } else {
                    listOf(Pair(0, emptyList()))
                }
            }

        val collectedForThisNode = mutableListOf<Pair<Int, List<Int>>>()
        while (collectedForThisNode.size < maxResultSize) {
            var minI = -1
            var min: Int? = null
            for (i in children.indices) {
                val child = children[i]
                if (childrenSuffixIndex[i] >= suffixes[i].size) {
                    continue
                } else {
                    val phraseMatched = word
                        .substring(
                            depth, child.suffix ?: word.length
                        )
                    val levenshteinDistance = LEVENSHTEIN_DISTANCE.apply(phraseMatched, child.matchedWordInTrie)
                    val distance = if (child.suffix == null) {
                        levenshteinDistance
                    } else {
                        levenshteinDistance + suffixes[i][childrenSuffixIndex[i]].first
                    }
                    if (min == null || distance < min) {
                        min = distance
                        minI = i
                    }
                }
            }
            if (min == null) {
                break
            } else {
                collectedForThisNode.add(
                    Pair(min, listOf(children[minI].wordId) + suffixes[minI][childrenSuffixIndex[minI]].second)
                )
                childrenSuffixIndex[minI] = childrenSuffixIndex[minI] + 1
            }
        }
        collectCache[depth] = collectedForThisNode
        return collectedForThisNode
    }
}