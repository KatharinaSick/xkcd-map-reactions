package model

import com.google.gson.annotations.Expose

data class SimpleResponse(
    @Expose
    var message: String
)