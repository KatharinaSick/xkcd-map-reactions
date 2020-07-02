package test

import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors
import kotlin.system.measureTimeMillis

fun main() {
    var trie: Trie? = null
    var measureTimeMillis: Long
    measureTimeMillis = measureTimeMillis {
        trie = createTrie()
    }
    println("create took: $measureTimeMillis ms")
    measureTimeMillis = measureTimeMillis {
//        search(trie!!, "Truly sorry to loose a friend this way!")
        search(trie!!, "sniperhill")
    }
    println("search took: $measureTimeMillis ms")
}

fun search(trie: Trie, search: String) {
    val word = prepare(search)
    val results = mutableSetOf<Int>()
    recursiveSearch(0, word, trie.getRoot(), results)
    results.forEach { println(trie.getWordList()[it]) }
}

fun recursiveSearch(depth: Int, word: String, node: TrieNode, results: MutableSet<Int>) {
    if (depth < word.length) {
        val firstChar = word[depth]
        for (child in node.getChildren()) {
            if (child.key.startsWith(firstChar)) {
                var i = 1
                while (i + depth < word.length && i < child.key.length && word[depth + i] == child.key[i]) {
                    i++
                }
                if (child.key.length == i) {
                    recursiveSearch(depth + i, word, child.value, results)
                }
                break
            }
        }
    } else {
        if (node.isWord()) {
            results.addAll(node.getWords())
        }
    }
}

fun prepare(search: String): String {
    return search
        .filter { it.isLetter() }
        .map { it.toLowerCase().toString() }
        .stream().collect(Collectors.joining())
}


fun createTrie(): Trie {
    val words = mutableListOf<String>()
    val trie = Trie()
    var i = 0
    Files.lines(Paths.get("C:\\Users\\mableidinger\\own\\xkcd-map-reactions\\dbMigration\\src\\main\\resources\\US.txt"))
        .filter { it.isNotEmpty() }
        .map {
            val city = it.split("\t")[1]
            city to prepare(city)
        }
        .filter {
            it.first.isNotEmpty() && it.second.isNotEmpty()
        }
//        .limit(200)
        .forEach {
            i++
            if (i % 10000 == 0) {
                println("${String.format("%3.0f", i.toFloat() / 2_200_000.toFloat() * 100)}%: ${i}/${2_200_000}")
            }
//                println(it)
            val index = words.size
            words.add(it.first)
            trie.addWord(index, it.second)
        }
    trie.setWordList(words)
    return trie
}

class Trie {
    private lateinit var words: List<String>
    private val root = TrieNode()

    fun getRoot(): TrieNode {
        return root
    }

    fun addWord(index: Int, word: String) {
        recursiveAdd(0, word, root, index)
    }

    private fun recursiveAdd(depth: Int, word: String, node: TrieNode, wordIndex: Int) {
        var childFound = false
        val firstChar = word[depth]
        for (child in node.getChildren()) {
            if (child.key.startsWith(firstChar)) {
                childFound = true
                var i = 1
                while (i + depth < word.length && i < child.key.length && word[depth + i] == child.key[i]) {
                    i++
                }
                if (i + depth == word.length && i == child.key.length) {
                    child.value.addWord(wordIndex)
                } else if (i == child.key.length) {
                    recursiveAdd(depth + i, word, child.value, wordIndex)
                } else {
                    node.removeChild(child.key)
                    val firstHalf = child.key.substring(0, i)
                    val secondHalf = child.key.substring(i)
                    val newChild = node.addChild(firstHalf)
                    newChild.addChild(secondHalf, child.value)
                    if (i + depth == word.length) {
                        newChild.addWord(wordIndex)
                    } else {
                        recursiveAdd(i + depth, word, newChild, wordIndex)
                    }
                }
                break
            }
        }
        if (!childFound) {
            node.addChild(word.substring(depth)).addWord(wordIndex)
        }
    }

    fun setWordList(words: List<String>) {
        this.words = words;
    }

    fun getWordList(): List<String> {
        return this.words
    }
}

class TrieNode {
    var children: MutableMap<String, TrieNode>? = null
    private var wordIndex: MutableSet<Int>? = null

    fun hasChild(string: String): Boolean {
        if (children == null) {
            return false
        }
        return children!!.containsKey(string)
    }

    fun addChild(string: String, child: TrieNode? = null): TrieNode {
        if (children == null) {
            children = HashMap(0)
        }
        val newChild = child ?: TrieNode()
        children!![string] = newChild
        return newChild
    }

    fun getChild(string: String): TrieNode {
        return children!![string]!!
    }

    fun addWord(index: Int) {
        if (wordIndex == null) {
            wordIndex = HashSet(0)
        }
        wordIndex!!.add(index)
    }

    fun isWord(): Boolean {
        if (wordIndex == null) {
            return false
        }
        return wordIndex!!.isNotEmpty()
    }

    fun getWords(): Set<Int> {
        return wordIndex!!
    }

    fun getChildren(): Set<Map.Entry<String, TrieNode>> {
        if (children == null) {
            return emptySet()
        }
        return children!!.entries
    }

    fun removeChild(string: String) {
        children!!.remove(string)
    }

}
