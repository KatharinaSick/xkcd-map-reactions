package persistence.dao

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object BeiderMorseEncodedPlaces : LongIdTable("beider_morse_encoded_places") {
    val code = varchar("code", 256).index()
    val place = reference("place_id", Places)
}

class BeiderMorseEncodedPlaceDao(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<BeiderMorseEncodedPlaceDao>(BeiderMorseEncodedPlaces)

    var code by BeiderMorseEncodedPlaces.code
    var place by PlaceDao referencedOn BeiderMorseEncodedPlaces.place
}