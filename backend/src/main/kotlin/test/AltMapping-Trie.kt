package test

import org.apache.commons.text.similarity.LevenshteinDistance
import persistence.PlaceRepository
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream
import kotlin.system.measureTimeMillis

fun main() {
    var trie: Trie? = null
    var measureTimeMillis: Long
    measureTimeMillis = measureTimeMillis {
        trie = loadTrie()
    }
    println("load took: $measureTimeMillis ms")
    placeRepository.findAllForIds(setOf(1L)) //fist query needs to create connection or something, takes way longer then subsequent calls
    measureTimeMillis = measureTimeMillis {
        search(trie!!, "truly sorry to loose a friendship this way")
        search(
            trie!!,
            "truly sorry to loose a friendship this way truly sorry to loose a friendship this way truly sorry to loose a friendship this way"
        )
        search(trie!!, "hope you fall in a big hole")
        search(trie!!, "today is a nice day and i don't have to do anything")
    }
    println("search took: $measureTimeMillis ms")
}

class Trie(bytes: ByteArray) {
    private val buffer = ByteBuffer.wrap(bytes)

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

    fun getWord(): Int {
        return buffer.getInt(offset + WORD_START_OFFSET)
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
    return Trie(input.readAllBytes())
}

val placeRepository = PlaceRepository()
fun search(trie: Trie, search: String) {
    val results = TrieSearch(trie, search).search()
    val allPlaceIds = results.flatten().map { it.toLong() }.toSet()
    val placeMappings = placeRepository.findAllForIds(allPlaceIds)
    val result = results.map { it.map { placeMappings[it.toLong()]!!.name }.joinToString(" ") }
    println("size: " + result.size)
    println(result.minBy { LevenshteinDistance().apply(search, it) })
}