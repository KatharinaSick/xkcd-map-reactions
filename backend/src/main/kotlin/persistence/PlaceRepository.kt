package persistence

import model.Place
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import persistence.dao.*

class PlaceRepository {

    init {
        Database.connect(
            "jdbc:postgresql://${System.getenv("DB_URL")}/${System.getenv("DB_NAME")}",
            driver = "org.postgresql.Driver",
            user = System.getenv("DB_USER")
                ?: "", // necessary for testing, as MockK is always calling the init block when an object is mocked
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

    fun findAllForIds(allPlaceIds: Set<Long>): Map<Long, Place> {
        return transaction {
            try {
                PlaceDao
                    .find {
                        Places.id inList allPlaceIds.map { EntityID(it, Places) }
                    }
                    .map { it.id.value to it.toModel() }
                    .toMap()
            } catch (e: IllegalArgumentException) {
                emptyMap()
            }
        }
    }

    fun findAll(): List<PlaceDao> {
        return transaction {
            try {
                PlaceDao
                    .all()
                    .toList()
            } catch (e: IllegalArgumentException) {
                emptyList()
            }
        }
    }
}