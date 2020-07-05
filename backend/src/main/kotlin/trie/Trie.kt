package trie

import java.nio.ByteBuffer

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