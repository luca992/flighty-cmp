package flighty.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The sheet/chrome layout is phone-designed; hosts wider than this (web,
 * tablets, wide desktop windows) center it as a column while the map/globe
 * backdrop bleeds across the full window (see FlightyShell).
 */
val PhoneMaxWidth: Dp = 480.dp
