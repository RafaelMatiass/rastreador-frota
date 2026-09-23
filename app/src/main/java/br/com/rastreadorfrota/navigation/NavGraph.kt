package br.com.rastreadorfrota.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.rastreadorfrota.ui.screens.CadastroMotoristaScreen
import br.com.rastreadorfrota.ui.screens.CadastroVeiculoScreen
import br.com.rastreadorfrota.ui.screens.HomeScreen
import br.com.rastreadorfrota.ui.screens.LoginScreen
import br.com.rastreadorfrota.ui.screens.MapaFrotaScreen

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val CADASTRO_VEICULO = "cadastro_veiculo"
    const val CADASTRO_MOTORISTA = "cadastro_motorista"
    const val MAPA_FROTA = "mapa_frota"
}

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onCadastrarVeiculo = { navController.navigate(Routes.CADASTRO_VEICULO) },
                onCadastrarMotorista = { navController.navigate(Routes.CADASTRO_MOTORISTA) },
                onAbrirMapa = { navController.navigate(Routes.MAPA_FROTA) }
            )
        }
        composable(Routes.CADASTRO_VEICULO) {
            CadastroVeiculoScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CADASTRO_MOTORISTA) {
            CadastroMotoristaScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.MAPA_FROTA) {
            MapaFrotaScreen(onBack = { navController.popBackStack() })
        }
    }
}