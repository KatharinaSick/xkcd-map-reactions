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
            setOf("ä", "ae", "e", "a"),
            setOf("ü", "ue", "u"),
            setOf("ö", "oe", "o"),
            setOf("ch", "k", "g", "sch"),
            setOf("sch", "sh", "s"),
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
        ).precalc()

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
        cache[0] = TrieCacheEntry(mutableListOf(Pair(0, TrieResultNode(-1, null))))
        searchDepth(0)
        while (depthsToCompute.isNotEmpty()) {
            val depth = depthsToCompute.min()!!
            depthsToCompute.remove(depth)

            cleanupDepth(depth)
            if (depth == word.length) {
                return makeToResultList(cache[depth]!!.results)
            } else {
                searchDepth(depth)
            }
        }
        return emptyList()
    }

    private fun makeToResultList(resultChains: MutableList<Pair<Int, TrieResultNode>>): List<List<Int>> {
        return resultChains.map {
            val result = mutableListOf<Int>()
            var currentNode: TrieResultNode? = it.second
            while (currentNode != null && currentNode.wordId != -1) {
                result.add(currentNode.wordId)
                currentNode = currentNode.prefix
            }
            result.reversed()
        }
    }

    private fun cleanupDepth(depth: Int) {
        // all our matches. toList() is important since the index of a match is the same for #matches, #matchPrefixIndexes and #prefixes
        val matches = cache[depth]!!.matches.toList()
        // matchPrefixIndexes is which match of the prefix should be used next (this relies on the matches of the prefix being sorted)
        val matchPrefixIndexes = Array(matches.size) { 0 }
        val prefixes =
            Array(matches.size) { i -> cache[matches[i].startDepth]!!.results }

        val matchDistances =
            Array(matches.size) { i ->
                val phraseMatched = word
                    .substring(
                        matches[i].startDepth, matches[i].endDepth
                    )
                LEVENSHTEIN_DISTANCE.apply(phraseMatched, matches[i].matchedWordInTrie)
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
                results.add(
                    Pair(min, TrieResultNode(matches[minI].wordId, prefixes[minI][matchPrefixIndexes[minI]].second))
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
            cache[depth]!!.isNeededFrom.add(suffix)
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
        val nextChars = mutableListOf<String>()
        if (depth + 1 <= word.length) nextChars.add(word.substring(depth, depth + 1))
        if (depth + 2 <= word.length) nextChars.add(word.substring(depth, depth + 2))
        if (nextChars.isEmpty()) {
            return emptyList()
        }

        val nextNodes = mutableListOf<Triple<Int, TrieNode, String>>()
        var found = false
        nextChars.forEach { nextChar ->
            val fuzzyMatches = FUZZY_GROUPS[nextChar]
            if (fuzzyMatches != null) {
                found = true
                //TODO the heck, we are faster if we do stuff multiple times instead of deduplicating it with an hashset
                fuzzyMatches.forEach {
                    tryFuzzyString(it, depth, node, nextNodes)
                }
            }
        }

        if (!found) {
            val nextChar = word.substring(depth, depth + 1)
            tryFuzzyString(nextChar, depth, node, nextNodes)
            tryFuzzyString(nextChar + nextChar, depth, node, nextNodes)
        }

        return nextNodes
    }

    private fun tryFuzzyString(
        fuzzyMatchToTry: String,
        depth: Int,
        node: TrieNode,
        nextNodes: MutableList<Triple<Int, TrieNode, String>>
    ) {
        var valid = true
        var currentNode = node
        for (char in fuzzyMatchToTry) {
            val child = currentNode.getChild(char)
            if (child != null) {
                currentNode = child
            } else {
                valid = false
                break
            }
        }
        if (valid) {
            if (fuzzyMatchToTry.length > 1) {
                nextNodes.add(Triple(depth + fuzzyMatchToTry.length - 1, currentNode, fuzzyMatchToTry))
            }
            nextNodes.add(Triple(depth + fuzzyMatchToTry.length, currentNode, fuzzyMatchToTry))
            nextNodes.add(Triple(depth + fuzzyMatchToTry.length + 1, currentNode, fuzzyMatchToTry))
        }
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

private fun List<Set<String>>.precalc(): Map<String, Set<String>> {
    val allStrings = toSet().flatten()
    return allStrings.map { string ->
        val allMatches = filter { it.contains(string) }.flatten().toSet()
        string to allMatches
    }.toMap()
}
