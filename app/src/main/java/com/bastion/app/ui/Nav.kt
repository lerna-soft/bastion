package com.bastion.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bastion.app.data.VaultRepository

object Routes {
    const val MAIN = "main"
    const val HOST_EDIT = "host_edit/{hostId}"
    const val HOST_ADD = "host_add"
    const val ABOUT = "about"

    fun hostEdit(hostId: Long) = "host_edit/$hostId"
}

@Composable
fun BastionNavGraph(
    navController: NavHostController,
    repository: VaultRepository,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.MAIN,
        modifier = modifier
    ) {
        composable(Routes.MAIN) {
            AppLayout(
                repository = repository,
                onNavigateToAddHost = { navController.navigate(Routes.HOST_ADD) },
                onNavigateToEditHost = { id -> navController.navigate(Routes.hostEdit(id)) },
                onNavigateToAbout = { navController.navigate(Routes.ABOUT) }
            )
        }

        composable(Routes.HOST_ADD) {
            HostEditScreen(
                hostId = null,
                repository = repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ABOUT) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.HOST_EDIT,
            arguments = listOf(navArgument("hostId") { type = NavType.LongType })
        ) {
            val hostId = it.arguments?.getLong("hostId") ?: return@composable
            HostEditScreen(
                hostId = hostId,
                repository = repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
