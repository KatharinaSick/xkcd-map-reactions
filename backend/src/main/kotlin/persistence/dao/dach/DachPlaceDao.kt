package persistence.dao.dach

import model.Place
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object DachPlaces : LongIdTable("dach_places") {
    val name = varchar("name", 256)
    val latitude = double("latitude")
    val longitude = double("longitude")
}

class DachPlaceDao(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<DachPlaceDao>(DachPlaces)

    var name by DachPlaces.name
    var latitude by DachPlaces.latitude
    var longitude by DachPlaces.longitude

    fun toModel() : Place {
        return Place(id.value, name, latitude, longitude)
    }
}