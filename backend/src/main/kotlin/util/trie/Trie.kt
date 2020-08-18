package util.trie

import java.nio.ByteBuffer

/**
 * trie implementation based on a custom binary file format
 */
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

    private val cachedIsWord = readWordCount() != 0.toByte()
    private val endOfMyChildrenIndex = buffer.getInt(offset + OFFSET_OFFSET)
    private val childStartIndex = offset + WORD_START_OFFSET + if (isWord()) WORD_SIZE else 0

    fun isWord(): Boolean {
        return cachedIsWord
    }

    private fun readWordCount(): Byte {
        return buffer.get(offset + WORD_COUNT_OFFSET)
    }

    fun getWord(): Int {
        return buffer.getInt(offset + WORD_START_OFFSET)
    }

    fun getChild(char: Char): TrieNode? {
        val childIndex = findChildIndex(char)
        return if (childIndex == null) {
            null
        } else {
            TrieNode(buffer, childIndex)
        }
    }

    private fun findChildIndex(char: Char): Int? {
        var childIndex = childStartIndex
        while (childIndex < endOfMyChildrenIndex) {
            if (buffer.getChar(childIndex + CHAR_OFFSET) == char) {
                return childIndex
            }
            childIndex = buffer.getInt(childIndex + OFFSET_OFFSET)
        }
        return null
    }
}