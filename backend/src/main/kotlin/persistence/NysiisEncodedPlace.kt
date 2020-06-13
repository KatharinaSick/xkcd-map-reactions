package persistence

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object NysiisEncodedPlaces : LongIdTable("nysiis_encoded_places") {
    val code = varchar("code", 256).index()
    val place = reference("place_id", Places)
}

class NysiisEncodedPlace(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<NysiisEncodedPlace>(NysiisEncodedPlaces)

    var code by NysiisEncodedPlaces.code
    var place by Place referencedOn NysiisEncodedPlaces.place
}