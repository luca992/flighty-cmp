package flighty.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import flighty.data.FlightRepository
import flighty.model.AddFlightSuggestion
import flighty.model.Flight
import flighty.model.Friend
import flighty.model.TravelStats
import flighty.platformName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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
    val flights: List<Flight>,
    val showPast: Boolean,
    val upcomingCount: Int,
)

class FlightsViewModel(private val repository: FlightRepository) : ViewModel() {
    private val showPast = MutableStateFlow(false)

    val uiState: StateFlow<FlightsUiState> = showPast
        .map { past ->
            FlightsUiState(
                flights = if (past) repository.pastFlights() else repository.upcomingFlights(),
                showPast = past,
                upcomingCount = repository.upcomingFlights().size,
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            FlightsUiState(
                flights = repository.upcomingFlights(),
                showPast = false,
                upcomingCount = repository.upcomingFlights().size,
            ),
        )

    fun setShowPast(showPast: Boolean) {
        this.showPast.value = showPast
    }
}

data class FlightDetailUiState(val flight: Flight?)

class FlightDetailViewModel(
    repository: FlightRepository,
    flightId: String,
) : ViewModel() {
    val uiState: StateFlow<FlightDetailUiState> =
        MutableStateFlow(FlightDetailUiState(repository.flightById(flightId)))
}

data class FriendsUiState(val friends: List<Friend>)

class FriendsViewModel(repository: FlightRepository) : ViewModel() {
    val uiState: StateFlow<FriendsUiState> =
        MutableStateFlow(FriendsUiState(repository.friends()))
}

data class PassportUiState(val stats: TravelStats, val runningOn: String)

class PassportViewModel(repository: FlightRepository) : ViewModel() {
    val uiState: StateFlow<PassportUiState> =
        MutableStateFlow(PassportUiState(repository.travelStats(), platformName()))
}

data class AddFlightUiState(val suggestions: List<AddFlightSuggestion>)

class AddFlightViewModel(repository: FlightRepository) : ViewModel() {
    val uiState: StateFlow<AddFlightUiState> =
        MutableStateFlow(AddFlightUiState(repository.addFlightSuggestions()))
}
