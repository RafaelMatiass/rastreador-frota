package br.com.rastreadorfrota.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import br.com.rastreadorfrota.ui.components.Avisos
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.rastreadorfrota.PreferenciasApp
import br.com.rastreadorfrota.auth.EstadoSessao
import br.com.rastreadorfrota.auth.Perfil
import br.com.rastreadorfrota.auth.SessaoViewModel
import br.com.rastreadorfrota.data.local.AppDatabase
import br.com.rastreadorfrota.ui.screens.AlertasScreen
import br.com.rastreadorfrota.ui.screens.CadastroUsuarioScreen
import br.com.rastreadorfrota.ui.screens.CercaEditorScreen
import br.com.rastreadorfrota.ui.screens.EquipeScreen
import br.com.rastreadorfrota.ui.screens.FrotaScreen
import br.com.rastreadorfrota.ui.screens.InicioGestorScreen
import br.com.rastreadorfrota.ui.screens.InicioMotoristaScreen
import br.com.rastreadorfrota.ui.screens.LoginScreen
import br.com.rastreadorfrota.ui.screens.MapaFrotaScreen
import br.com.rastreadorfrota.ui.screens.MeuVeiculoScreen
import br.com.rastreadorfrota.ui.screens.OnboardingScreen
import br.com.rastreadorfrota.ui.screens.PerfilScreen
import br.com.rastreadorfrota.ui.screens.VeiculoDetalheScreen
import br.com.rastreadorfrota.ui.screens.VeiculoFormScreen
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme

private val ROTAS_DESLOGADO = setOf(Rotas.ONBOARDING, Rotas.LOGIN, Rotas.CADASTRO_USUARIO)

/**
 * Grafo único do app. A barra de abas aparece só nas rotas que são abas do perfil logado;
 * formulários, detalhes e editor de cerca abrem em tela cheia por cima.
 *
 * [placaNotificacao]: placa vinda de uma notificação de cerca (abre o mapa focado no veículo).
 */
@Composable
fun AppNavGraph(
    sessao: SessaoViewModel,
    placaNotificacao: String?,
    onPlacaConsumida: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val estado by sessao.estado.collectAsStateWithLifecycle()
    if (estado is EstadoSessao.Carregando) return // a splash do sistema continua na tela

    val context = LocalContext.current
    val prefs = remember { PreferenciasApp(context) }
    val usuario = (estado as? EstadoSessao.Logado)?.usuario
    val inicio = remember {
        when {
            !prefs.onboardingVisto -> Rotas.ONBOARDING
            usuario != null -> Rotas.inicioDo(usuario.perfil)
            else -> Rotas.LOGIN
        }
    }

    fun limparPilhaE(rota: String) = navController.navigate(rota) {
        popUpTo(navController.graph.id) { inclusive = true }
        launchSingleTop = true
    }

    // Saiu da conta (ou foi desativado pelo gestor): volta ao login, sem histórico.
    LaunchedEffect(estado) {
        val rotaAtual = navController.currentDestination?.route
        if (estado is EstadoSessao.Deslogado && rotaAtual != null && rotaAtual !in ROTAS_DESLOGADO) {
            limparPilhaE(Rotas.LOGIN)
        }
    }

    // Toque numa notificação de cerca: mapa focado no veículo.
    LaunchedEffect(placaNotificacao, usuario?.uid) {
        if (placaNotificacao != null && usuario != null) {
            PedidosNavegacao.pedirMapa(PedidoMapa(placa = placaNotificacao))
            navController.navigate(Rotas.MAPA) { launchSingleTop = true }
            onPlacaConsumida()
        }
    }

    val entrada by navController.currentBackStackEntryAsState()
    val rotaAtual = entrada?.destination?.route
    val abas = usuario?.let { abasDo(it.perfil) }.orEmpty()
    val mostrarBarra = abas.any { it.rota == rotaAtual }
    val naoLidos by remember { AppDatabase.getInstance(context).alertaDao().observarNaoLidos() }
        .collectAsStateWithLifecycle(initialValue = 0)

    fun irParaAba(rota: String) {
        val raiz = abas.firstOrNull()?.rota ?: return
        navController.navigate(rota) {
            popUpTo(raiz) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val abrirPerfil: () -> Unit = {
        if (usuario?.perfil == Perfil.MOTORISTA) irParaAba(Rotas.PERFIL)
        else navController.navigate(Rotas.PERFIL) { launchSingleTop = true }
    }

    // Avisos do app inteiro ("Veículo salvo", "Excluído · Desfazer") aparecem aqui.
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        Avisos.fluxo.collect { aviso ->
            snackbar.currentSnackbarData?.dismiss()
            val resultado = snackbar.showSnackbar(aviso.texto, aviso.acao, withDismissAction = aviso.acao == null, duration = SnackbarDuration.Short)
            if (resultado == SnackbarResult.ActionPerformed) aviso.onAcao?.invoke()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = mostrarBarra,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    abas.forEach { aba ->
                        val selecionada = aba.rota == rotaAtual
                        val badge = if (aba.rota == Rotas.ALERTAS) naoLidos else 0
                        NavigationBarItem(
                            selected = selecionada,
                            onClick = { irParaAba(aba.rota) },
                            icon = {
                                BadgedBox(badge = {
                                    if (badge > 0) Badge { Text(if (badge > 99) "99+" else badge.toString()) }
                                }) {
                                    Icon(if (selecionada) aba.iconeSelecionado else aba.icone, contentDescription = null)
                                }
                            },
                            label = { Text(aba.rotulo) },
                            modifier = Modifier.semantics {
                                contentDescription = aba.rotulo + if (badge > 0) ", $badge alertas não lidos" else ""
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = TrakSyncTheme.colors.textSecondary,
                                unselectedTextColor = TrakSyncTheme.colors.textSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = inicio,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(180)) }
        ) {
            composable(Rotas.ONBOARDING) {
                OnboardingScreen(onConcluir = {
                    prefs.onboardingVisto = true
                    limparPilhaE(sessao.usuario?.let { Rotas.inicioDo(it.perfil) } ?: Rotas.LOGIN)
                })
            }
            composable(Rotas.LOGIN) {
                LoginScreen(
                    aviso = (estado as? EstadoSessao.Deslogado)?.aviso,
                    onEntrou = { u ->
                        sessao.entrou(u)
                        limparPilhaE(Rotas.inicioDo(u.perfil))
                    },
                    onCriarConta = { navController.navigate(Rotas.CADASTRO_USUARIO) }
                )
            }
            composable(Rotas.CADASTRO_USUARIO, enterTransition = { slideEntrada() }, popExitTransition = { slideSaida() }) {
                CadastroUsuarioScreen(
                    onBack = { navController.popBackStack() },
                    onCadastroConcluido = { u ->
                        sessao.entrou(u)
                        limparPilhaE(Rotas.inicioDo(u.perfil))
                    }
                )
            }

            // ---------- Abas ----------
            composable(Rotas.INICIO_GESTOR) {
                InicioGestorScreen(
                    usuario = usuario,
                    onAbrirPerfil = abrirPerfil,
                    onAbrirAba = ::irParaAba,
                    onAbrirVeiculo = { navController.navigate(Rotas.veiculoDetalhe(it)) }
                )
            }
            composable(Rotas.INICIO_MOTORISTA) {
                InicioMotoristaScreen(
                    usuario = usuario,
                    onAbrirPerfil = abrirPerfil,
                    onAbrirAba = ::irParaAba,
                    onCadastrarVeiculo = { navController.navigate(Rotas.veiculoForm()) }
                )
            }
            composable(Rotas.MAPA) {
                MapaFrotaScreen(usuario = usuario, onAbrirVeiculo = { navController.navigate(Rotas.veiculoDetalhe(it)) })
            }
            composable(Rotas.FROTA) {
                FrotaScreen(
                    usuario = usuario,
                    onAbrirPerfil = abrirPerfil,
                    onNovoVeiculo = { navController.navigate(Rotas.veiculoForm()) },
                    onAbrirVeiculo = { navController.navigate(Rotas.veiculoDetalhe(it)) }
                )
            }
            composable(Rotas.EQUIPE) {
                EquipeScreen(usuario = usuario, onAbrirPerfil = abrirPerfil)
            }
            composable(Rotas.ALERTAS) {
                AlertasScreen(
                    usuario = usuario,
                    onAbrirPerfil = abrirPerfil,
                    onNovaCerca = { navController.navigate(Rotas.cercaEditor()) },
                    onEditarCerca = { navController.navigate(Rotas.cercaEditor(it)) },
                    onVerNoMapa = { placa ->
                        PedidosNavegacao.pedirMapa(PedidoMapa(placa = placa))
                        irParaAba(Rotas.MAPA)
                    }
                )
            }
            composable(Rotas.MEU_VEICULO) {
                MeuVeiculoScreen(
                    usuario = usuario,
                    onCadastrarVeiculo = { navController.navigate(Rotas.veiculoForm()) },
                    onEditarVeiculo = { navController.navigate(Rotas.veiculoForm(it)) },
                    onAbrirMapa = { irParaAba(Rotas.MAPA) }
                )
            }
            composable(Rotas.PERFIL) {
                PerfilScreen(
                    usuario = usuario,
                    mostrarVoltar = usuario?.perfil != Perfil.MOTORISTA,
                    onBack = { navController.popBackStack() },
                    onPerfilAtualizado = sessao::atualizar,
                    onSair = { sessao.sair() }
                )
            }

            // ---------- Telas cheias ----------
            composable(
                Rotas.VEICULO_FORM,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
                enterTransition = { slideEntrada() },
                popExitTransition = { slideSaida() }
            ) { e ->
                VeiculoFormScreen(
                    veiculoId = e.arguments?.getLong("id")?.takeIf { it > 0 },
                    usuario = usuario,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                Rotas.VEICULO_DETALHE,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
                enterTransition = { slideEntrada() },
                popExitTransition = { slideSaida() }
            ) { e ->
                VeiculoDetalheScreen(
                    veiculoId = e.arguments?.getLong("id") ?: 0L,
                    usuario = usuario,
                    onBack = { navController.popBackStack() },
                    onEditar = { navController.navigate(Rotas.veiculoForm(it)) },
                    onVerNoMapa = { placa ->
                        PedidosNavegacao.pedirMapa(PedidoMapa(placa = placa))
                        navController.popBackStack()
                        irParaAba(Rotas.MAPA)
                    }
                )
            }
            composable(
                Rotas.CERCA_EDITOR,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
                enterTransition = { slideEntrada() },
                popExitTransition = { slideSaida() }
            ) { e ->
                CercaEditorScreen(
                    cercaId = e.arguments?.getLong("id")?.takeIf { it > 0 },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

private fun AnimatedContentTransitionScope<*>.slideEntrada() =
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(280)) + fadeIn(tween(280))

private fun AnimatedContentTransitionScope<*>.slideSaida() =
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(250)) + fadeOut(tween(250))

