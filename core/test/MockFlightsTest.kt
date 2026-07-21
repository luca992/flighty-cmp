package flighty

import flighty.data.MockFlights
import flighty.model.FlightStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MockFlightsTest {

    @Test
    fun liveFlightHasValidProgress() {
        assertEquals(FlightStatus.InAir, MockFlights.live.status)
        assertTrue(MockFlights.live.progress in 0f..1f)
        assertTrue(MockFlights.live.landingIn != null)
    }

    @Test
    fun upcomingAndPastPartitionAllFlights() {
        assertEquals(
            MockFlights.all.size,
            MockFlights.upcoming.size + MockFlights.past.size,
        )
        assertTrue(MockFlights.past.all { it.status == FlightStatus.Landed })
        assertTrue(MockFlights.upcoming.none { it.status == FlightStatus.Landed })
    }

    @Test
    fun delayedFlightsCarryTheirOriginalSchedule() {
        MockFlights.all.filter { it.status == FlightStatus.Delayed }.forEach {
            assertTrue(it.scheduledDepartTime != null, "${it.number} is delayed but has no original time")
        }
    }
}
