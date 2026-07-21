import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.viewmodel.compose.viewModel
import flighty.App
import flighty.FlightsFlow
import flighty.data.AppGraph
import flighty.model.Flight
import flighty.ui.AddFlightContent
import flighty.ui.FlightyColors
import flighty.ui.FlightyShell
import flighty.ui.FlightyTheme
import flighty.ui.FriendsScreen
import flighty.ui.PassportScreen
import flighty.ui.components.LocalNativeFlightMenuHost
import flighty.ui.components.LocalNativeProfileMenuHost
import flighty.ui.components.NativeFlightMenuHost
import flighty.ui.components.NativeProfileMenuHost
import flighty.vm.AddFlightViewModel
import flighty.vm.FriendsViewModel
import flighty.vm.PassportViewModel
import platform.UIKit.UIViewController

/**
 * Entry points for the native-chrome (Liquid Glass) app: SwiftUI owns the tab
 * bar, search sheet, and long-press context menus; every screen — including
 * the in-sheet Home → Detail navigation, which behaves exactly like the
 * reference demo (the map just updates behind the persistent sheet) — is the
 * same shared Compose code the full-Compose apps use.
 * Pattern per https://kotlinlang.org/docs/multiplatform/ios-liquid-glass.html
 */
private fun glassController(content: @Composable () -> Unit): UIViewController =
    ComposeUIViewController {
        FlightyTheme {
            // Same hard-edge scroll policy as the full-Compose app.
            CompositionLocalProvider(LocalOverscrollFactory provides null) {
                content()
            }
        }
    }

/**
 * Row-bounds registry backing the native context menu: Compose reports where
 * each flight row sits (window px), Swift hit-tests long-presses against it
 * and snapshots the row for the menu preview. Coordinates stored in points.
 */
private object GlassFlightMenu : NativeFlightMenuHost {
    private val rows = LinkedHashMap<String, DoubleArray>()
    var density: Float = 1f

    override fun updateRowBounds(
        flight: Flight,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ) {
        val d = density
        rows[flight.id] = doubleArrayOf(
            (left / d).toDouble(),
            (top / d).toDouble(),
            (right / d).toDouble(),
            (bottom / d).toDouble(),
        )
    }

    override fun removeRow(flightId: String) {
        rows.remove(flightId)
    }

    fun flightIdAt(x: Double, y: Double): String? = rows.entries.firstOrNull { (_, r) ->
        x >= r[0] && x <= r[2] && y >= r[1] && y <= r[3]
    }?.key

    /** [x, y, width, height] in points, or null if the row is gone. */
    fun rowRect(flightId: String): DoubleArray? = rows[flightId]?.let { r ->
        doubleArrayOf(r[0], r[1], r[2] - r[0], r[3] - r[1])
    }
}

/**
 * Bridges the profile avatar's on-screen position to Swift, which parks an
 * invisible system-menu button over it. One anchor per tab controller.
 */
class ProfileMenuAnchor : NativeProfileMenuHost {
    var listener: ((Double, Double, Double, Double) -> Unit)? = null
    internal var density: Float = 1f

    override fun updateAvatarBounds(left: Float, top: Float, right: Float, bottom: Float) {
        val d = density
        listener?.invoke(
            (left / d).toDouble(),
            (top / d).toDouble(),
            ((right - left) / d).toDouble(),
            ((bottom - top) / d).toDouble(),
        )
    }

    override fun clearAvatar() {
        listener?.invoke(0.0, 0.0, 0.0, 0.0)
    }
}

@Suppress("unused")
fun profileDisplayName(): String = AppGraph.flightRepository.profile().name

@Suppress("unused", "FunctionName")
fun FlightsTabController(
    onDetailShown: (String?) -> Unit,
    profileAnchor: ProfileMenuAnchor,
): UIViewController = glassController {
    GlassFlightMenu.density = LocalDensity.current.density
    profileAnchor.density = LocalDensity.current.density
    CompositionLocalProvider(
        LocalNativeFlightMenuHost provides GlassFlightMenu,
        LocalNativeProfileMenuHost provides profileAnchor,
    ) {
        FlightsFlow(onDetailShown = onDetailShown)
    }
}

@Suppress("unused")
fun flightMenuFlightIdAt(x: Double, y: Double): String? = GlassFlightMenu.flightIdAt(x, y)

@Suppress("unused")
fun flightMenuRowRect(flightId: String): DoubleArray? = GlassFlightMenu.rowRect(flightId)

/** Origin and destination airport codes for the menu's Open in Maps submenu. */
@Suppress("unused")
fun flightMenuAirports(flightId: String): List<String> =
    AppGraph.flightRepository.flightById(flightId)
        ?.let { listOf(it.origin.code, it.destination.code) }
        ?: emptyList()

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("unused", "FunctionName")
fun FriendsTabController(profileAnchor: ProfileMenuAnchor): UIViewController = glassController {
    profileAnchor.density = LocalDensity.current.density
    val friendsViewModel = viewModel { FriendsViewModel(AppGraph.flightRepository) }
    val state by friendsViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    CompositionLocalProvider(LocalNativeProfileMenuHost provides profileAnchor) {
        FlightyShell(
            backdropFlight = null,
            detail = false,
            contentAtTop = { scrollState.value == 0 },
        ) { innerScrollEnabled ->
            FriendsScreen(
                friends = state.friends,
                profile = state.profile,
                scrollState = scrollState,
                scrollEnabled = innerScrollEnabled,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("unused", "FunctionName")
fun PassportTabController(profileAnchor: ProfileMenuAnchor): UIViewController = glassController {
    profileAnchor.density = LocalDensity.current.density
    val passportViewModel = viewModel { PassportViewModel(AppGraph.flightRepository) }
    val state by passportViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    CompositionLocalProvider(LocalNativeProfileMenuHost provides profileAnchor) {
        FlightyShell(
            backdropFlight = null,
            detail = false,
            contentAtTop = { scrollState.value == 0 },
        ) { innerScrollEnabled ->
            PassportScreen(
                stats = state.stats,
                runningOn = state.runningOn + " · Liquid Glass",
                profile = state.profile,
                scrollState = scrollState,
                scrollEnabled = innerScrollEnabled,
            )
        }
    }
}

@Suppress("unused", "FunctionName")
fun AddFlightController(onDismiss: () -> Unit): UIViewController = glassController {
    val addFlightViewModel = viewModel { AddFlightViewModel(AppGraph.flightRepository) }
    val state by addFlightViewModel.uiState.collectAsState()
    Box(modifier = Modifier.fillMaxSize().background(FlightyColors.SheetBg)) {
        AddFlightContent(
            shortcuts = state.shortcuts,
            suggestions = state.suggestions,
            onDismiss = onDismiss,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        )
    }
}

/** Full-Compose fallback for iOS versions without Liquid Glass (< 26). */
@Suppress("unused", "FunctionName")
fun FullComposeController(): UIViewController = ComposeUIViewController { App() }
