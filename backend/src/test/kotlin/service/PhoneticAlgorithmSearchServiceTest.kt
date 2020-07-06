package service

import exception.BadRequestException
import exception.NotFoundException
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.OverrideMockKs
import io.mockk.junit5.MockKExtension
import model.Place
import org.apache.commons.codec.language.Nysiis
import org.apache.commons.codec.language.Soundex
import org.apache.commons.codec.language.bm.BeiderMorseEncoder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import persistence.PlaceRepository

@ExtendWith(MockKExtension::class)
internal class PhoneticAlgorithmSearchServiceTest {

    @MockK
    lateinit var placeRepository: PlaceRepository

    @OverrideMockKs
    var phoneticAlgorithmSearchService = PhoneticAlgorithmSearchService()


    private val testWord = "test"
    private val beiderMorseTestCodes = BeiderMorseEncoder().encode(testWord).split("\\|")
    private val nysiisTestCode = Nysiis().encode(testWord)
    private val soundexTestCode = Soundex().encode(testWord)

    private val place1 = Place("place 1", 1.0, 2.0)
    private val place2 = Place("place 2", 1.0, 2.0)
    private val place3 = Place("place 3", 1.0, 2.0)
    private val place4 = Place("place 4", 1.0, 2.0)

    @Test
    fun `mapPhraseToRoute() throws a BadRequestException when the list of words to map is empty`() {
        assertThrows(BadRequestException::class.java) { phoneticAlgorithmSearchService.mapPhraseToRoute("") }
    }

    @Test
    fun `mapPhraseToRoute() throws a NotFoundException when the a word can't be matched`() {
        every { placeRepository.findAllWhereNameMatchesIgnoreCase(testWord) } returns listOf()
        beiderMorseTestCodes.forEach { every { placeRepository.findAllWhereBeiderMorseCodeMatches(it) } returns listOf() }
        every { placeRepository.findAllWhereNysiisCodeMatches(nysiisTestCode) } returns listOf()
        every { placeRepository.findAllWhereSoundexCodeMatches(soundexTestCode) } returns listOf()

        assertThrows(NotFoundException::class.java) { phoneticAlgorithmSearchService.mapPhraseToRoute(testWord) }
    }

    @Test
    fun `mapPhraseToRoute() returns an exact match for a word when it exists`() {
        every { placeRepository.findAllWhereNameMatchesIgnoreCase(testWord) } returns listOf(place1)
        beiderMorseTestCodes.forEach {
            every { placeRepository.findAllWhereBeiderMorseCodeMatches(it) } returns listOf(place2, place3)
        }
        every { placeRepository.findAllWhereNysiisCodeMatches(nysiisTestCode) } returns listOf(place1, place3)
        every { placeRepository.findAllWhereSoundexCodeMatches(soundexTestCode) } returns listOf(place4)

        assertEquals(phoneticAlgorithmSearchService.mapPhraseToRoute(testWord), listOf(place1))
    }

    @Test
    fun `mapPhraseToRoute() returns a match contained in nysiis and beider morse results when possible`() {
        every { placeRepository.findAllWhereNameMatchesIgnoreCase(testWord) } returns listOf()
        beiderMorseTestCodes.forEach {
            every { placeRepository.findAllWhereBeiderMorseCodeMatches(it) } returns listOf(place2, place3)
        }
        every { placeRepository.findAllWhereNysiisCodeMatches(nysiisTestCode) } returns listOf(place1, place3)
        every { placeRepository.findAllWhereSoundexCodeMatches(soundexTestCode) } returns listOf(place4)
        assertEquals(phoneticAlgorithmSearchService.mapPhraseToRoute(testWord), listOf(place3))

        every { placeRepository.findAllWhereNysiisCodeMatches(nysiisTestCode) } returns listOf(place1, place2, place3)
        assertTrue(listOf(place2, place3).containsAll(phoneticAlgorithmSearchService.mapPhraseToRoute(testWord)!!))
    }

    @Test
    fun `mapPhraseToRoute() returns a match contained in nysiis or beider morse results when possible`() {
        // only beider morse
        every { placeRepository.findAllWhereNameMatchesIgnoreCase(testWord) } returns listOf()
        beiderMorseTestCodes.forEach {
            every { placeRepository.findAllWhereBeiderMorseCodeMatches(it) } returns listOf(place2, place3)
        }
        every { placeRepository.findAllWhereNysiisCodeMatches(nysiisTestCode) } returns listOf()
        every { placeRepository.findAllWhereSoundexCodeMatches(soundexTestCode) } returns listOf(place4)
        var result = phoneticAlgorithmSearchService.mapPhraseToRoute(testWord)!!
        assertTrue(listOf(place2, place3).containsAll(result))
        assertFalse(listOf(place1, place4).containsAll(result))

        // only nysiis
        beiderMorseTestCodes.forEach {
            every { placeRepository.findAllWhereBeiderMorseCodeMatches(it) } returns listOf()
        }
        every { placeRepository.findAllWhereNysiisCodeMatches(nysiisTestCode) } returns listOf(place1, place4)
        result = phoneticAlgorithmSearchService.mapPhraseToRoute(testWord)!!
        assertTrue(listOf(place1, place4).containsAll(result))
        assertFalse(listOf(place2, place3).containsAll(result))

        // one of both
        beiderMorseTestCodes.forEach {
            every { placeRepository.findAllWhereBeiderMorseCodeMatches(it) } returns listOf(place2)
        }
        every { placeRepository.findAllWhereNysiisCodeMatches(nysiisTestCode) } returns listOf(place1)
        result = phoneticAlgorithmSearchService.mapPhraseToRoute(testWord)!!
        assertTrue(listOf(place1, place2).containsAll(result))
        assertFalse(listOf(place3, place4).containsAll(result))
    }

    @Test
    fun `mapPhraseToRoute() returns a match contained in soundex results when no other algorithm matched`() {
        every { placeRepository.findAllWhereNameMatchesIgnoreCase(testWord) } returns listOf()
        beiderMorseTestCodes.forEach {
            every { placeRepository.findAllWhereBeiderMorseCodeMatches(it) } returns listOf()
        }
        every { placeRepository.findAllWhereNysiisCodeMatches(nysiisTestCode) } returns listOf()
        every { placeRepository.findAllWhereSoundexCodeMatches(soundexTestCode) } returns listOf(place3, place4)
        val result = phoneticAlgorithmSearchService.mapPhraseToRoute(testWord)!!
        assertTrue(listOf(place3, place4).containsAll(result))
        assertFalse(listOf(place1, place2).containsAll(result))
    }
}