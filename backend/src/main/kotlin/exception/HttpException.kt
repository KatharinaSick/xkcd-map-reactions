package exception

abstract class HttpException(
    val statusCode: Int,
    override val message: String?
) : Exception(message)