package util.trie

import persistence.DachPlaceRepository
import persistence.PlaceRepository
import persistence.UsPlaceRepository
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.stream.Collectors
import java.util.zip.GZIPOutputStream
import kotlin.system.measureTimeMillis

/**
 * Creates the US.trie and DACH.trie files in the resources folder, which is necessary to map phrases to routes. This file is not
 * added to git, therefore it needs to be generated before running the project for the first time.
 */
fun main() {
    createAndSaveTrie("[US]", UsPlaceRepository(), "src/main/resources/US.trie")
    createAndSaveTrie("[DACH]", DachPlaceRepository(), "src/main/resources/DACH.trie")
}

fun createAndSaveTrie(logPrefix: String, placeRepository: PlaceRepository, fileOutputPath: String) {
    var trie: CreateTrie? = null
    var measureTimeMillis: Long
    measureTimeMillis = measureTimeMillis {
        trie = createTrie(placeRepository)
    }
    println("$logPrefix create took: $measureTimeMillis ms")
    measureTimeMillis = measureTimeMillis {
        trie!!.calculateOffsets()
    }
    println("$logPrefix calculating offset took: $measureTimeMillis ms")
    measureTimeMillis = measureTimeMillis {
        saveTrie(trie!!, fileOutputPath)
    }
    println("$logPrefix save took: $measureTimeMillis ms")
}

fun saveTrie(trie: CreateTrie, outputPath: String) {
    val out =
        BufferedOutputStream(
            GZIPOutputStream(
                FileOutputStream(File(outputPath))
            )
        )

    saveTrie(out, trie)
}

fun saveTrie(out: OutputStream, trie: CreateTrie) {
    recursiveSaveTrie(out, 0.toChar(), trie.getRoot())
    out.flush()
    out.close()
}

fun recursiveSaveTrie(out: OutputStream, char: Char, node: TrieNodeCreate) {
    out.write(char.toInt().shr(8))
    out.write(char.toInt())
    out.write(node.offset.shr(24))
    out.write(node.offset.shr(16))
    out.write(node.offset.shr(8))
    out.write(node.offset)
    //TODO we only save the first id, need extra mapping somewhere
    if (node.getWords().isEmpty()) {
        out.write(0)
    } else {
        out.write(1)
        val word = node.getWords().first()
        out.write(word.shr(24))
        out.write(word.shr(16))
        out.write(word.shr(8))
        out.write(word)
    }
    node.getChildren().forEach {
        recursiveSaveTrie(out, it.key, it.value)
    }
}

fun prepare(search: String): String {
    return search
        .filter { it.isLetter() } //TODO numbers and stuff?
        .map { it.toLowerCase().toString() }
        .stream().collect(Collectors.joining())
}

fun createTrie(placeRepository: PlaceRepository): CreateTrie {
    val allPlaces = placeRepository
        .findAll()
        .filter { it.name.isNotEmpty() }
        .map {
            it.id to prepare(it.name)
        }
    return createTrie(allPlaces)
}

fun createTrie(allPlaces: Collection<Pair<Long, String>>): CreateTrie {
    val trie = CreateTrie()
    var i = 0
    allPlaces
        .filter {
            it.second.isNotEmpty()
        }
        .forEach {
            i++
            if (i % 10000 == 0) {
                println("${String.format("%3.0f", i.toFloat() / 2_200_000.toFloat() * 100)}%: ${i}/${2_200_000}")
            }
            trie.addWord(it.first.toInt(), it.second)
        }
    return trie
}

class CreateTrie {

    companion object {
        val NODE_SIZE = 7
        val WORD_SIZE = 4
    }

    private val root = TrieNodeCreate()

    fun getRoot(): TrieNodeCreate {
        return root
    }

    fun addWord(index: Int, word: String) {
        recursiveAdd(0, word, root, index)
    }

    private fun recursiveAdd(depth: Int, word: String, node: TrieNodeCreate, index: Int) {
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

    fun calculateOffsets() {
        calculateOffsetsRecursive(0, root)
    }

    private fun calculateOffsetsRecursive(oldOffset: Int, node: TrieNodeCreate): Int {
        var myOffset = oldOffset + NODE_SIZE
        myOffset += if (node.getWords().isEmpty()) 0 else WORD_SIZE
        for (child in node.getChildren()) {
            myOffset = calculateOffsetsRecursive(myOffset, child.value)
        }
        node.offset = myOffset
        return myOffset
    }
}

class TrieNodeCreate {
    var children: MutableMap<Char, TrieNodeCreate>? = null
    private var wordIndex: MutableSet<Int>? = null
    var offset: Int = 0

    fun hasChild(char: Char): Boolean {
        if (children == null) {
            return false
        }
        return children!!.containsKey(char)
    }

    fun addChild(char: Char): TrieNodeCreate {
        if (children == null) {
            children = HashMap(0)
        }
        val child = TrieNodeCreate()
        children!![char] = child
        return child
    }

    fun getChild(char: Char): TrieNodeCreate {
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

    fun getChildren(): List<Map.Entry<Char, TrieNodeCreate>> {
        if (children == null) {
            return emptyList()
        }
        return children!!.entries.sortedBy { it.key }
    }

}
