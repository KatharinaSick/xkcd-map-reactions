import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.google.gson.GsonBuilder
import exception.HttpException
import model.SimpleResponse
import service.RouteService
import util.Region
import java.net.HttpURLConnection

class MapPhraseToRouteRequestHandler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private val routeService = RouteService()

    private val gson = GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .setPrettyPrinting()
        .create()

    override fun handleRequest(
        inputEvent: APIGatewayProxyRequestEvent,
        context: Context
    ): APIGatewayProxyResponseEvent {
        val phrase = inputEvent.queryStringParameters?.getOrDefault("phrase", null)
        val regionId = inputEvent.queryStringParameters?.getOrDefault("region", null)
        val phoneticFirst = inputEvent.queryStringParameters?.getOrDefault("phonetic_first", "false")

        val region = Region.fromId(regionId) ?: Region.US

        context.logger.log("REGION: $region\n")

        return try {
            sendResponse(HttpURLConnection.HTTP_OK, gson.toJson(routeService.mapPhraseToRoute(phrase, phoneticFirst == "true", region)))
        } catch (e: HttpException) {
            sendResponse(e.statusCode, gson.toJson(e))
        } catch (e: Exception) {
            context.logger.log(e.message)

            sendResponse(HttpURLConnection.HTTP_INTERNAL_ERROR, gson.toJson(SimpleResponse("Internal server error")))
        }
    }

    private fun sendResponse(statusCode: Int, body: String): APIGatewayProxyResponseEvent {
        return APIGatewayProxyResponseEvent().apply {
            this.statusCode = statusCode
            this.body = body
        }
    }
}