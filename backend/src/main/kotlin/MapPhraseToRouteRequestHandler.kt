import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.google.gson.GsonBuilder
import exception.HttpException
import service.RouteService
import java.net.HttpURLConnection

class MapPhraseToRouteRequestHandler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    override fun handleRequest(
        inputEvent: APIGatewayProxyRequestEvent,
        context: Context
    ): APIGatewayProxyResponseEvent {

        val gson = GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .create()

        return try {
            val route = RouteService().mapPhraseToRoute(inputEvent.queryStringParameters?.getOrDefault("phrase", null))
            if (route.isNotEmpty()) {
                getResponse(HttpURLConnection.HTTP_OK, gson.toJson(route))
            } else {
                getResponse(HttpURLConnection.HTTP_NOT_FOUND, getJsonForMessage("Route for the given phrase not found"))
            }
        } catch (e: HttpException) {
            getResponse(e.statusCode, gson.toJson(e))
        } finally {
            getResponse(HttpURLConnection.HTTP_INTERNAL_ERROR, getJsonForMessage("Internal server error"))
        }
    }

    private fun getResponse(statusCode: Int, body: String): APIGatewayProxyResponseEvent {
        return APIGatewayProxyResponseEvent().apply {
            this.statusCode = statusCode
            this.body = body
        }
    }

    private fun getJsonForMessage(message: String): String {
        return "{\"message\": \"$message\"}"
    }
}