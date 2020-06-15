import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import exception.BadRequestException
import exception.NotFoundException
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.OverrideMockKs
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import model.Place
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import service.PhraseService
import service.RouteService
import java.net.HttpURLConnection

@ExtendWith(MockKExtension::class)
internal class MapPhraseToRouteRequestHandlerTest {

    @MockK
    lateinit var routeService: RouteService

    @MockK
    lateinit var phraseService: PhraseService

    @MockK
    lateinit var apiGatewayProxyRequestEvent: APIGatewayProxyRequestEvent

    @OverrideMockKs
    var mapPhraseToRouteRequestHandler = MapPhraseToRouteRequestHandler()

    private val validPhrase = "Truly sorry to loose a friend this way"
    private val validPhraseAsWordList = listOf("Truly", "sorry", "to", "loose", "a", "friend", "this", "way")

    private val emptyResultPhrase = "Empty result"
    private val emptyResultPhraseAsWordList = listOf("Empty", "result")

    private val badRequestPhrase = "bad request"
    private val badRequestPhraseAsWordList = listOf("bad", "request")

    private val notFoundPhrase = "not found"
    private val notFoundPhraseAsWordList = listOf("not", "found")

    private val internalErrorPhrase = "internal error"
    private val internalErrorPhraseAsWordList = listOf("internal", "error")

    @BeforeEach
    fun setUp() {
        every { phraseService.splitPhraseToWords(validPhrase) } returns validPhraseAsWordList
        every { routeService.mapPhraseToRoute(validPhraseAsWordList) } returns listOf(
            Place("place 1", 1.0, 2.0),
            Place("place 2", 1.0, 2.0),
            Place("place 3", 1.0, 2.0)
        )

        every { phraseService.splitPhraseToWords(emptyResultPhrase) } returns emptyResultPhraseAsWordList
        every { routeService.mapPhraseToRoute(emptyResultPhraseAsWordList) } returns listOf()

        every { phraseService.splitPhraseToWords(badRequestPhrase) } returns badRequestPhraseAsWordList
        every { routeService.mapPhraseToRoute(badRequestPhraseAsWordList) } throws BadRequestException(badRequestPhrase)

        every { phraseService.splitPhraseToWords(notFoundPhrase) } returns notFoundPhraseAsWordList
        every { routeService.mapPhraseToRoute(notFoundPhraseAsWordList) } throws NotFoundException(notFoundPhrase)

        every { phraseService.splitPhraseToWords(internalErrorPhrase) } returns internalErrorPhraseAsWordList
        every { routeService.mapPhraseToRoute(internalErrorPhraseAsWordList) } throws IndexOutOfBoundsException()
    }

    @Test
    fun `handleRequest() returns 200 when a valid route for a phrase is found`() {
        every {
            apiGatewayProxyRequestEvent.queryStringParameters?.getOrDefault("phrase", null)
        } returns validPhrase

        assertTrue(
            mapPhraseToRouteRequestHandler
                .handleRequest(apiGatewayProxyRequestEvent, mockk()).statusCode == HttpURLConnection.HTTP_OK
        )
    }

    @Test
    fun `handleRequest() returns 404 when the route for a phrase is empty`() {
        every {
            apiGatewayProxyRequestEvent.queryStringParameters?.getOrDefault("phrase", null)
        } returns emptyResultPhrase

        assertTrue(
            mapPhraseToRouteRequestHandler
                .handleRequest(apiGatewayProxyRequestEvent, mockk()).statusCode == HttpURLConnection.HTTP_NOT_FOUND
        )
    }

    @Test
    fun `handleRequest() returns the according statuscode when any HttpException is thrown`() {
        every {
            apiGatewayProxyRequestEvent.queryStringParameters?.getOrDefault("phrase", null)
        } returns badRequestPhrase

        assertTrue(
            mapPhraseToRouteRequestHandler
                .handleRequest(apiGatewayProxyRequestEvent, mockk()).statusCode == HttpURLConnection.HTTP_BAD_REQUEST
        )

        every {
            apiGatewayProxyRequestEvent.queryStringParameters?.getOrDefault("phrase", null)
        } returns notFoundPhrase
        assertTrue(
            mapPhraseToRouteRequestHandler
                .handleRequest(apiGatewayProxyRequestEvent, mockk()).statusCode == HttpURLConnection.HTTP_NOT_FOUND
        )
    }

    @Test
    fun `handleRequest() returns 500 when any non-HttpException exception is thrown`() {
        every {
            apiGatewayProxyRequestEvent.queryStringParameters?.getOrDefault("phrase", null)
        } returns internalErrorPhrase

        assertTrue(
            mapPhraseToRouteRequestHandler
                .handleRequest(apiGatewayProxyRequestEvent, mockk()).statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR
        )
    }
}
