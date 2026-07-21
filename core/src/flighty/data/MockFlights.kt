package flighty.data

import flighty.model.AddFlightShortcut
import flighty.model.AddFlightSuggestion
import flighty.model.Airline
import flighty.model.Airport
import flighty.model.DelayForecast
import flighty.model.Flight
import flighty.model.FlightStatus
import flighty.model.Friend
import flighty.model.Profile
import flighty.model.TravelStats

object Airlines {
    val AirCanada = Airline("AC", "Air Canada", 0xFFE01F2D)
    val Flair = Airline("F8", "Flair", 0xFF2B2B2B)
    val WestJet = Airline("WS", "WestJet", 0xFF00A3A1)
}

object Airports {
    val YYZ = Airport("YYZ", "Toronto", "Lester B. Pearson Intl.", 43.6772, -79.6306)
    val YVR = Airport("YVR", "Vancouver", "Vancouver Intl.", 49.1967, -123.1815)
    val LHR = Airport("LHR", "London", "Heathrow", 51.4700, -0.4543)
    val YOW = Airport("YOW", "Ottawa", "Macdonald–Cartier Intl.", 45.3225, -75.6692)
}

/**
 * Static mock data mirroring the reference demo: an Air Canada traveler on
 * "Sun, 19 Jul" with AC 856 to London in the air and running a few minutes late.
 */
object MockFlights {

    val live = Flight(
        id = "ac856",
        airline = Airlines.AirCanada,
        number = "AC 856",
        origin = Airports.YYZ,
        destination = Airports.LHR,
        dayLabel = "LIVE",
        dateLabel = "SUN, 19 JUL",
        departTime = "8:47 PM",
        arriveTime = "8:43 AM",
        arrivesNextDay = true,
        scheduledDepartTime = "8:40 PM",
        status = FlightStatus.InAir,
        progress = 0.62f,
        landingIn = "2h 31m",
        late = true,
        departNote = "7min Late · 4h 16min ago",
        arriveNote = "9min Late · Landing in 2h 31min",
        departTerminal = "1",
        departGate = "E75",
        arriveTerminal = "2",
        baggageClaim = "4",
        bookingCode = "AC7YYZ",
        seat = "24K",
        aircraft = "Boeing 787-9",
        aircraftInfo = "C-FVND · First flight Mar 2016 · 10 years old",
        altitudeFt = 38_000,
        speedKmh = 902,
        distanceKm = 5_711,
        duration = "6h 47m",
        delayForecast = DelayForecast(
            early = 45, onTime = 38, late15 = 9, late30 = 4, late45Plus = 4,
            canceled = 0, diverted = 0, avgDelayMin = 12, observed = 33,
        ),
        inboundNote = "The aircraft is en route with only very minor delays.",
    )

    val all: List<Flight> = listOf(
        live,
        Flight(
            id = "f8501",
            airline = Airlines.Flair,
            number = "F8 501",
            origin = Airports.YYZ,
            destination = Airports.YVR,
            dayLabel = "TOMORROW · MON, JUL 20",
            dateLabel = "MON, 20 JUL",
            departTime = "9:15 AM",
            arriveTime = "11:32 AM",
            scheduledDepartTime = "8:00 AM",
            status = FlightStatus.Delayed,
            late = true,
            departNote = "1h 15min Late",
            arriveNote = "1h 2min Late",
            departTerminal = "3",
            departGate = "B14",
            arriveTerminal = "M",
            bookingCode = "F8QW3Z",
            seat = "8F",
            aircraft = "Boeing 737 MAX 8",
            aircraftInfo = "C-FLKD · First flight Jun 2021 · 5 years old",
            distanceKm = 3_355,
            duration = "5h 17m",
            delayForecast = DelayForecast(
                early = 5, onTime = 31, late15 = 22, late30 = 19, late45Plus = 21,
                canceled = 2, diverted = 0, avgDelayMin = 38, observed = 21,
            ),
            inboundNote = "Inbound aircraft still in Calgary · departure at risk.",
        ),
        Flight(
            id = "ac857",
            airline = Airlines.AirCanada,
            number = "AC 857",
            origin = Airports.LHR,
            destination = Airports.YYZ,
            dayLabel = "SAT, JUL 25",
            dateLabel = "SAT, 25 JUL",
            departTime = "12:05 PM",
            arriveTime = "3:05 PM",
            status = FlightStatus.Scheduled,
            departNote = "On Time",
            arriveNote = "On Time",
            departTerminal = "2",
            departGate = "A23",
            arriveTerminal = "1",
            bookingCode = "AC7YYZ",
            seat = "31A",
            aircraft = "Boeing 777-300ER",
            aircraftInfo = "C-FIVW · First flight Feb 2013 · 13 years old",
            distanceKm = 5_711,
            duration = "8h 0m",
            delayForecast = DelayForecast(
                early = 51, onTime = 40, late15 = 5, late30 = 2, late45Plus = 2,
                canceled = 0, diverted = 0, avgDelayMin = 6, observed = 41,
            ),
            inboundNote = "Aircraft assignment expected 24h before departure.",
        ),
        Flight(
            id = "ac115",
            airline = Airlines.AirCanada,
            number = "AC 115",
            origin = Airports.YYZ,
            destination = Airports.YVR,
            dayLabel = "TODAY · SUN, JUL 19",
            dateLabel = "SUN, 19 JUL",
            departTime = "5:30 PM",
            arriveTime = "7:33 PM",
            status = FlightStatus.Landed,
            departNote = "On Time · Departed 5:30 PM",
            arriveNote = "Landed 7:26 PM · Gate arrival 7:33 PM",
            departTerminal = "1",
            departGate = "D40",
            arriveTerminal = "M",
            arriveGate = "C48",
            baggageClaim = "23",
            bookingCode = "AC9YVR",
            seat = "14A",
            aircraft = "Airbus A330-300",
            aircraftInfo = "C-GFAF · First flight Aug 14, 2009 · 17 years old",
            distanceKm = 3_355,
            duration = "5h 3m",
            delayForecast = DelayForecast(
                early = 17, onTime = 61, late15 = 13, late30 = 5, late45Plus = 4,
                canceled = 0, diverted = 0, avgDelayMin = 12, observed = 29,
            ),
            inboundNote = "Looking good! The aircraft arrived with only very minor delays.",
        ),
        Flight(
            id = "ws706",
            airline = Airlines.WestJet,
            number = "WS 706",
            origin = Airports.YVR,
            destination = Airports.YYZ,
            dayLabel = "SUN, JUL 12",
            dateLabel = "SUN, 12 JUL",
            departTime = "1:15 PM",
            arriveTime = "8:39 PM",
            status = FlightStatus.Landed,
            departNote = "On Time · Departed 1:15 PM",
            arriveNote = "Arrived 8:39 PM",
            departTerminal = "M",
            departGate = "B22",
            arriveTerminal = "3",
            arriveGate = "C31",
            baggageClaim = "8",
            aircraft = "Boeing 737-800",
            distanceKm = 3_355,
            duration = "4h 24m",
        ),
    )

    val upcoming: List<Flight> = all.filter { !it.isPast }
    val past: List<Flight> = all.filter { it.isPast }

    fun byId(id: String): Flight? = all.firstOrNull { it.id == id }

    val addFlightShortcuts: List<AddFlightShortcut> = listOf(
        AddFlightShortcut("Return Flight", "LHR → YYZ"),
        AddFlightShortcut("Alternatives for My Next Flight", "YYZ → LHR · Today"),
    )

    val addFlightSuggestions: List<AddFlightSuggestion> = listOf(
        AddFlightSuggestion("F8", Airlines.Flair.color, "Flair", "F8 · FLE"),
        AddFlightSuggestion("AC", Airlines.AirCanada.color, "Air Canada", "AC · ACA"),
        AddFlightSuggestion("🇨🇦", null, "Lester B. Pearson Intl.", "YYZ · CYYZ · Toronto"),
        AddFlightSuggestion("🇨🇦", null, "Vancouver Intl.", "YVR · CYVR · Vancouver"),
    )

    val friends: List<Friend> = listOf(
        Friend("Sarah Chen", "SC", 0xFF6B5AE0, "AC 103", "YYZ → YVR", FlightStatus.InAir, "Landing in 42m"),
        Friend("Marcus Webb", "MW", 0xFF0FA96B, "F8 202", "YVR → YYZ", FlightStatus.Scheduled, "Departs 6:20 PM"),
        Friend("Priya Patel", "PP", 0xFFE0722F, "AC 848", "YYZ → LHR", FlightStatus.Landed, "Landed 2:14 PM"),
        Friend("Tom Okafor", "TO", 0xFFD14B8F, "WS 3410", "YOW → YYZ", FlightStatus.Delayed, "Now departs 8:05 PM"),
    )

    val profile = Profile(name = "Luca Spinazzola", initials = "LS")

    val stats = TravelStats(
        flights = 7,
        longHaul = 2,
        distanceKm = 28_359,
        aroundWorld = "0.7x around the world",
        flightTime = "1d 16h",
        airports = 5,
        airlines = 4,
        monthFlights = 3,
        monthLabel = "flights in July",
    )
}
