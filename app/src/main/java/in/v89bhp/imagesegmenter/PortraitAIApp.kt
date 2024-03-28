package `in`.v89bhp.imagesegmenter

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import `in`.v89bhp.imagesegmenter.ui.backgroundremover.ImageSegmenter
import `in`.v89bhp.imagesegmenter.ui.home.Home
import `in`.v89bhp.imagesegmenter.ui.home.NavigationDestination
import `in`.v89bhp.imagesegmenter.ui.upscaler.Sisr

@Composable
fun PortraitAIApp(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    appState: PortraitAIAppState = rememberPortraitAIAppState()
) {
    NavHost(
        navController = appState.navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Home.route) { backStackEntry ->
            Home(
                onNavigateTo = { route ->
                    appState.navigateTo(route, backStackEntry)
                }
            )
        }

        composable(NavigationDestination.BACKGROUND_REMOVER.route) { backStackEntry ->
            ImageSegmenter(
                navigateBack = { appState.navigateBack() }
            )
        }

        composable(NavigationDestination.UPSCALER.route) { backStackEntry ->
            Sisr(
                navigateBack = { appState.navigateBack() }
            )
        }




        // TODO Add new navigation destinations here


    }
}