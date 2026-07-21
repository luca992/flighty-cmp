package flighty

/** Navigation 3 back-stack keys. */
sealed interface AppScreen {
    data object Home : AppScreen
    data class FlightDetail(val flightId: String) : AppScreen
}
