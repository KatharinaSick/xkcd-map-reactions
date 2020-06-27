package test

import org.apache.commons.codec.language.bm.BeiderMorseEncoder
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
        search(trie!!, "znQpYril")
        println(BeiderMorseEncoder().encode("sniperhill"))
    }
    println("search took: $measureTimeMillis ms")
}

fun search(trie: Trie, search: String) {
    val word = prepare(search)
    val results = mutableSetOf<String>()
    recursiveSearch(StringBuilder(), 0, word, trie.getRoot(), results)
    results.forEach { println(it) }
}

fun recursiveSearch(currentWord: StringBuilder, depth: Int, word: String, node: TrieNode, results: MutableSet<String>) {
    if (node.isWord()) {
        results.add(currentWord.toString())
    }
    if (depth < word.length) {
        val c = word[depth]
        if (node.hasChild(c)) {
            currentWord.append(c)
            recursiveSearch(currentWord, depth + 1, word, node.getChild(c), results)
            currentWord.setLength(currentWord.length - 1)
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
    val encoder = BeiderMorseEncoder()
    val words = mutableListOf<String>()
    val trie = Trie()
    var i = 0
    Files.lines(Paths.get("E:\\Projects\\xkcd-map-reactions\\dbMigration\\src\\main\\resources\\US.txt"))
            .map { prepare(it.split("\t")[1]) }
            .map { it to encoder.encode(it).split("|") }
            .limit(200)
            .forEach {
                i++
                if (i % 10000 == 0) {
                    println("${String.format("%3.0f", i.toFloat() / 2_200_000.toFloat() * 100)}%: ${i}/${2_200_000}")
                }
//                println(it)
                val index = words.size
                words.add(it.first)
                it.second.forEach { word ->
//                    println(word)
                    trie.addWord(index, word)
                }
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

    private fun recursiveAdd(depth: Int, word: String, node: TrieNode, index: Int) {
        val char = word[depth]
        val child = if (!node.hasChild(char)) {
            node.addChild(char)
        } else {
            node.getChild(char)
        }
        if (depth + 1 < word.length) {
            recursiveAdd(depth + 1, word, child, index)
        } else {
            child.addWord(index)
        }
    }

    fun setWordList(words: List<String>) {
        this.words = words;
    }
}

class TrieNode {
    var children: MutableMap<Char, TrieNode>? = null
    private var wordIndex: MutableSet<Int>? = null

    fun hasChild(char: Char): Boolean {
        if (children == null) {
            return false
        }
        return children!!.containsKey(char)
    }

    fun addChild(char: Char): TrieNode {
        if (children == null) {
            children = HashMap(0)
        }
        val child = TrieNode()
        children!![char] = child
        return child
    }

    fun getChild(char: Char): TrieNode {
        return children!![char]!!
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

    fun getChildren(): Set<Map.Entry<Char, TrieNode>> {
        if (children == null) {
            return emptySet()
        }
        return children!!.entries
    }

}
