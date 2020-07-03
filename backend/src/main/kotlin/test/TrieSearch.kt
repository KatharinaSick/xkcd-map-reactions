package test

import java.util.stream.Collectors

class TrieSearch(private val trie: Trie, private val search: String) {
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
    private val currentResult = mutableListOf<Int>()

    fun search(): Set<List<Int>> {
        recursiveSearch(0, trie.getRoot())
        return results
    }

    private fun recursiveSearch(depth: Int, node: TrieNode) {
        if (depth >= word.length) {
            if (node.isWord()) {
                currentResult.add(node.getWords().first()) //TODO do not use first
                results.add(currentResult.toList())
                currentResult.removeAt(currentResult.size - 1)
            }
        } else {
            //word end means we use this city, otherwise we continue until we find a valid city (=merge multiple words)
            if (word[depth] == '|' && node.isWord()) {
                currentResult.add(node.getWords().first()) //TODO do not use first
                recursiveSearch(depth + 1, trie.getRoot())
                currentResult.removeAt(currentResult.size - 1)
            } else {
                if (depth < word.length) {
                    val depthForNextNode = if (word[depth] == '|') depth + 1 else depth
                    val nextNodes = collectNextNodes(depthForNextNode, node)
                    for (nextNode in nextNodes) {
                        recursiveSearch(nextNode.first, nextNode.second)
                    }
                }
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