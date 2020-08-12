package util

interface Matcher {
    fun match(depth: Int): Pair<Boolean,Set<Match>>
}