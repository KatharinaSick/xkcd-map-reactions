package test

import java.util.stream.Collectors

class TrieSearch(private val trie: Trie, search: String) {
    companion object {
        val FUZZY_GROUPS = listOf(
            setOf("z", "zz", "s", "ss", "ts", "zs"),
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
    private val results = mutableSetOf<List<Int>>()
    private val currentResult = mutableListOf<Pair<Int, Int>>()
    private val cache = mutableMapOf<Int, MutableList<List<Int>>>()

    fun search(): Set<List<Int>> {
        recursiveSearch(0, trie.getRoot(), 0)
        return results
    }

    private fun recursiveSearch(depth: Int, node: TrieNode, lastWordStartDepth: Int) {
        if (depth >= word.length) {
            if (node.isWord()) {
                currentResult.add(Pair(lastWordStartDepth, node.getWords().first())) //TODO do not use first
                val result = currentResult.toList()
                fillCacheWithResult(result)
                results.add(result.map { it.second })
                currentResult.removeAt(currentResult.size - 1)
            }
        } else {
            //word end means we use this city, otherwise we continue until we find a valid city (=merge multiple words)
            if (word[depth] == '|' && node.isWord()) {
                currentResult.add(Pair(lastWordStartDepth, node.getWords().first())) //TODO do not use first
                if (cache.containsKey(depth + 1)) {
                    completeFromCache(depth + 1)
                } else {
                    recursiveSearch(depth + 1, trie.getRoot(), depth + 1)
                    //TODO cleanup cache?
                }
                currentResult.removeAt(currentResult.size - 1)
            }
            val depthForNextNode = if (word[depth] == '|') depth + 1 else depth
            val nextNodes = collectNextNodes(depthForNextNode, node)
            for (nextNode in nextNodes) {
                recursiveSearch(nextNode.first, nextNode.second, lastWordStartDepth)
            }
        }
    }

    private fun fillCacheWithResult(result: List<Pair<Int, Int>>) {
        val currentSuffix = mutableListOf<Int>()
        for (pair in result.reversed()) {
            currentSuffix.add(0, pair.second)
            if (pair.first != -1) {
                if (!cache.containsKey(pair.first)) {
                    cache[pair.first] = mutableListOf()
                }
                cache[pair.first]!!.add(currentSuffix.toList())
            }
        }
    }

    private fun completeFromCache(depth: Int) {
        val originalResultSize = currentResult.size
        for (cacheResult in cache[depth]!!) {
            currentResult.addAll(cacheResult.map { -1 to it })
            val result = currentResult.toList()
            fillCacheWithResult(result)
            results.add(result.map { it.second })
            for (i in 1..currentResult.size - originalResultSize) {
                currentResult.removeAt(currentResult.size - 1)
            }
        }
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
}