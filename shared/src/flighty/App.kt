package flighty

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import flighty.data.AppGraph
import flighty.ui.AddFlightSheetHost
import flighty.ui.DetailActionBar
import flighty.ui.FlightDetailScreen
import flighty.ui.FlightsScreen
import flighty.ui.FlightyColors
import flighty.ui.FlightyShell
import flighty.ui.FlightyTheme
import flighty.ui.FriendsScreen
import flighty.ui.PassportScreen
import flighty.ui.components.AppIcons
import flighty.vm.AddFlightViewModel
import flighty.vm.AppViewModel
import flighty.vm.FlightDetailViewModel
import flighty.vm.FlightsViewModel
import flighty.vm.FriendsViewModel
import flighty.vm.PassportViewModel

private enum class Tab(val title: String) {
    Flights("My Flights"),
    Friends("Friends"),
    Passport("Passport"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    FlightyTheme {
    // Hard edges everywhere: no rubber-band on list ends or on the sheet's own
    // min/max bounds (the band's spring-back re-rasterizes the whole sheet per
    // frame on iOS and reads as jerky).
    CompositionLocalProvider(LocalOverscrollFactory provides null) {
        val appViewModel = viewModel { AppViewModel(AppGraph.flightRepository) }
        val backStack = remember { mutableStateListOf<AppScreen>(AppScreen.Home) }
        var tab by remember { mutableStateOf(Tab.Flights) }
        var showAddFlight by remember { mutableStateOf(false) }

        val detailFlight = (backStack.lastOrNull() as? AppScreen.FlightDetail)
            ?.let { appViewModel.flightById(it.flightId) }

        // Scroll positions are hoisted so the shell's desktop wheel handler can
        // tell whether the visible content sits at its top. Also keeps each
        // tab's scroll position across tab switches.
        val flightsScrollState = rememberScrollState()
        val friendsScrollState = rememberScrollState()
        val passportScrollState = rememberScrollState()
        val detailScrollState = rememberScrollState()

        FlightyShell(
            backdropFlight = detailFlight
                ?: if (tab == Tab.Flights) appViewModel.liveFlight else null,
            detail = detailFlight != null,
            bottomOverlay = {
                if (detailFlight == null) {
                    FlightyTabBar(
                        selected = tab,
                        onSelect = { tab = it },
                        onSearch = { showAddFlight = true },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                } else {
                    DetailActionBar(
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
                // Opaque backing: nav transitions render entries into
                // graphics layers, and any transparent region shows
                // the black Metal backing layer on iOS.
                modifier = Modifier.background(FlightyColors.SheetBg),
                // Alpha-free slides instead of the default 700ms
                // cross-fade: on iOS the fading layers composite
                // against the black Metal backing and the whole
                // sheet flashes dark during navigation.
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
                    // Each entry gets an opaque sheet-colored panel:
                    // the screens themselves have no background, so
                    // without it the slide layers are transparent
                    // and the outgoing screen shows through.
                    entry<AppScreen.Home> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(FlightyColors.SheetBg),
                        ) {
                            when (tab) {
                                Tab.Flights -> {
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
                                Tab.Friends -> {
                                    val friendsViewModel =
                                        viewModel { FriendsViewModel(AppGraph.flightRepository) }
                                    val state by friendsViewModel.uiState.collectAsState()
                                    FriendsScreen(
                                        friends = state.friends,
                                        profile = state.profile,
                                        scrollState = friendsScrollState,
                                        scrollEnabled = innerScrollEnabled,
                                    )
                                }
                                Tab.Passport -> {
                                    val passportViewModel =
                                        viewModel { PassportViewModel(AppGraph.flightRepository) }
                                    val state by passportViewModel.uiState.collectAsState()
                                    PassportScreen(
                                        stats = state.stats,
                                        runningOn = state.runningOn,
                                        profile = state.profile,
                                        scrollState = passportScrollState,
                                        scrollEnabled = innerScrollEnabled,
                                    )
                                }
                            }
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

        if (showAddFlight) {
            AddFlightSheetHost(onDismiss = { showAddFlight = false })
        }
    }
    }
}

@Composable
private fun FlightyTabBar(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = FlightyColors.CardBg,
        shape = RoundedCornerShape(50),
        shadowElevation = 6.dp,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        ) {
            Tab.entries.forEach { entry ->
                TabItem(
                    icon = when (entry) {
                        Tab.Flights -> AppIcons.Plane
                        Tab.Friends -> AppIcons.People
                        Tab.Passport -> AppIcons.Person
                    },
                    title = entry.title,
                    selected = selected == entry,
                    onClick = { onSelect(entry) },
                )
                Spacer(Modifier.width(10.dp))
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(FlightyColors.ChipBg, CircleShape)
                    .clickable(onClick = onSearch),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppIcons.Search,
                    contentDescription = "Search",
                    tint = FlightyColors.TextDark,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (selected) FlightyColors.Blue else FlightyColors.TextGray
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = tint,
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = title,
            fontSize = 10.sp,
            color = tint,
        )
    }
}
