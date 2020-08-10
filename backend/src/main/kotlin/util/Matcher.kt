package util

interface Matcher {
    fun match(depth: Int): Set<Match>
}