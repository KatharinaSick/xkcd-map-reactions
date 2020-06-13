import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import exception.BadRequestException
import exception.HttpException
import service.RouteService
import java.net.HttpURLConnection
import java.util.*

class MapPhraseToRouteRequestHandler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private val routeService = RouteService()

    override fun handleRequest(
        inputEvent: APIGatewayProxyRequestEvent,
        context: Context
    ): APIGatewayProxyResponseEvent {

        return try {
            val response = routeService
                .mapPhraseToRoute(inputEvent.queryStringParameters?.getOrDefault("phrase", null))
                .joinToString(" , ") { it.name } // Return the list of places as a simple string for now to make it easier to read for testing

            APIGatewayProxyResponseEvent().apply {
                statusCode = HttpURLConnection.HTTP_OK
                body = response
            }
        } catch (e: HttpException) {
            APIGatewayProxyResponseEvent().apply {
                statusCode = e.statusCode
                body = e.message
            }
        } finally {
            APIGatewayProxyResponseEvent().apply {
                statusCode = HttpURLConnection.HTTP_INTERNAL_ERROR
            }
        }
    }
}