import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.google.gson.GsonBuilder
import exception.HttpException
import model.SimpleResponse
import service.PhraseService
import service.RouteService
import java.lang.Exception
import java.net.HttpURLConnection

class MapPhraseToRouteRequestHandler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private val phraseService = PhraseService()
    private val routeService = RouteService()

    override fun handleRequest(
        inputEvent: APIGatewayProxyRequestEvent,
        context: Context
    ): APIGatewayProxyResponseEvent {
        val phrase = inputEvent.queryStringParameters?.getOrDefault("phrase", null)

        val gson = GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .create()

        return try {
            val wordsToMap = phraseService.splitPhraseToWords(phrase)
            val route = routeService.mapPhraseToRoute(wordsToMap)
            if (route.isNotEmpty()) {
                getResponse(HttpURLConnection.HTTP_OK, gson.toJson(route))
            } else {
                getResponse(HttpURLConnection.HTTP_NOT_FOUND, gson.toJson(SimpleResponse("Route for the given phrase not found")))
            }
        } catch (e: HttpException) {
            getResponse(e.statusCode, gson.toJson(e))
        } catch (e: Exception) {
            getResponse(HttpURLConnection.HTTP_INTERNAL_ERROR, gson.toJson(SimpleResponse("Internal server error")))
        }
    }

    private fun getResponse(statusCode: Int, body: String): APIGatewayProxyResponseEvent {
        return APIGatewayProxyResponseEvent().apply {
            this.statusCode = statusCode
            this.body = body
        }
    }
}