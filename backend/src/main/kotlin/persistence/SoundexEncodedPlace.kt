package persistence

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object SoundexEncodedPlaces : LongIdTable("soundex_encoded_places") {
    val code = varchar("code", 256).index()
    val place = reference("place_id", Places)
}

class SoundexEncodedPlace(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<SoundexEncodedPlace>(SoundexEncodedPlaces)

    var code by SoundexEncodedPlaces.code
    var place by Place referencedOn SoundexEncodedPlaces.place
}