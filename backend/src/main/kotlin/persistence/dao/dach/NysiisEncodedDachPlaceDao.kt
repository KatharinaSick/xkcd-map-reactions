package persistence.dao.dach

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object NysiisEncodedDachPlaces : LongIdTable("nysiis_encoded_dach_places") {
    val code = varchar("code", 256).index()
    val place = reference("place_id", DachPlaces)
}

class NysiisEncodedDachPlaceDao(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<NysiisEncodedDachPlaceDao>(NysiisEncodedDachPlaces)

    var code by NysiisEncodedDachPlaces.code
    var place by DachPlaceDao referencedOn NysiisEncodedDachPlaces.place
}