package util

enum class Region(val id: String) {
    DACH("dach"), US("us");

    companion object {
        private val map = values().associateBy(Region::id)
        fun fromId(id: String?) = if (id == null) null else map[id]
    }
}