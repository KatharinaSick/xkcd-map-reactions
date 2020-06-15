package service

import exception.BadRequestException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

internal class PhraseServiceTest {

    private val phraseService = PhraseService()

    @Test
    fun `splitPhraseToWords() throws a BadRequestException when the phrase is empty`() {
        assertThrows(BadRequestException::class.java) { phraseService.splitPhraseToWords("") }
        assertThrows(BadRequestException::class.java) { phraseService.splitPhraseToWords("   ") }
        assertThrows(BadRequestException::class.java) { phraseService.splitPhraseToWords(null) }
    }

    @Test
    fun `splitPhraseToWords() throws a BadRequestException when the phrase contains digits or special characters`() {
        assertThrows(BadRequestException::class.java) { phraseService.splitPhraseToWords("1") }
        assertThrows(BadRequestException::class.java) { phraseService.splitPhraseToWords("*") }
        assertThrows(BadRequestException::class.java) { phraseService.splitPhraseToWords("I'm") }
        assertThrows(BadRequestException::class.java) { phraseService.splitPhraseToWords("one, two") }
    }

    @Test
    fun `splitPhraseToWords() splits the phrase into words without spaces when it is valid`() {
        assertEquals(phraseService.splitPhraseToWords("Hello how are you"), listOf("Hello", "how", "are", "you"))
        assertEquals(phraseService.splitPhraseToWords("   Hello how   are you "), listOf("Hello", "how", "are", "you"))
        assertEquals(phraseService.splitPhraseToWords(
            "Ein deutscher Satz mit doofen Buchstaben enthält ä ü oder ö"),
            listOf("Ein", "deutscher", "Satz", "mit", "doofen", "Buchstaben", "enthält", "ä", "ü", "oder", "ö")
        )
        assertEquals(phraseService.splitPhraseToWords(
            "Also this strange ñ á è things work"),
            listOf("Also", "this", "strange", "ñ", "á", "è", "things", "work")
        )

    }
}