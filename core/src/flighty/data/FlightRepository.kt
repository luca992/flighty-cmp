package flighty.data

import flighty.model.AddFlightSuggestion
import flighty.model.Flight
import flighty.model.Friend
import flighty.model.Profile
import flighty.model.TravelStats

/**
 * The data boundary of the app. ViewModels depend on this interface only, so
 * swapping the mock for a real backend never touches the presentation layer.
 */
interface FlightRepository {
    val liveFlight: Flight
    fun upcomingFlights(): List<Flight>
    fun pastFlights(): List<Flight>
    fun flightById(id: String): Flight?
    fun friends(): List<Friend>
    fun travelStats(): TravelStats
    fun addFlightSuggestions(): List<AddFlightSuggestion>
    fun profile(): Profile
}

class MockFlightRepository : FlightRepository {
    override val liveFlight: Flight = MockFlights.live
    override fun upcomingFlights(): List<Flight> = MockFlights.upcoming
    override fun pastFlights(): List<Flight> = MockFlights.past
    override fun flightById(id: String): Flight? = MockFlights.byId(id)
    override fun friends(): List<Friend> = MockFlights.friends
    override fun travelStats(): TravelStats = MockFlights.stats
    override fun addFlightSuggestions(): List<AddFlightSuggestion> = MockFlights.addFlightSuggestions
    override fun profile(): Profile = MockFlights.profile
}

/**
 * Composition root. Kept as a plain object while the graph is this small;
 * replace with proper DI when the dependency count justifies it.
 */
object AppGraph {
    val flightRepository: FlightRepository = MockFlightRepository()
}
