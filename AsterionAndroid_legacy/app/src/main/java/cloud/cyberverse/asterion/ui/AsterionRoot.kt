package cloud.cyberverse.asterion.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cloud.cyberverse.asterion.ui.components.AsterionLoadingBox
import com.clerk.api.Clerk
import com.clerk.ui.auth.AuthView

// Clerk.isInitialized/userFlow are already shared StateFlows, so no wrapping ViewModel is needed here.
@Composable
fun AsterionRoot() {
    val isInitialized by Clerk.isInitialized.collectAsState()
    val user by Clerk.userFlow.collectAsState()

    when {
        !isInitialized -> AsterionLoadingBox()
        user == null -> AuthView()
        else -> AsterionNavHost()
    }
}
