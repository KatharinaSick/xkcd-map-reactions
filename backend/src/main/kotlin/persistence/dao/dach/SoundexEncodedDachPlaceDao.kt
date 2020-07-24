package persistence.dao.dach

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object SoundexEncodedDachPlaces : LongIdTable("soundex_encoded_dach_places") {
    val code = varchar("code", 256).index()
    val place = reference("place_id", DachPlaces)
}

class SoundexEncodedDachPlaceDao(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<SoundexEncodedDachPlaceDao>(SoundexEncodedDachPlaces)

    var code by SoundexEncodedDachPlaces.code
    var place by DachPlaceDao referencedOn SoundexEncodedDachPlaces.place
}