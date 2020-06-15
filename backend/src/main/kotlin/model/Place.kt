package model

import com.google.gson.annotations.Expose

data class Place(
    @Expose
    var name: String,

    @Expose
    var latitude: Double,

    @Expose
    var longitude: Double
)