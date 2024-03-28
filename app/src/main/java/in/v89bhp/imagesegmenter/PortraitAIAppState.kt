package `in`.v89bhp.imagesegmenter

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController


/**
 * List of screens for [PortraitAI App]
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")

    object BackgroundRemover : Screen("backgroundremover")

    object Upscaler : Screen("upscaler")
}


@Composable
fun rememberPortraitAIAppState(
    navController: NavHostController = rememberNavController(),
    context: Context = LocalContext.current
) = remember(navController, context) {
    PortraitAIAppState(navController, context)
}

class PortraitAIAppState(
    val navController: NavHostController,
    private val context: Context
) {

    fun navigateTo(route: String, from: NavBackStackEntry) {
        // In order to discard duplicated navigation events, we check the Lifecycle
        if (from.lifecycleIsResumed()) {
            navController.navigate(route)
        }
    }

    fun navigateBack() {
        navController.popBackStack()
    }


}

/**
 * If the lifecycle is not resumed it means this NavBackStackEntry already processed a nav event.
 *
 * This is used to de-duplicate navigation events.
 */
private fun NavBackStackEntry.lifecycleIsResumed() =
    this.lifecycle.currentState == Lifecycle.State.RESUMED