package util

/**
 * interface the PhraseSearch class uses to match the actual phrase. the first call is always with depth 0, which has to return 0+ matches.
 * a Match describes an matching place to the input phrase starting at the (opaque) depth-argument and ending on the endDepth of the Match result.
 * a Match also has to return the if of the word/place which is matched, as well as the score of the match (smaller = better). it does not matter how the score is calculated.
 * the endDepth has to be bigger then the given startDepth.
 * the boolean part in the return argument represents if this depth is the end of the search-string, to indicate if this would be a valid solution to end here.
 */
interface Matcher {
    fun match(depth: Int): Pair<Boolean, Set<Match>>
}