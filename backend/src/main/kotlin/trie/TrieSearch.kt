package trie

import org.apache.commons.text.similarity.LevenshteinDistance

class TrieSearch(private val trie: Trie, search: String, private val splitWords: Boolean = false) {
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

    private val cache = mutableMapOf<Int, MutableSet<Triple<Int, Int?, String>>>()
    private val currentTriePath = StringBuilder()

    fun search(): List<List<Int>> {
        val depthsToCompute = mutableSetOf(0)
        while (depthsToCompute.isNotEmpty()) {
            val depth = depthsToCompute.first()
            depthsToCompute.remove(depth)

            val results = mutableSetOf<Triple<Int, Int?, String>>()
            recursiveSearch(depth, trie.getRoot(), results)
            cache[depth] = results

            results.forEach {
                if (it.second != null && !cache.containsKey(it.second!!)) {
                    depthsToCompute.add(it.second!!)
                }
            }
        }
        cleanupCaches()
        return expandResults()
    }

    private fun recursiveSearch(depth: Int, node: TrieNode, results: MutableSet<Triple<Int, Int?, String>>) {
        if (depth >= word.length) {
            if (node.isWord()) {
                results.add(Triple(node.getWord(), null, currentTriePath.toString()))
            }
            return
        }
        if (isAcceptableWordSplit(depth) && node.isWord()) {
            results.add(Triple(node.getWord(), depth, currentTriePath.toString()))
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

    private fun cleanupCaches() {
        //TODO maybe we can think of something smarter then this?
        //TOOD also maybe we find a generic way to limit result size? split words mode makes this way too huge
        for (cacheEntry in cache) {
            if (cacheEntry.value.isEmpty()) {
                continue
            }
            val distances = cacheEntry.value.map { cacheValue ->
                val wordSuffix = word
                    .substring(
                        cacheEntry.key, if (cacheValue.second == null) word.length else cacheValue.second!!
                    )
                    .filter { it != '|' }
                val cacheWord = cacheValue.third
                cacheValue to LEVENSHTEIN_DISTANCE.apply(wordSuffix, cacheWord)
                    .toDouble() / wordSuffix.length.toDouble()
            }
            val min = distances.minBy { it.second }!!.second
            val cleanedUpCache = distances
                .filter { it.second <= min + 0.25 }
                .map { it.first }.toMutableSet()
            cache[cacheEntry.key] = cleanedUpCache
        }
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
            if (char.isLetter()) {//TODO numbers and stuff?
                word.append(char)
                i++
            } else {
                if (char.isWhitespace() && !(lastChar == null || lastChar.isWhitespace())) {
                    wordBeginnings.add(i)
                }
            }
            lastChar = char
        }
        return word.toString()
    }

    private fun expandResults(): List<List<Int>> {
        if (!cache.containsKey(0)) {
            return emptyList()
        }
        val results = mutableListOf<List<Int>>()
        expandResultsRecursive(cache[0]!!, mutableListOf(), results)
        return results
    }

    private fun expandResultsRecursive(
        currentCacheEntry: MutableSet<Triple<Int, Int?, String>>,
        currentResult: MutableList<Int>,
        results: MutableList<List<Int>>
    ) {
        for (entry in currentCacheEntry) {
            currentResult.add(entry.first)
            if (entry.second != null) {
                expandResultsRecursive(cache[entry.second!!]!!, currentResult, results)
            } else {
                results.add(currentResult.toList())
            }
            currentResult.removeAt(currentResult.size - 1)
        }
    }
}