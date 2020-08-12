package util

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class PhraseSearchTest {

    @Test
    fun `test done with first match is emptyList`() {
        test(setOf(Triple(0, true, emptySet())), emptyList())
    }

    @Test
    fun `test empty with first match is emptyList`() {
        test(setOf(Triple(0, false, emptySet())), emptyList())
    }

    @Test
    fun `test one result and then done is this one result`() {
        test(setOf(
            Triple(0, false, setOf(Match(1,1,1))),
            Triple(1, true, emptySet())
        ), listOf(listOf(1)))
    }

    @Test
    fun `test two results and then done is best result`() {
        test(setOf(
            Triple(0, false, setOf(
                Match(1,1,1),
                Match(2,1,2)
            )),
            Triple(1, true, emptySet())
        ), listOf(listOf(1)),1)
    }

    @Test
    fun `test two results and then done is two results ordered by score`() {
        test(setOf(
            Triple(0, false, setOf(
                Match(1,1,1),
                Match(2,1,2)
            )),
            Triple(1, true, emptySet())
        ), listOf(listOf(1),listOf(2)), 2)
    }

    @Test
    fun `test simple chain`() {
        test(setOf(
            Triple(0, false, setOf(
                Match(1,1,1)
            )),
            Triple(1, false, setOf(
                Match(2,2,1)
            )),
            Triple(2, true, emptySet())
        ), listOf(listOf(1,2)))
    }

    @Test
    fun `test two by two chain with 4 results`() {
        test(setOf(
            Triple(0, false, setOf(
                Match(1,1,1),
                Match(2,1,4)
            )),
            Triple(1, false, setOf(
                Match(3,2,1),
                Match(4,2,2)
            )),
            Triple(2, true, emptySet())
        ), listOf(listOf(1,3),listOf(1,4),listOf(2,3),listOf(2,4)),4)
    }

    @Test
    fun `test interwoven chain`() {
        test(setOf(
            Triple(0, false, setOf(
                Match(1,1,1),
                Match(2,2,4)
            )),
            Triple(1, false, setOf(
                Match(3,2,1)
            )),
            Triple(2, true, emptySet())
        ), listOf(listOf(1,3),listOf(2)))
    }

    @Test
    fun `test split and merge chain`() {
        test(setOf(
            Triple(0, false, setOf(
                Match(1,1,1)
            )),
            Triple(1, false, setOf(
                Match(2,2,2),
                Match(3,2,1)
            )),
            Triple(2, false, setOf(
                Match(4,3,1)
            )),
            Triple(3, true, emptySet())
        ), listOf(listOf(1,3,4),listOf(1,2,4)))
    }

    @Test
    fun `test late unsuccessful stop`() {
        test(setOf(
            Triple(0, false, setOf(
                Match(1,2,2),
                Match(2,1,1)
            )),
            Triple(1, false, setOf(
                Match(3,2,1)
            )),
            Triple(2, false, setOf(
            ))
        ), emptyList())
    }

    private fun test(inputs: Collection<Triple<Int, Boolean, Set<Match>>>, expectedResult: List<List<Int>>, resultCount: Int = 100) {
        val matcher = newMatcher()

        for (input in inputs) {
            matcher.result(input.first, input.second, input.third)
        }

        val result = PhraseSearch(matcher, resultCount).search()

        Assertions.assertEquals(expectedResult, result)
    }

    private fun newMatcher(): Matcher {
        return mockk()
    }

    private fun Matcher.result(depth: Int, done: Boolean, results: Set<Match>) {
        val matcher = this
        every { matcher.match(depth) } returns Pair(done, results)
    }

}
