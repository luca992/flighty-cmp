package flighty.model

data class Airline(
    val code: String,
    val name: String,
    /** Brand color as 0xAARRGGBB. */
    val color: Long,
)

data class Airport(
    val code: String,
    val city: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

enum class FlightStatus {
    Scheduled,
    Delayed,
    InAir,
    Landed,
}

data class Flight(
    val id: String,
    val airline: Airline,
    val number: String,
    val origin: Airport,
    val destination: Airport,
    /** Section header the flight is grouped under, e.g. "TODAY · MON, JUL 20". */
    val dayLabel: String,
    /** Short date shown in card/detail headers, e.g. "MON, 20 JUL". */
    val dateLabel: String,
    val departTime: String,
    val arriveTime: String,
    val arrivesNextDay: Boolean = false,
    /** Original schedule, only set when [status] is [FlightStatus.Delayed]. */
    val scheduledDepartTime: String? = null,
    val status: FlightStatus,
    /** 0f..1f, only meaningful when [status] is [FlightStatus.InAir]. */
    val progress: Float = 0f,
    val landingIn: String? = null,
    /** Running late — big times render red even while in the air (Flighty style). */
    val late: Boolean = false,
    /** Small line under the departure time, e.g. "On Time · Departed 7:05 AM". */
    val departNote: String,
    /** Small line under the arrival time, e.g. "Landing in 1h 58m". */
    val arriveNote: String,
    val departTerminal: String,
    val departGate: String,
    val arriveTerminal: String,
    val arriveGate: String? = null,
    val baggageClaim: String? = null,
    val bookingCode: String? = null,
    val seat: String? = null,
    val aircraft: String,
    /** Registration and age line, e.g. "C-GFAF · First flight Aug 14, 2009". */
    val aircraftInfo: String? = null,
    val altitudeFt: Int? = null,
    val speedKmh: Int? = null,
    val distanceKm: Int,
    val duration: String,
    /** Historical on-time performance for this flight number, in percent. */
    val delayForecast: DelayForecast? = null,
    /** "Where's my plane?" — status of the inbound aircraft. */
    val inboundNote: String? = null,
) {
    val isPast: Boolean get() = status == FlightStatus.Landed
    val routeLabel: String get() = "${origin.code} → ${destination.code}"
    val cityRoute: String get() = "${origin.city} to ${destination.city}"
}

/** Share of historical outcomes for a flight number, in percent (sums to 100). */
data class DelayForecast(
    val early: Int,
    val onTime: Int,
    val late15: Int,
    val late30: Int,
    val late45Plus: Int,
    val canceled: Int,
    val diverted: Int,
    /** Average delay in minutes across observed flights. */
    val avgDelayMin: Int,
    /** Number of observed flights behind these stats. */
    val observed: Int,
)

data class Friend(
    val name: String,
    val initials: String,
    /** Avatar color as 0xAARRGGBB. */
    val color: Long,
    val flightLabel: String,
    val route: String,
    val status: FlightStatus,
    val statusNote: String,
)

/** The signed-in traveler shown in screen headers and the avatar menu. */
data class Profile(
    val name: String,
    val initials: String,
)

/** A row in the Add Flight sheet's "Frequently Used" section. */
data class AddFlightSuggestion(
    val badgeCode: String,
    /** Brand color as 0xAARRGGBB, or null for the neutral airport badge. */
    val badgeColor: Long?,
    val name: String,
    val codes: String,
)

data class TravelStats(
    val flights: Int,
    val longHaul: Int,
    val distanceKm: Int,
    /** e.g. "1.9x around the world" */
    val aroundWorld: String,
    /** e.g. "4d 17h" */
    val flightTime: String,
    val airports: Int,
    val airlines: Int,
    val monthFlights: Int,
    val monthLabel: String,
)
