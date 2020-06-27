package test

import org.apache.commons.codec.language.bm.BeiderMorseEncoder
import org.apache.commons.text.similarity.LevenshteinDistance
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.measureTimeMillis

fun main() {
    var map: Map<String, String>? = null
    var measureTimeMillis: Long
    measureTimeMillis = measureTimeMillis {
        map = createMap()
    }
    println("create took: $measureTimeMillis ms")
    measureTimeMillis = measureTimeMillis {
        search(map!!, "sniperhill")
        search(map!!, "Truly sorry to loose a friend this way!")
    }
    println("search took: $measureTimeMillis ms")
}

fun search(map: Map<String, String>, search: String) {
    val beiderMorseEncoder = BeiderMorseEncoder()
    val preparedSearch = prepare(search)
    val root = Node(null, 0, 0)
    recurseSearch(map, preparedSearch, 0, root, beiderMorseEncoder)
    val wordList = mutableListOf<List<String>>()
    root.children.map { toWordList(it, mutableListOf(), wordList) }

    val levenshteinDistance = LevenshteinDistance()

    val bestMatch = wordList.minBy { levenshteinDistance.apply(preparedSearch, prepare(it.joinToString(""))) }

    println(bestMatch!!.joinToString(","))
}

fun toWordList(node: Node, currentTree: MutableList<String>, wordList: MutableList<List<String>>) {
    currentTree.add(node.word!!)
    if (node.children.isEmpty()) {
        wordList.add(currentTree.toList())
    } else {
        node.children.forEach { child ->
            toWordList(child, currentTree, wordList)
        }
    }
    currentTree.removeAt(currentTree.size - 1)
}

fun recurseSearch(
    map: Map<String, String>,
    search: String,
    begin: Int,
    currentNode: Node,
    beiderMorseEncoder: BeiderMorseEncoder
) {
    var end = begin + 3
    while (end <= search.length) {
        val substring = search.substring(begin, end)
        val encodedWords = beiderMorseEncoder.encode(substring).split("|")
        for (encodedWord in encodedWords) {
            if (map.containsKey(encodedWord)) {
                val newNode = Node(map[encodedWord]!!, begin, end)
                if (!currentNode.children.contains(newNode)) {
                    currentNode.children.add(newNode)
                    recurseSearch(map, search, end, newNode, beiderMorseEncoder)
                }
            }
        }
        end++
    }
}

class Node(val word: String?, val begin: Int, val end: Int) {
    val children = mutableListOf<Node>()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Node

        if (word != other.word) return false
        if (begin != other.begin) return false
        if (end != other.end) return false

        return true
    }

    override fun hashCode(): Int {
        var result = word?.hashCode() ?: 0
        result = 31 * result + begin
        result = 31 * result + end
        return result
    }


}


fun createMap(): Map<String, String> {
    val map = HashMap<String, String>(100_000_000)
    var i = 0
    Files.lines(Paths.get("C:\\Users\\mableidinger\\own\\xkcd-map-reactions\\dbMigration\\src\\main\\resources\\US-bm.txt"))
        .map { it.split("|") }
        .map { it.first() to it.drop(1) }
//        .limit(200)
        .forEach {
            i++
            if (i % 10000 == 0) {
                println("${String.format("%3.0f", i.toFloat() / 2_200_000.toFloat() * 100)}%: ${i}/${2_200_000}")
            }
//                println(it)
            it.second.forEach { word ->
//                    println(word)
                map.put(word, it.first)
            }
        }
    return map
}
