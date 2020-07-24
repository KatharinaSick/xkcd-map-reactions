package persistence.dao.us

import model.Place
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object Places : LongIdTable("places") {
    val name = varchar("name", 256)
    val latitude = double("latitude")
    val longitude = double("longitude")
}

class PlaceDao(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PlaceDao>(Places)

    var name by Places.name
    var latitude by Places.latitude
    var longitude by Places.longitude

    fun toModel(): Place {
        return Place(id.value, name, latitude, longitude)
    }
}