package flighty.vm

import androidx.lifecycle.ViewModel
import flighty.data.FlightRepository
import flighty.model.AddFlightSuggestion
import flighty.model.Flight
import flighty.model.Friend
import flighty.model.Profile
import flighty.model.TravelStats
import flighty.platformName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Each screen has one ViewModel exposing a single immutable [StateFlow] of its
 * UiState (state down) and plain functions for events (events up). Screens in
 * the UI module receive the UiState and callbacks — never the ViewModel.
 */

/** App-level data the scaffolding needs: the backdrop route and id lookups. */
class AppViewModel(private val repository: FlightRepository) : ViewModel() {
    val liveFlight: Flight = repository.liveFlight
    fun flightById(id: String): Flight? = repository.flightById(id)
}

data class FlightsUiState(
    val upcoming: List<Flight>,
    val past: List<Flight>,
    val profile: Profile,
)

class FlightsViewModel(repository: FlightRepository) : ViewModel() {
    val uiState: StateFlow<FlightsUiState> = MutableStateFlow(
        FlightsUiState(
            upcoming = repository.upcomingFlights(),
            past = repository.pastFlights(),
            profile = repository.profile(),
        ),
    )
}

data class FlightDetailUiState(val flight: Flight?)

class FlightDetailViewModel(
    repository: FlightRepository,
    flightId: String,
) : ViewModel() {
    val uiState: StateFlow<FlightDetailUiState> =
        MutableStateFlow(FlightDetailUiState(repository.flightById(flightId)))
}

data class FriendsUiState(val friends: List<Friend>, val profile: Profile)

class FriendsViewModel(repository: FlightRepository) : ViewModel() {
    val uiState: StateFlow<FriendsUiState> =
        MutableStateFlow(FriendsUiState(repository.friends(), repository.profile()))
}

data class PassportUiState(
    val stats: TravelStats,
    val runningOn: String,
    val profile: Profile,
)

class PassportViewModel(repository: FlightRepository) : ViewModel() {
    val uiState: StateFlow<PassportUiState> = MutableStateFlow(
        PassportUiState(repository.travelStats(), platformName(), repository.profile()),
    )
}

data class AddFlightUiState(val suggestions: List<AddFlightSuggestion>)

class AddFlightViewModel(repository: FlightRepository) : ViewModel() {
    val uiState: StateFlow<AddFlightUiState> =
        MutableStateFlow(AddFlightUiState(repository.addFlightSuggestions()))
}
