package persistence.dao.us

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object SoundexEncodedPlaces : LongIdTable("soundex_encoded_places") {
    val code = varchar("code", 256).index()
    val place = reference("place_id", Places)
}

class SoundexEncodedPlaceDao(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<SoundexEncodedPlaceDao>(SoundexEncodedPlaces)

    var code by SoundexEncodedPlaces.code
    var place by PlaceDao referencedOn SoundexEncodedPlaces.place
}