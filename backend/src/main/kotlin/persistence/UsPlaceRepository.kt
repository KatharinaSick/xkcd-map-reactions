package persistence

import model.Place
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import persistence.dao.us.*

class UsPlaceRepository : PlaceRepository {

    init {
        Database.connect(
            "jdbc:postgresql://${System.getenv("DB_URL")}/${System.getenv("DB_NAME")}",
            driver = "org.postgresql.Driver",
            user = System.getenv("DB_USER")
                ?: "", // necessary for testing, as MockK is always calling the init block when an object is mocked
            password = System.getenv("DB_PASSWORD") ?: ""
        )
    }

    override fun findAllWhereNameMatchesIgnoreCase(name: String): List<Place> {
        return transaction {
            PlaceDao
                .find { Places.name.lowerCase() eq name.toLowerCase() }
                .map { it.toModel() }
        }
    }


    override fun findAllWhereNysiisCodeMatches(nysiisCode: String): List<Place> {
        return transaction {
            NysiisEncodedPlaceDao
                .find { NysiisEncodedPlaces.code eq nysiisCode }
                .with(NysiisEncodedPlaceDao::place)
                .map { it.place.toModel() }
        }
    }

    override fun findAllWhereBeiderMorseCodeMatches(beiderMorseCodes: List<String>): List<Place> {
        return transaction {
            BeiderMorseEncodedPlaceDao
                .find { BeiderMorseEncodedPlaces.code inList beiderMorseCodes }
                .with(BeiderMorseEncodedPlaceDao::place)
                .map { it.place.toModel() }
        }
    }

    override fun findAllWhereSoundexCodeMatches(soundexCode: String): List<Place> {
        return transaction {
            try {
                SoundexEncodedPlaceDao
                    .find { SoundexEncodedPlaces.code eq soundexCode }
                    .with(SoundexEncodedPlaceDao::place)
                    .map { it.place.toModel() }
            } catch (e: IllegalArgumentException) {
                ArrayList()
            }
        }
    }

    override fun findAllForIds(allPlaceIds: Set<Long>): Map<Long, Place> {
        return transaction {
            try {
                PlaceDao
                    .find {
                        Places.id inList allPlaceIds.map {
                            EntityID(
                                it,
                                Places
                            )
                        }
                    }
                    .map { it.id.value to it.toModel() }
                    .toMap()
            } catch (e: IllegalArgumentException) {
                emptyMap()
            }
        }
    }

    override fun findAll(): List<Place> {
        return transaction {
            try {
                PlaceDao
                    .all()
                    .map { it.toModel() }
                    .toList()
            } catch (e: IllegalArgumentException) {
                emptyList()
            }
        }
    }
}