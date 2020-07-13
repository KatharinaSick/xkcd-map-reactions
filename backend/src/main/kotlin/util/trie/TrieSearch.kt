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

    private val cache = mutableMapOf<Int, TrieCacheEntry>()
    private val depthsToCompute = mutableSetOf<Int>()
    private val currentTriePath = StringBuilder()

    fun search(): List<List<Int>> {
        searchDepth(0)
        while (depthsToCompute.isNotEmpty()) {
            val depth = depthsToCompute.min()!!
            depthsToCompute.remove(depth)

            cleanupDepth(depth)
            if (depth == word.length) {
                return cache[depth]!!.results.map { it.second }
            } else {
                searchDepth(depth)
            }
        }
        return emptyList()
    }

    private fun cleanupDepth(depth: Int) {
        //TODO remove old caches
        // all our children. toList() is important since the index of a child is the same for #children , #childrenSuffixIndex and #suffixes
        val children = cache[depth]!!.matches.toList()
        // childrenSuffixIndex is which child of the suffix should be used next (this relies on the children of the suffix being sorted)
        val childrenPrefixIndex = Array(children.size) { 0 }
        val prefixes =
            Array(children.size) { i ->
                val startDepth = children[i].startDepth
                if (startDepth != 0) {
                    cache[startDepth]!!.results
                } else {
                    listOf(Pair(0, emptyList<Int>()))
                }
            }

        val childrenDistances =
            Array(children.size) { i ->
                val phraseMatched = word
                    .substring(
                        children[i].startDepth, children[i].endDepth
                    )
                LEVENSHTEIN_DISTANCE.apply(phraseMatched, children[i].matchedWordInTrie)
            }

        val results = cache[depth]!!.results
        while (results.size < maxResultSize) {
            var minI = -1
            var min: Int? = null
            for (i in children.indices) {
                if (childrenPrefixIndex[i] >= prefixes[i].size) {
                    continue
                } else {
                    val distance = childrenDistances[i] + prefixes[i][childrenPrefixIndex[i]].first
                    if (min == null || distance < min) {
                        min = distance
                        minI = i
                    }
                }
            }
            if (min == null) {
                break
            } else {
                results.add(
                    Pair(min, prefixes[minI][childrenPrefixIndex[minI]].second + listOf(children[minI].wordId))
                )
                childrenPrefixIndex[minI] = childrenPrefixIndex[minI] + 1
            }
        }
    }

    private fun searchDepth(depth: Int) {
        val results = mutableSetOf<TrieMatch>()
        recursiveSearch(depth, depth, trie.getRoot(), results)

        val cacheGroups = results.groupBy { it.endDepth }
        cacheGroups.forEach {
            val suffix = it.key

            if (!cache.containsKey(suffix)) {
                depthsToCompute.add(suffix)
                cache[suffix] = TrieCacheEntry()
            }

            cache[suffix]!!.matches.addAll(it.value)
        }
    }

    private fun recursiveSearch(startDepth: Int, depth: Int, node: TrieNode, results: MutableSet<TrieMatch>) {
        if (depth >= word.length) {
            if (node.isWord()) {
                results.add(TrieMatch(node.getWord(), startDepth, depth, currentTriePath.toString()))
            }
            return
        }
        if (isAcceptableWordSplit(depth) && node.isWord()) {
            results.add(TrieMatch(node.getWord(), startDepth, depth, currentTriePath.toString()))
        }
        val nextNodes = collectNextNodes(depth, node)
        for (nextNode in nextNodes) {
            currentTriePath.append(nextNode.third)
            recursiveSearch(startDepth, nextNode.first, nextNode.second, results)
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


        val fuzzyNextStrings = mutableSetOf<String>()
        FUZZY_GROUPS.forEach {
            for (nextChar in nextChars) {
                if (it.contains(nextChar)) {
                    fuzzyNextStrings.addAll(it)
                    break
                }
            }
        }

        if (fuzzyNextStrings.isEmpty()) {
            val nextChar = word.substring(depth, depth + 1)
            fuzzyNextStrings.add(nextChar)
            fuzzyNextStrings.add(nextChar + nextChar)
        }

        val nextNodes = mutableListOf<Triple<Int, TrieNode, String>>()
        for (fuzzyNextString in fuzzyNextStrings) {
            var valid = true
            var currentNode = node
            for (char in fuzzyNextString) {
                val child = currentNode.getChild(char)
                if (child != null) {
                    currentNode = child
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
}