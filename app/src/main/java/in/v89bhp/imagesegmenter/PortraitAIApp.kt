package `in`.v89bhp.imagesegmenter

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import `in`.v89bhp.imagesegmenter.ui.home.Home

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

        composable(NavigationDestination.GAUGES.route) { backStackEntry ->
            Gauges(
                onNavigateTo = { route -> appState.navigateTo(route, backStackEntry) },
                navigateBack = { appState.navigateBack() }
            )
        }

        composable(NavigationDestination.SCAN.route) { backStackEntry ->
            ScanContainer(
                backStackEntry = backStackEntry,
                onNavigateTo = { route -> appState.navigateTo(route, backStackEntry) },
                navigateBack = { appState.navigateBack() }
            )
        }

        composable(NavigationDestination.CONNECTIVITY.route) { backStackEntry ->
            Connectivity(
                backStackEntry = backStackEntry,
                navigateBack = {
                    appState.navigateBack()
                })
        }

        composable(NavigationDestination.IM_READINESS.route) { backStackEntry ->
            IMReadinessContainer(
                backStackEntry = backStackEntry,
                navigateBack = {
                    appState.navigateBack()
                })
        }

        composable(NavigationDestination.SETTINGS.route) { backStackEntry ->
            Settings(onNavigateTo = { route ->
                appState.navigateTo(
                    route,
                    backStackEntry
                )
            },
                navigateBack = { appState.navigateBack() })
        }

        composable(NavigationDestination.ABOUT.route) { backStackEntry ->
            About(navigateBack = {
                appState.navigateBack()
            })
        }

        composable(Screen.GaugeTypePicker.route) { backStackEntry ->
            GaugeTypePicker(navigateBack = {
                appState.navigateBack()
            })
        }
        composable(Screen.GaugePicker.route) { backStackEntry ->
            GaugePicker(navigateBack = {
                appState.navigateBack()
            })
        }

        composable(Screen.FreezeFrame.route) { backStackEntry ->
            FreezeFrame(
                obdCode = backStackEntry.arguments!!.getString("obdCode")!!,
                navigateBack = {
                    appState.navigateBack()
                })
        }

        // TODO Add new navigation destinations here


    }
}