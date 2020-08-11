package persistence

import model.Place
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import persistence.dao.dach.*
import persistence.dao.us.Places

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

    override fun findAllWhereNysiisCodeMatches(nysiisCode: String): List<Place> {
        return transaction {
            NysiisEncodedDachPlaceDao
                .find { NysiisEncodedDachPlaces.code eq nysiisCode }
                .with(NysiisEncodedDachPlaceDao::place)
                .map { it.place.toModel() }
        }
    }

    override fun findAllWhereBeiderMorseCodeMatches(beiderMorseCodes: List<String>): List<Place> {
        return transaction {
            BeiderMorseEncodedDachPlaceDao
                .find { BeiderMorseEncodedDachPlaces.code inList beiderMorseCodes }
                .with(BeiderMorseEncodedDachPlaceDao::place)
                .map { it.place.toModel() }
        }
    }

    override fun findAllWhereSoundexCodeMatches(soundexCode: String): List<Place> {
        return transaction {
            try {
                SoundexEncodedDachPlaceDao
                    .find { SoundexEncodedDachPlaces.code eq soundexCode }
                    .with(SoundexEncodedDachPlaceDao::place)
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
                        DachPlaces.id inList allPlaceIds.map {
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