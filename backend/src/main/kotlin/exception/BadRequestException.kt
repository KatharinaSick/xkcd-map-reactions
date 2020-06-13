package exception

import java.net.HttpURLConnection

class BadRequestException(
    override val message: String?
) : HttpException(HttpURLConnection.HTTP_BAD_REQUEST, message)