package flighty

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import flighty.data.AppGraph
import flighty.model.AddFlightSuggestion
import flighty.ui.FlightDetailScreen
import flighty.ui.FlightsScreen
import flighty.ui.FlightyColors
import flighty.ui.FlightyTheme
import flighty.ui.FriendsScreen
import flighty.ui.PassportScreen
import flighty.ui.components.AppIcons
import flighty.ui.components.MapBackdrop
import flighty.vm.AddFlightViewModel
import flighty.vm.AppViewModel
import flighty.vm.FlightDetailViewModel
import flighty.vm.FlightsViewModel
import flighty.vm.FriendsViewModel
import flighty.vm.PassportViewModel
import kotlinx.coroutines.launch

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

        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(FlightyColors.Space)) {
            val screenHeight = maxHeight
            val peekHeight = if (detailFlight != null) screenHeight * 0.55f else screenHeight * 0.66f

            // Globe ↔ real map handoff switches instantly: the MapLibre map is
            // a native interop view, and Compose cannot alpha-composite interop
            // layers — crossfading it painted the whole backdrop black on iOS
            // for the duration of the fade. The map's own camera fly-in
            // animation carries the transition instead.
            MapBackdrop(
                flight = detailFlight
                    ?: if (tab == Tab.Flights) appViewModel.liveFlight else null,
                detail = detailFlight != null,
                mapHeightFraction = if (detailFlight != null) 0.45f else 0.34f,
            )

            // Flighty's map controls sit over the backdrop but under the sheet:
            // they must disappear behind it as the sheet expands.
            MapControlsOverlay(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 6.dp, end = 10.dp),
            )

            val sheetState = rememberBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded,
                // No Hidden: the sheet is structural chrome and must never be
                // dismissable (the new API's default enabledValues includes it).
                enabledValues = setOf(SheetValue.PartiallyExpanded, SheetValue.Expanded),
            )
            val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
            val scope = rememberCoroutineScope()

            // Scroll positions are hoisted so the wheel handler below can tell
            // whether the visible content sits at its top. Also keeps each
            // tab's scroll position across tab switches.
            val flightsScrollState = rememberScrollState()
            val friendsScrollState = rememberScrollState()
            val passportScrollState = rememberScrollState()
            val detailScrollState = rememberScrollState()

            fun contentAtTop(): Boolean = if (detailFlight != null) {
                detailScrollState.value == 0
            } else when (tab) {
                Tab.Flights -> flightsScrollState.value == 0
                Tab.Friends -> friendsScrollState.value == 0
                Tab.Passport -> passportScrollState.value == 0
            }

            // Material3's sheet only follows touch drags, and desktop wheel
            // events skip nested scroll entirely when the list can't consume
            // them (e.g. at the top). Raw scroll pointer events always arrive,
            // so drive the sheet from those: scroll up into the content
            // expands it, scroll down at the top collapses it. Desktop-only —
            // touch platforms must not pay for this in the drag hot path.
            val wheelSheetModifier = if (platformName() != "Desktop JVM") Modifier
            else Modifier.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type != PointerEventType.Scroll) continue
                        val dy = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                        if (dy < -0.2f &&
                            contentAtTop() &&
                            sheetState.currentValue == SheetValue.Expanded &&
                            sheetState.targetValue == SheetValue.Expanded
                        ) {
                            scope.launch { sheetState.partialExpand() }
                        } else if (dy > 0.2f &&
                            sheetState.currentValue == SheetValue.PartiallyExpanded &&
                            sheetState.targetValue == SheetValue.PartiallyExpanded
                        ) {
                            scope.launch { sheetState.expand() }
                        }
                    }
                }
            }

            // Drag the sheet or scroll its content to grow it over the map,
            // Flighty-style; drag down to reveal more of the globe.
            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                sheetPeekHeight = peekHeight,
                sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                sheetContainerColor = FlightyColors.SheetBg,
                sheetShadowElevation = 0.dp,
                containerColor = Color.Transparent,
                sheetDragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp, bottom = 2.dp)
                            .size(width = 36.dp, height = 4.dp)
                            .background(FlightyColors.Divider, RoundedCornerShape(50)),
                    )
                },
                sheetContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(screenHeight - 72.dp)
                            .then(wheelSheetModifier),
                    ) {
                        // Inner scrolling engages only once the sheet is fully
                        // expanded: at peek, gestures are pure sheet drags.
                        // Gate on the SETTLED value only — flipping mid-gesture
                        // recomposes the scrollable out of the nested-scroll
                        // chain and makes collapse flings jump.
                        val innerScrollEnabled =
                            sheetState.currentValue == SheetValue.Expanded

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
                },
            ) { /* Map area — the backdrop behind the scaffold shows through. */ }

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

            if (showAddFlight) {
                val addFlightViewModel = viewModel { AddFlightViewModel(AppGraph.flightRepository) }
                val state by addFlightViewModel.uiState.collectAsState()
                AddFlightSheet(
                    suggestions = state.suggestions,
                    onDismiss = { showAddFlight = false },
                )
            }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFlightSheet(
    suggestions: List<AddFlightSuggestion>,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Flighty's search opens straight to a full-height sheet — no partial
        // stop, so the enabled values skip PartiallyExpanded entirely.
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        containerColor = FlightyColors.SheetBg,
        // From google issue 467297218 (comment #4): the TOP window inset makes
        // near-full-height sheets oscillate on fast flings — keep only the
        // bottom inset so content still clears the home indicator.
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Add Flight",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlightyColors.TextDark,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(FlightyColors.ChipBg, CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = "Close",
                        tint = FlightyColors.TextDark,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                text = "Enter airline, airport, or flight",
                fontSize = 13.sp,
                color = FlightyColors.TextGray,
                modifier = Modifier.padding(top = 2.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .background(FlightyColors.ChipBg, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "Flair, YYZ, or F8123",
                    fontSize = 14.sp,
                    color = FlightyColors.TextGray,
                )
            }
            Text(
                text = "FREQUENTLY USED",
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.SemiBold,
                color = FlightyColors.TextGray,
                modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
            )
            suggestions.forEach { suggestion ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(
                                suggestion.badgeColor?.let { Color(it) } ?: FlightyColors.ChipBg,
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = suggestion.badgeCode,
                            fontSize = if (suggestion.badgeCode.length > 2) 8.sp else 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (suggestion.badgeColor != null) Color.White else FlightyColors.TextDark,
                        )
                    }
                    Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                        Text(
                            text = suggestion.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FlightyColors.TextDark,
                        )
                        Text(text = suggestion.codes, fontSize = 11.sp, color = FlightyColors.TextGray)
                    }
                    Text(text = "›", fontSize = 16.sp, color = FlightyColors.TextGray)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
            ) {
                Icon(
                    imageVector = AppIcons.Plane,
                    contentDescription = null,
                    tint = FlightyColors.Blue,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Find by Route",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FlightyColors.Blue,
                    modifier = Modifier.padding(start = 10.dp),
                )
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
private fun MapControlsOverlay(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        MapControlButton(AppIcons.Map, "Map style")
        MapControlButton(AppIcons.Cloud, "Weather")
        MapControlButton(AppIcons.Locate, "Live tracking")
    }
}

@Composable
private fun MapControlButton(icon: ImageVector, description: String) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(Color(0x66141922), RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(15.dp),
        )
    }
}

/**
 * The share / alerts / more cluster plus the Add Return pill that floats over
 * the bottom of the flight-detail sheet, standing in for the tab bar there.
 */
@Composable
private fun DetailActionBar(modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        Surface(
            color = FlightyColors.CardBg,
            shape = RoundedCornerShape(50),
            shadowElevation = 6.dp,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            ) {
                DetailActionIcon(AppIcons.Share, "Share")
                DetailActionIcon(AppIcons.BellOff, "Alerts")
                DetailActionIcon(AppIcons.More, "More")
            }
        }
        Spacer(Modifier.weight(1f))
        Surface(
            color = FlightyColors.Blue,
            shape = RoundedCornerShape(50),
            shadowElevation = 6.dp,
        ) {
            Text(
                text = "Add Return",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 13.dp),
            )
        }
    }
}

@Composable
private fun DetailActionIcon(icon: ImageVector, description: String) {
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = FlightyColors.TextDark,
        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp).size(19.dp),
    )
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
