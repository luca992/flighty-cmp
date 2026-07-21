import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import flighty.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Flighty KMP",
        state = WindowState(
            position = WindowPosition.Aligned(Alignment.Center),
            width = 430.dp,
            height = 900.dp,
        ),
    ) {
        App()
    }
}
