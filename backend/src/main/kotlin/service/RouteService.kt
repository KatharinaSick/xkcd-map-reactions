package service

import exception.BadRequestException
import exception.HttpException
import exception.NotFoundException
import model.Place
import util.MatcherType
import util.Region

class RouteService {

    private val trieSearchService = PhraseSearchService()
    private val phoneticAlgorithmSearchService = PhoneticAlgorithmSearchService()

    /**
     * Maps the passed phrase to a route (a list of places) that sounds similar.
     *
     * @param phrase the phrase to map to a route.
     * @return a list of places representing the mapped route.
     */
    @Throws(HttpException::class)
    fun mapPhraseToRoute(phrase: String?, phoneticFirst: Boolean, region: Region): List<Place> {
        if (phrase == null || phrase.isBlank()) {
            throw BadRequestException("Phrase must not be empty")
        }

        var route = if (phoneticFirst) {
            phoneticAlgorithmSearchService.mapPhraseToRoute(phrase, region)
        } else {
            trieSearchService.mapPhraseToRoute(phrase, region, MatcherType.TRIE)
        }


        if (route == null || route.isEmpty()) {
            route = if (phoneticFirst) {
                trieSearchService.mapPhraseToRoute(phrase, region, MatcherType.TRIE)
            } else {
                phoneticAlgorithmSearchService.mapPhraseToRoute(phrase, region)
            }
        }

        if (route == null || route.isEmpty()) {
            throw NotFoundException("Couldn't find a route for the given phrase.")
        }

        return route
    }
}