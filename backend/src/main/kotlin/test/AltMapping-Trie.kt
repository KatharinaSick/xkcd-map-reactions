//package test
//
//import org.apache.commons.text.similarity.LevenshteinDistance
//import kotlin.system.measureTimeMillis
//
//fun main() {
//    var trie: Trie? = null
//    var measureTimeMillis: Long
//    measureTimeMillis = measureTimeMillis {
//        trie = loadTrie()
//    }
//    println("load took: $measureTimeMillis ms")
//    measureTimeMillis = measureTimeMillis {
////        search(trie!!, "truly sorry to loose a friendship this way")
////        search(trie!!, "truly sorry to loose a friendship this way truly sorry to loose a friendship this way truly sorry to loose a friendship this way")
//        search(trie!!, "hope you fall in a big hole")
//        search(trie!!, "hope")
//        search(trie!!, "you")
//        search(trie!!, "fall")
//        search(trie!!, "in")
//        search(trie!!, "a")
//        search(trie!!, "big")
//        search(trie!!, "hole")
//    }
//    println("search took: $measureTimeMillis ms")
//}
//
//fun loadTrie(): Trie {
//    TODO("Not yet implemented")
//}
//
//fun search(trie: Trie, search: String) {
//    val results = TrieSearch(trie, search).search()
//    val wordList = trie.getWordList()
//    val result = results.map { it.map { wordList[it] }.joinToString(" ") }
//    println("size: " + result.size)
//    println(result.minBy { LevenshteinDistance().apply(search, it) })
//}