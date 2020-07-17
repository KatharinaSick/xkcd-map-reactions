package model

import com.google.gson.annotations.Expose

data class Place(
    var id: Long,

    @Expose
    var name: String,

    @Expose
    var latitude: Double,

    @Expose
    var longitude: Double
)