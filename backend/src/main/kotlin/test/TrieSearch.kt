package test

import org.apache.commons.text.similarity.LevenshteinDistance
import java.util.stream.Collectors

class TrieSearch(private val trie: Trie, search: String) {
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

    private val word = prepare(search)
    private val cache = mutableMapOf<Int, MutableList<Pair<Int, Int?>>>()

    fun search(): Set<List<Int>> {
        recursiveSearch(0, trie.getRoot(), 0)
        return expandResults()
    }

    private fun recursiveSearch(depth: Int, node: TrieNode, lastWordStartDepth: Int) {
        if (depth >= word.length) {
            if (node.isWord()) {
                fillCacheWithResult(lastWordStartDepth, node.getWords().first(), null) //TODO do not use first
            }
            return
        }
        //word end means we use this city, otherwise we continue until we find a valid city (=merge multiple words)
        if (word[depth] == '|' && node.isWord()) {
            if (cache.containsKey(depth + 1)) {
                if (cache[depth + 1]!!.isNotEmpty()) {
                    fillCacheWithResult(lastWordStartDepth, node.getWords().first(), depth + 1) //TODO do not use first
                }
            } else {
                recursiveSearch(depth + 1, trie.getRoot(), depth + 1)
                if (!cache.containsKey(depth + 1) || cache[depth + 1]!!.isEmpty()) {
                    //means no possible solution exists for this path
                    cache[depth + 1] = mutableListOf()
                } else {
                    //means we found values, we want to compress them
                    cleanupCache(depth + 1)
                    fillCacheWithResult(lastWordStartDepth, node.getWords().first(), depth + 1) //TODO do not use first
                }
            }
        }
        val depthForNextNode = if (word[depth] == '|') depth + 1 else depth
        val nextNodes = collectNextNodes(depthForNextNode, node)
        for (nextNode in nextNodes) {
            recursiveSearch(nextNode.first, nextNode.second, lastWordStartDepth)
        }
    }

    private fun cleanupCache(depth: Int) {
        val wordList = trie.getWordList()//TODO should not do this -> this is later in the db
        val cleanedUpCache = mutableListOf<Pair<Int, Int?>>()
        var min = Integer.MAX_VALUE
        for (cacheEntry in cache[depth]!!) {
            val wordSuffix = word.substring(depth, if (cacheEntry.second == null) word.length else cacheEntry.second!!)
                .filter { it != '|' }
            val cacheWord = prepare(wordList[cacheEntry.first]).filter { it != '|' }
            val currentDistance = LEVENSHTEIN_DISTANCE.apply(wordSuffix, cacheWord)
            if (currentDistance < min) {
                cleanedUpCache.clear()
                min = currentDistance
            }
            if (currentDistance == min) {
                cleanedUpCache.add(cacheEntry)
            }
        }
        cache[depth] = cleanedUpCache
    }

    private fun fillCacheWithResult(depth: Int, wordIndex: Int, nextSuffix: Int?) {
        if (!cache.containsKey(depth)) {
            cache[depth] = mutableListOf()
        }
        cache[depth]!!.add(Pair(wordIndex, nextSuffix))
    }

    private fun collectNextNodes(depth: Int, node: TrieNode): List<Pair<Int, TrieNode>> {
        val nextChars = mutableSetOf<String>()
        if (depth + 1 <= word.length) nextChars.add(word.substring(depth, depth + 1))
        if (depth + 2 <= word.length) nextChars.add(word.substring(depth, depth + 2))
        if (nextChars.isEmpty()) {
            return emptyList()
        }

        var fuzzyNextStrings = FUZZY_GROUPS
            .filter { it.intersect(nextChars).isNotEmpty() }
            .flatten()
            .toSet()

        if (fuzzyNextStrings.isEmpty()) {
            val nextChar = word.substring(depth, depth + 1)
            fuzzyNextStrings = setOf(nextChar, nextChar + nextChar)
        }

        val nextNodes = mutableListOf<Pair<Int, TrieNode>>()
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
                nextNodes.add(Pair(depth + 1, currentNode))
                nextNodes.add(Pair(depth + 2, currentNode))
            }
        }
        return nextNodes
    }

    private fun prepare(search: String): String {
        return search
            .filter { it.isLetter() || it.isWhitespace() }
            .map { it.toLowerCase().toString() }
            .stream().collect(Collectors.joining())
            .split("\\s+".toRegex())
            .joinToString("|")
    }

    private fun expandResults(): Set<List<Int>> {
        if (!cache.containsKey(0)) {
            return emptySet()
        }
        val results = mutableSetOf<List<Int>>()
        expandResultsRecursive(cache[0]!!, mutableListOf(), results)
        return results
    }

    private fun expandResultsRecursive(
        currentCacheEntry: MutableList<Pair<Int, Int?>>,
        currentResult: MutableList<Int>,
        results: MutableSet<List<Int>>
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