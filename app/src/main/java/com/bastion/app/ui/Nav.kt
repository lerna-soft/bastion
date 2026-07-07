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
    const val VAULT = "vault"
    const val HOST_EDIT = "host_edit/{hostId}"
    const val HOST_ADD = "host_add"
    const val TERMINAL = "terminal/{hostId}"

    fun hostEdit(hostId: Long) = "host_edit/$hostId"
    fun terminal(hostId: Long) = "terminal/$hostId"
}

@Composable
fun BastionNavGraph(
    navController: NavHostController,
    repository: VaultRepository,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.VAULT,
        modifier = modifier
    ) {
        composable(Routes.VAULT) {
            VaultScreen(
                repository = repository,
                onAddHost = { navController.navigate(Routes.HOST_ADD) },
                onEditHost = { id -> navController.navigate(Routes.hostEdit(id)) },
                onConnect = { id -> navController.navigate(Routes.terminal(id)) }
            )
        }

        composable(Routes.HOST_ADD) {
            HostEditScreen(
                hostId = null,
                repository = repository,
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

        composable(
            route = Routes.TERMINAL,
            arguments = listOf(navArgument("hostId") { type = NavType.LongType })
        ) {
            // Extract hostId from nav backstack — use saved state handle or re-fetch
            val hostId = it.arguments?.getLong("hostId") ?: return@composable
            TerminalScreen(
                repository = repository,
                initialHostId = hostId
            )
        }
    }
}
