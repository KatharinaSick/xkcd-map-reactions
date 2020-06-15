package exception

import java.net.HttpURLConnection

class NotFoundException(
    override val message: String
) : HttpException(HttpURLConnection.HTTP_NOT_FOUND, message)