package flighty

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import flighty.data.AppGraph
import flighty.ui.DetailActionBar
import flighty.ui.FlightDetailScreen
import flighty.ui.FlightsScreen
import flighty.ui.FlightyColors
import flighty.ui.FlightyShell
import flighty.vm.AppViewModel
import flighty.vm.FlightDetailViewModel
import flighty.vm.FlightsViewModel

/**
 * The My Flights experience exactly as the reference demo behaves: one
 * persistent map/sheet shell, with Home → Detail navigation happening inside
 * the sheet while the backdrop map updates behind it. Used by hosts that own
 * their own tab chrome (the native-chrome iOS app); the full-Compose [App]
 * inlines the same structure with its tab switcher.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightsFlow(
    modifier: Modifier = Modifier,
    /**
     * Lets a native host react to in-sheet navigation with the open flight's
     * id (null when back on the list) — e.g. hide its tab bar and show its
     * own detail action bar. When provided, the Compose action bar is not
     * drawn; the host owns that chrome.
     */
    onDetailShown: ((String?) -> Unit)? = null,
) {
    val appViewModel = viewModel { AppViewModel(AppGraph.flightRepository) }
    val backStack = remember { mutableStateListOf<AppScreen>(AppScreen.Home) }

    val detailFlight = (backStack.lastOrNull() as? AppScreen.FlightDetail)
        ?.let { appViewModel.flightById(it.flightId) }

    if (onDetailShown != null) {
        LaunchedEffect(detailFlight?.id) {
            onDetailShown(detailFlight?.id)
        }
    }

    val flightsScrollState = rememberScrollState()
    val detailScrollState = rememberScrollState()

    FlightyShell(
        backdropFlight = detailFlight ?: appViewModel.liveFlight,
        detail = detailFlight != null,
        modifier = modifier,
        bottomOverlay = {
            if (detailFlight != null && onDetailShown == null) {
                DetailActionBar(
                    flight = detailFlight,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        },
    ) { innerScrollEnabled ->
        NavDisplay(
            backStack = backStack,
            // Opaque backing + alpha-free slides: see App.kt — fading layers
            // composite against the black Metal backing on iOS.
            modifier = Modifier.background(FlightyColors.SheetBg),
            transitionSpec = {
                slideInHorizontally(tween(400)) { it } togetherWith
                    slideOutHorizontally(tween(400)) { -it / 3 }
            },
            popTransitionSpec = {
                slideInHorizontally(tween(400)) { -it / 3 } togetherWith
                    slideOutHorizontally(tween(400)) { it }
            },
            predictivePopTransitionSpec = {
                slideInHorizontally(tween(400)) { -it / 3 } togetherWith
                    slideOutHorizontally(tween(400)) { it }
            },
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<AppScreen.Home> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(FlightyColors.SheetBg),
                    ) {
                        val flightsViewModel =
                            viewModel { FlightsViewModel(AppGraph.flightRepository) }
                        val state by flightsViewModel.uiState.collectAsState()
                        FlightsScreen(
                            state = state,
                            scrollState = flightsScrollState,
                            scrollEnabled = innerScrollEnabled,
                            onFlightClick = {
                                backStack.add(AppScreen.FlightDetail(it.id))
                            },
                        )
                    }
                }
                entry<AppScreen.FlightDetail> { key ->
                    val detailViewModel = viewModel(key = "detail-${key.flightId}") {
                        FlightDetailViewModel(AppGraph.flightRepository, key.flightId)
                    }
                    val state by detailViewModel.uiState.collectAsState()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(FlightyColors.SheetBg),
                    ) {
                        state.flight?.let { flight ->
                            FlightDetailScreen(
                                flight = flight,
                                scrollState = detailScrollState,
                                scrollEnabled = innerScrollEnabled,
                                onBack = { backStack.removeLastOrNull() },
                            )
                        }
                    }
                }
            },
        )
    }
}
