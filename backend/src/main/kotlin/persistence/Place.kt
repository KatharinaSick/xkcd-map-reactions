package persistence

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object Places : LongIdTable("places") {
    val name = varchar("name", 256)
    val latitude = double("latitude")
    val longitude = double("longitude")
}

class Place(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Place>(Places)

    var name by Places.name
    var latitude by Places.latitude
    var longitude by Places.longitude
}