package br.com.rastreadorfrota.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.rastreadorfrota.auth.Perfil
import br.com.rastreadorfrota.ui.screens.CadastroUsuarioScreen
import br.com.rastreadorfrota.ui.screens.CadastroVeiculoScreen
import br.com.rastreadorfrota.ui.screens.HomeControladorScreen
import br.com.rastreadorfrota.ui.screens.HomeMotoristaScreen
import br.com.rastreadorfrota.ui.screens.LoginScreen
import br.com.rastreadorfrota.ui.screens.MapaFrotaScreen
import br.com.rastreadorfrota.ui.screens.MotoristasScreen
import br.com.rastreadorfrota.ui.screens.PerfilScreen

object Routes {
    const val LOGIN = "login"
    const val CADASTRO_USUARIO = "cadastro_usuario"
    const val HOME_CONTROLADOR = "home_controlador"
    const val HOME_MOTORISTA = "home_motorista"
    const val PERFIL = "perfil"
    const val CADASTRO_VEICULO = "cadastro_veiculo"
    const val MOTORISTAS = "motoristas"
    const val MAPA_FROTA = "mapa_frota"
    const val MAPA_MOTORISTA = "mapa_motorista?veiculo={veiculo}"

    fun mapaMotorista(veiculoRemoteId: String?) =
        "mapa_motorista" + (veiculoRemoteId?.let { "?veiculo=$it" } ?: "")

    fun homeDo(perfil: Perfil) = when (perfil) {
        Perfil.CONTROLADOR -> HOME_CONTROLADOR
        Perfil.MOTORISTA -> HOME_MOTORISTA
    }
}

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    // Entrar (login ou cadastro) e sair limpam a pilha inteira: o botão
    // voltar nunca leva de uma área logada pra tela de login, nem o contrário.
    fun irParaHome(perfil: Perfil) {
        navController.navigate(Routes.homeDo(perfil)) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = ::irParaHome,
                onCriarConta = { navController.navigate(Routes.CADASTRO_USUARIO) }
            )
        }
        composable(Routes.CADASTRO_USUARIO) {
            CadastroUsuarioScreen(
                onBack = { navController.popBackStack() },
                onCadastroConcluido = ::irParaHome
            )
        }
        composable(Routes.HOME_CONTROLADOR) {
            HomeControladorScreen(
                onAbrirPerfil = { navController.navigate(Routes.PERFIL) },
                onAbrirMapa = { navController.navigate(Routes.MAPA_FROTA) },
                onGerenciarMotoristas = { navController.navigate(Routes.MOTORISTAS) },
                onGerenciarVeiculos = { navController.navigate(Routes.CADASTRO_VEICULO) }
            )
        }
        composable(Routes.HOME_MOTORISTA) {
            HomeMotoristaScreen(
                onAbrirPerfil = { navController.navigate(Routes.PERFIL) },
                onCadastrarVeiculo = { navController.navigate(Routes.CADASTRO_VEICULO) },
                onAbrirMapa = { veiculoRemoteId -> navController.navigate(Routes.mapaMotorista(veiculoRemoteId)) }
            )
        }
        composable(Routes.PERFIL) {
            PerfilScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.CADASTRO_VEICULO) {
            CadastroVeiculoScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.MOTORISTAS) {
            MotoristasScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.MAPA_FROTA) {
            MapaFrotaScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.MAPA_MOTORISTA,
            arguments = listOf(navArgument("veiculo") { type = NavType.StringType; nullable = true })
        ) { entry ->
            MapaFrotaScreen(
                onBack = { navController.popBackStack() },
                modoMotorista = true,
                veiculoRemoteId = entry.arguments?.getString("veiculo")
            )
        }
    }
}
