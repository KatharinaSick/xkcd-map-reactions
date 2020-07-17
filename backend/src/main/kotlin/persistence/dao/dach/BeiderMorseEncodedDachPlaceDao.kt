package persistence.dao.dach

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object BeiderMorseEncodedDachPlaces : LongIdTable("beider_morse_encoded_dach_places") {
    val code = varchar("code", 256).index()
    val place = reference("place_id", DachPlaces)
}

class BeiderMorseEncodedDachPlaceDao(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<BeiderMorseEncodedDachPlaceDao>(
        BeiderMorseEncodedDachPlaces
    )

    var code by BeiderMorseEncodedDachPlaces.code
    var place by DachPlaceDao referencedOn BeiderMorseEncodedDachPlaces.place
}