package flighty

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
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
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import flighty.data.MockFlights
import flighty.model.Flight
import flighty.ui.FlightDetailScreen
import flighty.ui.FlightsScreen
import flighty.ui.FlightyColors
import flighty.ui.FlightyTheme
import flighty.ui.FriendsScreen
import flighty.ui.PassportScreen
import flighty.ui.components.AppIcons
import flighty.ui.components.MapBackdrop
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
        var tab by remember { mutableStateOf(Tab.Flights) }
        var selectedFlight by remember { mutableStateOf<Flight?>(null) }

        // System back (Android button/gesture) pops the detail sheet instead of
        // leaving the app — mirrors the X button on iOS.
        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            isBackEnabled = selectedFlight != null,
            onBackCompleted = { selectedFlight = null },
        )

        var showAddFlight by remember { mutableStateOf(false) }

        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(FlightyColors.Space)) {
            val screenHeight = maxHeight
            val peekHeight = if (selectedFlight != null) screenHeight * 0.55f else screenHeight * 0.66f

            // Globe ↔ real map handoff crossfades like the reference video.
            Crossfade(targetState = selectedFlight, animationSpec = tween(700)) { detailFlight ->
                MapBackdrop(
                    flight = detailFlight ?: if (tab == Tab.Flights) MockFlights.live else null,
                    detail = detailFlight != null,
                    mapHeightFraction = if (detailFlight != null) 0.45f else 0.34f,
                )
            }

            val sheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
            val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
            val scope = rememberCoroutineScope()

            // Scroll positions are hoisted so the wheel handler below can tell
            // whether the visible content sits at its top. Also keeps each
            // tab's scroll position across tab switches.
            val flightsScrollState = rememberScrollState()
            val friendsScrollState = rememberScrollState()
            val passportScrollState = rememberScrollState()
            val detailScrollState = rememberScrollState()

            fun contentAtTop(): Boolean = if (selectedFlight != null) {
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
                        // Gate on the SETTLED value only — targetValue flips the
                        // moment a collapse fling starts, and recomposing the
                        // scrollable out of the nested-scroll chain mid-gesture
                        // is what made collapse flings jump.
                        val innerScrollEnabled =
                            sheetState.currentValue == SheetValue.Expanded
                        AnimatedContent(
                            targetState = selectedFlight,
                            transitionSpec = {
                                (slideInVertically { it / 6 } + fadeIn()) togetherWith
                                    (slideOutVertically { it / 6 } + fadeOut())
                            },
                        ) { flight ->
                            if (flight != null) {
                                FlightDetailScreen(
                                    flight = flight,
                                    scrollState = detailScrollState,
                                    scrollEnabled = innerScrollEnabled,
                                    onBack = { selectedFlight = null },
                                )
                            } else {
                                when (tab) {
                                    Tab.Flights -> FlightsScreen(
                                        scrollState = flightsScrollState,
                                        scrollEnabled = innerScrollEnabled,
                                        onFlightClick = { selectedFlight = it },
                                    )
                                    Tab.Friends -> FriendsScreen(
                                        scrollState = friendsScrollState,
                                        scrollEnabled = innerScrollEnabled,
                                    )
                                    Tab.Passport -> PassportScreen(
                                        scrollState = passportScrollState,
                                        scrollEnabled = innerScrollEnabled,
                                    )
                                }
                            }
                        }
                    }
                },
            ) { /* Map area — the backdrop behind the scaffold shows through. */ }

            if (selectedFlight == null) {
                FlightyTabBar(
                    selected = tab,
                    onSelect = { tab = it },
                    onSearch = { showAddFlight = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }

            if (showAddFlight) {
                AddFlightSheet(onDismiss = { showAddFlight = false })
            }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFlightSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = FlightyColors.SheetBg,
        // From google issue 467297218 (comment #4): the TOP window inset makes
        // near-full-height sheets oscillate on fast flings — keep only the
        // bottom inset so content still clears the home indicator.
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
            Text(
                text = "Add Flight",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = FlightyColors.TextDark,
            )
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
            listOf(
                Triple(flighty.data.Airlines.AirCanada, "Air Canada", "AC · ACA"),
                Triple(flighty.data.Airlines.Flair, "Flair", "F8 · FLE"),
            ).forEach { (airline, name, codes) ->
                AddFlightRow(name = name, codes = codes) {
                    flighty.ui.components.AirlineBadge(airline, size = 30)
                }
            }
            listOf(
                Triple(flighty.data.Airports.YYZ, "Lester B. Pearson Intl.", "YYZ · CYYZ · Toronto"),
                Triple(flighty.data.Airports.YVR, "Vancouver Intl.", "YVR · CYVR · Vancouver"),
            ).forEach { (airport, name, codes) ->
                AddFlightRow(name = name, codes = codes) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(FlightyColors.ChipBg, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = airport.code,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = FlightyColors.TextDark,
                        )
                    }
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
private fun AddFlightRow(
    name: String,
    codes: String,
    badge: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        badge()
        Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = FlightyColors.TextDark,
            )
            Text(text = codes, fontSize = 11.sp, color = FlightyColors.TextGray)
        }
        Text(text = "›", fontSize = 16.sp, color = FlightyColors.TextGray)
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
