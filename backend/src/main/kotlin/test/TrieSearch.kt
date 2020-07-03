package test

class TrieSearch(private val trie: Trie, private val word: String) {
    companion object {
        val FUZZY_GROUPS = listOf(
            setOf("z", "zz", "s", "ss", "ts", "zs", "c", "cc"),
            ofPair("a", "e"),
            ofPair("i", "y"),
            ofPair("m", "n"),
            ofPair("o", "u"),
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

    private val results = mutableSetOf<Int>()

    fun search(): Set<Int> {
        recursiveSearch(0, trie.getRoot())
        return results
    }

    private fun recursiveSearch(depth: Int, node: TrieNode) {
        if (depth < word.length) {
            val nextNodes = collectNextNodes(depth, node)
            for (nextNode in nextNodes) {
                recursiveSearch(nextNode.first, nextNode.second)
            }
        } else {
            if (node.isWord()) {
                results.addAll(node.getWords())
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
            fuzzyNextStrings = setOf(word.substring(depth, depth + 1))
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
                nextNodes.add(Pair(depth + fuzzyNextString.length, currentNode))
            }
        }
        return nextNodes
    }
}