package flighty

import flighty.data.MockFlightRepository
import flighty.vm.FlightsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FlightsViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun defaultsToUpcomingFlights() {
        val repository = MockFlightRepository()
        val viewModel = FlightsViewModel(repository)

        val state = viewModel.uiState.value
        assertFalse(state.showPast)
        assertEquals(repository.upcomingFlights(), state.flights)
        assertEquals(repository.upcomingFlights().size, state.upcomingCount)
    }

    @Test
    fun togglingShowPastSwitchesTheList() {
        val repository = MockFlightRepository()
        val viewModel = FlightsViewModel(repository)

        viewModel.setShowPast(true)

        val state = viewModel.uiState.value
        assertTrue(state.showPast)
        assertEquals(repository.pastFlights(), state.flights)
    }
}
