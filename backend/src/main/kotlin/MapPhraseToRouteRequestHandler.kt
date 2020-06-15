import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.google.gson.GsonBuilder
import exception.HttpException
import service.RouteService
import java.net.HttpURLConnection

class MapPhraseToRouteRequestHandler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private val routeService = RouteService()

    override fun handleRequest(
        inputEvent: APIGatewayProxyRequestEvent,
        context: Context
    ): APIGatewayProxyResponseEvent {

        val gson = GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .create()

        return try {
            val route = routeService.mapPhraseToRoute(inputEvent.queryStringParameters?.getOrDefault("phrase", null))

            APIGatewayProxyResponseEvent().apply {
                statusCode = HttpURLConnection.HTTP_OK
                body = gson.toJson(route)
            }
        } catch (e: HttpException) {
            APIGatewayProxyResponseEvent().apply {
                statusCode = e.statusCode
                body = gson.toJson(e)
            }
        } // finally not necessary, API gateway will treat any other error as internal server error anyways
    }
}