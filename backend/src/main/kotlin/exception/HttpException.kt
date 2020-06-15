package exception

import com.google.gson.annotations.Expose

abstract class HttpException(

    @Expose
    val statusCode: Int,

    @Expose
    override val message: String

) : Exception(message)