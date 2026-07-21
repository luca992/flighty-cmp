package flighty.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flighty.model.Friend
import flighty.ui.components.StatusText

@Composable
fun FriendsScreen(
    friends: List<Friend>,
    scrollState: ScrollState,
    scrollEnabled: Boolean,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Friends",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = FlightyColors.TextDark,
            modifier = Modifier.padding(start = 20.dp, top = 18.dp),
        )
        Column(
            modifier = Modifier
                .verticalScroll(scrollState, enabled = scrollEnabled)
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            friends.forEach { friend ->
                FriendCard(friend)
            }
        }
    }
}

@Composable
private fun FriendCard(friend: Friend) {
    Surface(color = FlightyColors.CardBg, shape = RoundedCornerShape(18.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(friend.color), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = friend.initials,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = friend.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FlightyColors.TextDark,
                )
                Text(
                    text = "${friend.route} · ${friend.flightLabel}",
                    fontSize = 12.sp,
                    color = FlightyColors.TextGray,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusText(friend.status)
                Text(
                    text = friend.statusNote,
                    fontSize = 11.sp,
                    color = FlightyColors.TextGray,
                )
            }
        }
    }
}
