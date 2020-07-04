package test

import org.apache.commons.text.similarity.LevenshteinDistance
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.GZIPInputStream
import kotlin.system.measureTimeMillis

fun main() {
    var trie: Trie? = null
    var measureTimeMillis: Long
    measureTimeMillis = measureTimeMillis {
        trie = loadTrie()
    }
    println("load took: $measureTimeMillis ms")
    measureTimeMillis = measureTimeMillis {
        search(trie!!, "truly sorry to loose a friendship this way")
//        search(trie!!, "truly sorry to loose a friendship this way truly sorry to loose a friendship this way truly sorry to loose a friendship this way")
        search(trie!!, "hope you fall in a big hole")
    }
    println("search took: $measureTimeMillis ms")
}

class Trie(bytes: ByteArray) {
    private val buffer = ByteBuffer.wrap(bytes)
    private lateinit var wordList: List<String>

    fun getWordList(): List<String> {
        return wordList
    }

    fun setWordList(words: List<String>) {
        this.wordList = words;
    }

    fun getRoot(): TrieNode {
        return TrieNode(buffer, 0)
    }
}

class TrieNode(private val buffer: ByteBuffer, private val offset: Int) {
    companion object {
        val CHAR_OFFSET = 0
        val OFFSET_OFFSET = 2
        val WORD_COUNT_OFFSET = 6
        val WORD_START_OFFSET = 7
        val WORD_SIZE = 4
    }

    fun isWord(): Boolean {
        return readWordCount() != 0.toByte()
    }

    private fun readWordCount(): Byte {
        return buffer.get(offset + WORD_COUNT_OFFSET)
    }

    fun getWords(): List<Int> {
        val words = mutableListOf<Int>()
        for (i in 0 until readWordCount()) {
            words.add(buffer.getInt(offset + WORD_START_OFFSET + WORD_SIZE * i))
        }
        return words
    }

    fun hasChild(char: Char): Boolean {
        return findChildIndex(char) != null
    }

    fun getChild(char: Char): TrieNode {
        return TrieNode(buffer, findChildIndex(char)!!)
    }

    private fun findChildIndex(char: Char): Int? {
        val endOfMyChildrenIndex = buffer.getInt(offset + OFFSET_OFFSET)
        var childIndex = offset + WORD_START_OFFSET + readWordCount() * WORD_SIZE
        while (childIndex < endOfMyChildrenIndex) {
            if (buffer.getChar(childIndex + CHAR_OFFSET) == char) {
                return childIndex
            }
            childIndex = buffer.getInt(childIndex + OFFSET_OFFSET)
        }
        return null
    }
}

fun loadTrie(): Trie {
    val input =
        GZIPInputStream(
            BufferedInputStream(
                FileInputStream(File("C:\\Users\\mableidinger\\own\\xkcd-map-reactions\\dbMigration\\src\\main\\resources\\US.trie"))
            )
        )
    val trie = Trie(input.readAllBytes())

    val words = mutableListOf<String>()
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
        }
    trie.setWordList(words)
    return trie
}

fun search(trie: Trie, search: String) {
    val results = TrieSearch(trie, search).search()
    val wordList = trie.getWordList()
    val result = results.map { it.map { wordList[it] }.joinToString(" ") }
    println("size: " + result.size)
    println(result.minBy { LevenshteinDistance().apply(search, it) })
}