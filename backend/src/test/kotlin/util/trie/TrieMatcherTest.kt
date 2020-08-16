package util.trie

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import util.Match
import java.io.ByteArrayOutputStream

internal class TrieMatcherTest {

    @Test
    fun `test empty trie is empty result`() {
        test(0, "search", emptyList(), false, emptySet())
    }

    @Test
    fun `test single search is single result`() {
        test(
            0, "s",
            listOf(
                "s"
            ),
            false, setOf(
                Match(0, 1, 0)
            )
        )
    }

    @Test
    fun `test single search with end index is done`() {
        test(
            1, "s",
            listOf(
                "s"
            ),
            true, setOf(
            )
        )
    }

    @Test
    fun `test fuzzy searches`() {
        test(
            0, "m",
            listOf(
                "m",
                "n"
            ),
            false, setOf(
                Match(0, 1, 0),
                Match(1, 1, 1)
            )
        )
    }

    @Test
    fun `test search word in phrase`() {
        test(
            0, "mountain is high",
            listOf(
                "mountain",
                "anotherword"
            ),
            false, setOf(
                Match(0, 8, 0)
            )
        )
    }

    @Test
    fun `test search second word in phrase`() {
        test(
            4, "this mountain is high",
            listOf(
                "mountain",
                "anotherword"
            ),
            false, setOf(
                Match(0, 12, 0)
            )
        )
    }

    @Test
    fun `test score is levenshtein distance`() {
        test(
            0, "moontaim",
            listOf(
                "mountain"
            ),
            false, setOf(
                Match(0, 8, 2)
            )
        )
    }

    @Test
    fun `test splitwords deactivated`() {
        test(
            0, "splitword",
            listOf(
                "split",
                "word"
            ),
            false, setOf(
            ),
            false
        )
    }

    @Test
    fun `test splitwords activated`() {
        test(
            0, "splitword",
            listOf(
                "split",
                "word"
            ),
            false, setOf(
                Match(0,5,0),
                Match(0,6,1) //this result is because it is possible to "skip" chars through fuzzy matching
            ),
            true
        )
    }

    @Test
    fun `test splitwords activated second word`() {
        test(
            5, "splitword",
            listOf(
                "split",
                "word"
            ),
            false, setOf(
                Match(1,9,0)
            ),
            true
        )
    }

    @Test
    fun `test combine words`() {
        test(
            0, "split word",
            listOf(
                "splitword"
            ),
            false, setOf(
                Match(0,9,0)
            )
        )
    }

    private fun test(
        depth: Int,
        search: String,
        inputWords: List<String>,
        done: Boolean,
        results: Set<Match>,
        splitWords: Boolean = false
    ) {
        val trie = createTrie(inputWords.mapIndexed { id, word -> id.toLong() to word })
        trie.calculateOffsets()
        val out = ByteArrayOutputStream()
        saveTrie(out, trie)
        val result = TrieMatcher(search, Trie(out.toByteArray()), splitWords).match(depth)

        Assertions.assertEquals(done, result.first)
        Assertions.assertEquals(results, result.second)
    }

}
