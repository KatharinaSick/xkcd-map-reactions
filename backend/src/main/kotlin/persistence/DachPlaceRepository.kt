package persistence

import model.Place
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import persistence.dao.dach.BeiderMorseEncodedDachPlaceDao
import persistence.dao.dach.DachPlaceDao
import persistence.dao.dach.NysiisEncodedDachPlaceDao
import persistence.dao.dach.SoundexEncodedDachPlaceDao
import persistence.dao.us.BeiderMorseEncodedPlaceDao
import persistence.dao.us.BeiderMorseEncodedPlaces
import persistence.dao.us.NysiisEncodedPlaceDao
import persistence.dao.us.NysiisEncodedPlaces
import persistence.dao.us.PlaceDao
import persistence.dao.us.Places
import persistence.dao.us.SoundexEncodedPlaceDao
import persistence.dao.us.SoundexEncodedPlaces

class DachPlaceRepository : PlaceRepository {

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
            DachPlaceDao
                .find { Places.name.lowerCase() eq name.toLowerCase() }
                .map { it.toModel() }
        }
    }


    override fun findAllWhereNysiisCodeMatches(nysiisCode: String): List<Place> {
        return transaction {
            NysiisEncodedDachPlaceDao
                .find { NysiisEncodedPlaces.code eq nysiisCode }
                .map { it.place.toModel() }
        }
    }

    override fun findAllWhereBeiderMorseCodeMatches(beiderMorseCode: String): List<Place> {
        return transaction {
            BeiderMorseEncodedDachPlaceDao
                .find { BeiderMorseEncodedPlaces.code eq beiderMorseCode }
                .map { it.place.toModel() }
        }
    }

    override fun findAllWhereSoundexCodeMatches(soundexCode: String): List<Place> {
        return transaction {
            try {
                SoundexEncodedDachPlaceDao
                    .find { SoundexEncodedPlaces.code eq soundexCode }
                    .map { it.place.toModel() }
            } catch (e: IllegalArgumentException) {
                ArrayList()
            }
        }
    }

    override fun findAllForIds(allPlaceIds: Set<Long>): Map<Long, Place> {
        return transaction {
            try {
                DachPlaceDao
                    .find {
                        Places.id inList allPlaceIds.map { EntityID(it,
                            Places
                        ) }
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
                DachPlaceDao
                    .all()
                    .map { it.toModel() }
                    .toList()
            } catch (e: IllegalArgumentException) {
                emptyList()
            }
        }
    }
}