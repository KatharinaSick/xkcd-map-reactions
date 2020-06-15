package persistence

import model.Place
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import persistence.dao.BeiderMorseEncodedPlaceDao
import persistence.dao.BeiderMorseEncodedPlaces
import persistence.dao.NysiisEncodedPlaceDao
import persistence.dao.NysiisEncodedPlaces
import persistence.dao.PlaceDao
import persistence.dao.Places
import persistence.dao.SoundexEncodedPlaceDao
import persistence.dao.SoundexEncodedPlaces

class PlaceRepository {

    init {
        Database.connect(
            "jdbc:postgresql://${System.getenv("DB_URL")}/${System.getenv("DB_NAME")}",
            driver = "org.postgresql.Driver",
            user = System.getenv("DB_USER") ?: "", // necessary for testing, as MockK is always calling the init block when an object is mocked
            password = System.getenv("DB_PASSWORD") ?: ""
        )
    }

    fun findAllWhereNameMatchesIgnoreCase(name: String): List<Place> {
        return transaction {
            PlaceDao
                .find { Places.name.lowerCase() eq name.toLowerCase() }
                .map { it.toModel() }
        }
    }


    fun findAllWhereNysiisCodeMatches(nysiisCode: String): List<Place> {
        return transaction {
            NysiisEncodedPlaceDao
                .find { NysiisEncodedPlaces.code eq nysiisCode }
                .map { it.place.toModel() }
        }
    }

    fun findAllWhereBeiderMorseCodeMatches(beiderMorseCode: String): List<Place> {
        return transaction {
            BeiderMorseEncodedPlaceDao
                .find { BeiderMorseEncodedPlaces.code eq beiderMorseCode }
                .map { it.place.toModel() }
        }
    }

    fun findAllWhereSoundexCodeMatches(soundexCode: String): List<Place> {
        return transaction {
            try {
                SoundexEncodedPlaceDao
                    .find { SoundexEncodedPlaces.code eq soundexCode }
                    .map { it.place.toModel() }
            } catch (e: IllegalArgumentException) {
                ArrayList()
            }
        }
    }
}