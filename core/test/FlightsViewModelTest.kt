package flighty

import flighty.data.MockFlightRepository
import flighty.vm.FlightsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlightsViewModelTest {

    @Test
    fun exposesUpcomingAndPastFlights() {
        val repository = MockFlightRepository()
        val viewModel = FlightsViewModel(repository)

        val state = viewModel.uiState.value
        assertEquals(repository.upcomingFlights(), state.upcoming)
        assertEquals(repository.pastFlights(), state.past)
        assertTrue(state.upcoming.none { it.isPast })
        assertTrue(state.past.all { it.isPast })
    }
}
