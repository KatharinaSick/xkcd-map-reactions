package util.trie

import org.apache.commons.text.similarity.LevenshteinDistance
import util.Match
import util.Matcher

class TrieMatcher(
    search: String,
    private val trie: Trie,
    private val splitWords: Boolean = false
) : Matcher {

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

    private val currentTriePath = StringBuilder()
    private var startDepth = 0

    override fun match(depth: Int): Set<Match> {
        startDepth = depth
        val results = mutableSetOf<Match>()
        recursiveSearch(depth, trie.getRoot(), results)
        return results
    }

    private fun recursiveSearch(depth: Int, node: TrieNode, results: MutableSet<Match>) {
        if (depth >= word.length) {
            if (depth == word.length && node.isWord()) {
                results.add(matched(node, depth, true))
            }
            return
        }
        if (isAcceptableWordSplit(depth) && node.isWord()) {
            results.add(matched(node, depth, false))
        }
        val nextNodes = collectNextNodes(depth, node)
        for (nextNode in nextNodes) {
            currentTriePath.append(nextNode.third)
            recursiveSearch(nextNode.first, nextNode.second, results)
            currentTriePath.setLength(currentTriePath.length - nextNode.third.length)
        }
    }

    private fun matched(node: TrieNode, endDepth: Int, end: Boolean): Match {
        return Match(
            node.getWord(),
            endDepth,
            score(currentTriePath.toString(), word.substring(startDepth, endDepth)),
            end
        )
    }

    private fun score(matchedWord: String, matchedPhrase: String): Int {
        return LEVENSHTEIN_DISTANCE.apply(matchedWord, matchedPhrase)
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