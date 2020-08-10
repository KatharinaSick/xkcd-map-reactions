package persistence

import model.Place
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import persistence.dao.us.BeiderMorseEncodedPlaceDao
import persistence.dao.us.BeiderMorseEncodedPlaces
import persistence.dao.us.NysiisEncodedPlaceDao
import persistence.dao.us.NysiisEncodedPlaces
import persistence.dao.us.PlaceDao
import persistence.dao.us.Places
import persistence.dao.us.SoundexEncodedPlaceDao
import persistence.dao.us.SoundexEncodedPlaces

interface PlaceRepository {

    fun findAllWhereNameMatchesIgnoreCase(name: String): List<Place>
    fun findAllWhereNysiisCodeMatches(nysiisCode: String): List<Place>
    fun findAllWhereBeiderMorseCodeMatches(beiderMorseCodes: List<String>): List<Place>
    fun findAllWhereSoundexCodeMatches(soundexCode: String): List<Place>
    fun findAllForIds(allPlaceIds: Set<Long>): Map<Long, Place>
    fun findAll(): List<Place>
}