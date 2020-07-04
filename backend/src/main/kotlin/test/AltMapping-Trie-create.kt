package test

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipOutputStream
import kotlin.system.measureTimeMillis

fun main() {
    var trie: CreateTrie? = null
    var measureTimeMillis: Long
    measureTimeMillis = measureTimeMillis {
        trie = createTrie()
    }
    println("create took: $measureTimeMillis ms")
    measureTimeMillis = measureTimeMillis {
        trie!!.calculateOffsets()
    }
    println("calculating offset took: $measureTimeMillis ms")
    measureTimeMillis = measureTimeMillis {
        saveTrie(trie!!)
    }
    println("save took: $measureTimeMillis ms")
}

fun saveTrie(trie: CreateTrie) {
    val out =
        BufferedOutputStream(
        GZIPOutputStream(
            FileOutputStream(File("C:\\Users\\mableidinger\\own\\xkcd-map-reactions\\dbMigration\\src\\main\\resources\\US.trie"))))

    trie.getRoot().getChildren().forEach {
        recursiveSaveTrie(out, it.key, it.value)
    }

    out.flush()
    out.close()
}

fun recursiveSaveTrie(out: OutputStream, char: Char, node: TrieNode) {
    out.write(char.toInt().shr(8))
    out.write(char.toInt())
    out.write(node.offset.shr(24))
    out.write(node.offset.shr(16))
    out.write(node.offset.shr(8))
    out.write(node.offset)
    out.write(node.getChildren().size)
    node.getWords().forEach {
        out.write(it.shr(24))
        out.write(it.shr(16))
        out.write(it.shr(8))
        out.write(it)
    }
    node.getChildren().forEach {
        recursiveSaveTrie(out, it.key, it.value)
    }
}

fun prepare(search: String): String {
    return search
        .filter { it.isLetter() }
        .map { it.toLowerCase().toString() }
        .stream().collect(Collectors.joining())
}

fun createTrie(): CreateTrie {
    val words = mutableListOf<String>()
    val trie = CreateTrie()
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

class CreateTrie {

    companion object {
        val NODE_SIZE = 7
        val WORD_SIZE = 4
    }

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

    fun getWordList(): List<String> {
        return this.words
    }

    fun calculateOffsets() {
        println(calculateOffsetsRecursive(0, root))
    }

    private fun calculateOffsetsRecursive(oldOffset: Int, node: TrieNode): Int {
        var myOffset = oldOffset + if (node === root) 0 else NODE_SIZE
        myOffset += node.getWords().size * WORD_SIZE
        for (child in node.getChildren()) {
            myOffset = calculateOffsetsRecursive(myOffset, child.value)
        }
        node.offset = myOffset
        return myOffset
    }
}

class TrieNode {
    var children: MutableMap<Char, TrieNode>? = null
    private var wordIndex: MutableSet<Int>? = null
    var offset: Int = 0

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
        if (wordIndex == null) {
            return emptySet()
        }
        return wordIndex!!
    }

    fun getChildren(): List<Map.Entry<Char, TrieNode>> {
        if (children == null) {
            return emptyList()
        }
        return children!!.entries.sortedBy { it.key }
    }

}
