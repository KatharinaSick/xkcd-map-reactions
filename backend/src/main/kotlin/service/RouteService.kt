package service

import exception.BadRequestException
import exception.HttpException
import exception.NotFoundException
import model.Place

class RouteService {

    private val trieSearchService = TrieSearchService()
    private val phoneticAlgorithmSearchService = PhoneticAlgorithmSearchService()

    /**
     * Maps the passed phrase to a route (a list of places) that sounds similar.
     *
     * @param wordsToMap the phrase to map to a route.
     * @return a list of places representing the mapped route.
     */
    @Throws(HttpException::class)
    fun mapPhraseToRoute(phrase: String?): List<Place> {
        if (phrase == null || phrase.isBlank()) {
            throw BadRequestException("Phrase must not be empty")
        }

        var route = trieSearchService.mapPhraseToRoute(phrase)

        if (route == null || route.isEmpty()) {
            route = phoneticAlgorithmSearchService.mapPhraseToRoute(phrase)
        }

        if (route == null || route.isEmpty()) {
            throw NotFoundException("Couldn't find a route for the given phrase.")
        }

        return route
    }
}