package flighty.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flighty.model.Profile
import flighty.ui.FlightyColors

/**
 * The screen-title row Flighty puts on every tab: bold title on the left,
 * share action and the profile avatar (with its account menu) on the right.
 */
@Composable
fun ScreenHeader(
    title: String,
    profile: Profile,
    modifier: Modifier = Modifier,
    showMore: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = FlightyColors.TextDark,
            modifier = Modifier.weight(1f),
        )
        if (showMore) {
            Icon(
                imageVector = AppIcons.More,
                contentDescription = "More",
                tint = FlightyColors.TextDark,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(14.dp))
        }
        Icon(
            imageVector = AppIcons.Share,
            contentDescription = "Share",
            tint = FlightyColors.TextDark,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(12.dp))
        ProfileAvatar(profile)
    }
}

@Composable
private fun ProfileAvatar(profile: Profile) {
    val nativeHost = LocalNativeProfileMenuHost.current
    if (nativeHost != null) {
        // A native host owns the account menu: report where the avatar sits
        // so its invisible system-menu button can cover it.
        DisposableEffect(Unit) {
            onDispose { nativeHost.clearAvatar() }
        }
        Avatar(
            profile = profile,
            size = 34,
            modifier = Modifier.onGloballyPositioned { coords ->
                val bounds = coords.boundsInWindow()
                nativeHost.updateAvatarBounds(
                    bounds.left, bounds.top, bounds.right, bounds.bottom,
                )
            },
        )
        return
    }
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        Avatar(profile, size = 34, modifier = Modifier.clickable { menuOpen = true })
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            containerColor = FlightyColors.CardBg,
        ) {
            DropdownMenuItem(
                text = {
                    Column {
                        Text(
                            text = profile.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FlightyColors.TextDark,
                        )
                        Text(
                            text = "Edit Profile",
                            fontSize = 11.sp,
                            color = FlightyColors.TextGray,
                        )
                    }
                },
                leadingIcon = { Avatar(profile, size = 26) },
                onClick = { menuOpen = false },
            )
            HorizontalDivider(color = FlightyColors.Divider)
            DropdownMenuItem(
                text = { MenuLabel("Manage Friends") },
                leadingIcon = { MenuIcon(AppIcons.People) },
                onClick = { menuOpen = false },
            )
            DropdownMenuItem(
                text = { MenuLabel("Settings") },
                leadingIcon = { MenuIcon(AppIcons.Settings) },
                onClick = { menuOpen = false },
            )
        }
    }
}

@Composable
private fun MenuLabel(label: String) {
    Text(
        text = label,
        fontSize = 14.sp,
        color = FlightyColors.TextDark,
    )
}

@Composable
private fun MenuIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = FlightyColors.TextDark,
        modifier = Modifier.size(18.dp),
    )
}

@Composable
private fun Avatar(profile: Profile, size: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(
                Brush.linearGradient(listOf(FlightyColors.Blue, Color(0xFF7C4DE0))),
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = profile.initials,
            color = Color.White,
            fontSize = (size * 0.36f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
